package nl.tudelft.trustchain.literaturedao
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.frostwire.jlibtorrent.SessionManager
import com.frostwire.jlibtorrent.Sha1Hash
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.BlockSigner
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.trustchain.common.BaseActivity
import nl.tudelft.trustchain.literaturedao.ipv8.LiteratureCommunity
import nl.tudelft.trustchain.literaturedao.ipv8.SwarmHealth
import nl.tudelft.trustchain.literaturedao.torrent.ContentSeeder
import nl.tudelft.trustchain.literaturedao.util.Util


open class LiteratureDaoActivity : BaseActivity() {
    override val navigationGraph = R.navigation.nav_literaturedao
    override val bottomNavigationMenu = R.menu.literature_navigation_menu
    var swarmHealthMap = mutableMapOf<Sha1Hash, SwarmHealth>()
    private lateinit var literatureGossipingService: LiteratureGossipingService
    private var gossipServiceBound = false

    var sessionManager: SessionManager? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as LiteratureGossipingService.LocalBinder
            literatureGossipingService = binder.getService()
            gossipServiceBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            gossipServiceBound = false
        }
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            // handle search intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        actionBar?.setDisplayHomeAsUpEnabled(true)

        startup()
    }

    override fun onDestroy() {
        sessionManager?.stop()
        sessionManager = null
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        Intent(this, LiteratureGossipingService::class.java).also { intent ->
            startService(intent)
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()

        if (gossipServiceBound) {
            unbindService(connection)
            gossipServiceBound = false
        }
    }

    private fun startup() {
        val ses = SessionManager()
        ses.start()
        registerBlockSigner()
        iterativelyUpdateSwarmHealth()

        // Start ContentSeeder service: for serving music torrents to other devices
        ContentSeeder.getInstance(
            applicationContext.cacheDir,
            ses
        ).start()

        sessionManager = ses
    }

    /**
     * Show libtorrent connectivity stats
     */
    fun getStatsOverview(): String {
        val sessionManager = sessionManager ?: return "Starting torrent client..."
        if (!sessionManager.isRunning) return "Starting torrent client..."
        return "up: ${Util.readableBytes(sessionManager.uploadRate())}, down: ${
            Util.readableBytes(sessionManager.downloadRate())
        }, dht nodes (torrent): ${sessionManager.dhtNodes()}"
    }

    /**
     * On discovering a half block, with tag publish_release, agree it immediately (for now).
     */
    private fun registerBlockSigner() {
        val literatureCommunity = IPv8Android.getInstance().getOverlay<LiteratureCommunity>()
        literatureCommunity?.registerBlockSigner(
            "literature",
            object : BlockSigner {
                override fun onSignatureRequest(block: TrustChainBlock) {
                    literatureCommunity.createAgreementBlock(block, mapOf<Any?, Any?>())
                }
            }
        )
    }

    /**
     * Keep track of Swarm Health for all torrents being monitored
     */
    private fun iterativelyUpdateSwarmHealth() {
        lifecycleScope.launchWhenStarted {
            while (isActive) {
                swarmHealthMap = filterSwarmHealthMap()

                if (gossipServiceBound) {
                    literatureGossipingService.setSwarmHealthMap(swarmHealthMap)
                }
                delay(3000)
            }
        }
    }

    /**
     * Merge local and remote swarm health map and remove outdated data
     */
    private fun filterSwarmHealthMap(): MutableMap<Sha1Hash, SwarmHealth> {
        val localMap = updateLocalSwarmHealthMap()
        val literatureCommunity = IPv8Android.getInstance().getOverlay<LiteratureCommunity>()
        val communityMap = literatureCommunity?.swarmHealthMap ?: mutableMapOf()
        // Keep the highest numPeers/numSeeds count of all items in both maps
        // This map contains all the combined data, where local and community map data are merged;
        // the highest connectivity count for each item is saved in a gloal map for the MusicService
        val map: MutableMap<Sha1Hash, SwarmHealth> = mutableMapOf<Sha1Hash, SwarmHealth>()
        val allKeys = localMap.keys + communityMap.keys
        for (infoHash in allKeys) {
            val shLocal = localMap[infoHash]
            val shRemote = communityMap[infoHash]

            val bestSwarmHealth = SwarmHealth.pickBest(shLocal, shRemote)
            if (bestSwarmHealth != null) {
                map[infoHash] = bestSwarmHealth
            }
        }
        // Remove outdated swarm health data: if the data is outdated, we throw it away
        return map.filterValues { it.isUpToDate() }.toMutableMap()
    }

    /**
     * Go through all the torrents that we are currently seeding and mark its connectivity to peers
     */
    private fun updateLocalSwarmHealthMap(): MutableMap<Sha1Hash, SwarmHealth> {
        val sessionManager = sessionManager ?: return mutableMapOf()
        val contentSeeder =
            ContentSeeder.getInstance(cacheDir, sessionManager)
        val localMap = contentSeeder.swarmHealthMap
        for (infoHash in localMap.keys) {
            // Update all connectivity stats of the torrents that we are currently seeding
            if (sessionManager.isRunning) {
                val handle = sessionManager.find(infoHash) ?: continue
                val newSwarmHealth = SwarmHealth(
                    infoHash.toString(),
                    handle.status().numPeers().toUInt(),
                    handle.status().numSeeds().toUInt()
                )
                // Never go below 1, because we know we are at least 1 seeder of our local files
                if (newSwarmHealth.numSeeds.toInt() < 1) continue
                localMap[infoHash] = newSwarmHealth
            }
        }
        return localMap
    }

}
