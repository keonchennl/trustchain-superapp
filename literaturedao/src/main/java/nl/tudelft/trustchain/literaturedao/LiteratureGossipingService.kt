package nl.tudelft.trustchain.literaturedao

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.frostwire.jlibtorrent.Sha1Hash
import kotlinx.coroutines.*
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.literaturedao.ipv8.LiteratureCommunity
import nl.tudelft.trustchain.literaturedao.ipv8.SwarmHealth
import kotlin.system.exitProcess

class LiteratureGossipingService:
    Service() {
        private var swarmHealthMap: Map<Sha1Hash, SwarmHealth> = mutableMapOf()
        private val gossipTopTorrents = 1
        private val gossipRandomTorrents = 1
        private val binder = LocalBinder()
        private val scope = CoroutineScope(Dispatchers.IO)

        /**
         * Class used for the client Binder.  Because we know this service always
         * runs in the same process as its clients, we don't need to deal with IPC.
         */
        inner class LocalBinder : Binder() {
            // Return this instance of LocalService so clients can call public methods
            fun getService(): LiteratureGossipingService = this@LiteratureGossipingService
        }

        override fun onBind(intent: Intent): IBinder {
            return binder
        }

        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            return START_NOT_STICKY
        }

        override fun onCreate() {
            super.onCreate()
            Log.d("tag", "Gossiping service starting")
            scope.launch {
                iterativelySendReleaseBlocks()
            }
            scope.launch {
                iterativelyGossipSwarmHealth()
            }
        }

        override fun onDestroy() {
            scope.cancel()
            super.onDestroy()

            // We need to kill the app as IPv8 is started in Application.onCreate
            exitProcess(0)
        }

        fun setSwarmHealthMap(swarmHealthMap: Map<Sha1Hash, SwarmHealth>) {
            this.swarmHealthMap = swarmHealthMap
        }

        /**
         * This is a very simplistic way to crawl all chains from the peers you know
         */
        private suspend fun iterativelySendReleaseBlocks() {
            val literatureCommunity = IPv8Android.getInstance().getOverlay<LiteratureCommunity>()
            while (scope.isActive) {
                literatureCommunity?.communicateLiterature()
                delay(4000)
            }
        }

        /**
         * Every couple of seconds, gossip swarm health with other peers
         */
        private suspend fun iterativelyGossipSwarmHealth() {
            while (scope.isActive) {
                // Pick 1 of the most popular torrents and send those stats to any neighbour
                // First, we sort the map based on swarm health
                val sortedMap = swarmHealthMap.toList()
                    .sortedBy { (_, value) -> value }
                    .toMap()
                gossipSwarmHealth(sortedMap, gossipTopTorrents)
                gossipSwarmHealth(swarmHealthMap, gossipRandomTorrents)
                delay(4000)
            }
        }

        /**
         * Send SwarmHealth information to #maxIterations random peers
         */
        private fun gossipSwarmHealth(map: Map<Sha1Hash, SwarmHealth>, maxInterations: Int) {
            val literatureCommunity = IPv8Android.getInstance().getOverlay<LiteratureCommunity>()
            var count = 0
            for (entry in map.entries) {
                if (count > maxInterations) break
                literatureCommunity?.sendSwarmHealthMessage(entry.value)
                count += 1
            }
        }
}
