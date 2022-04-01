package nl.tudelft.trustchain.literaturedao.ipv8
import android.util.Log
import com.frostwire.jlibtorrent.Sha1Hash
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCrawler
import nl.tudelft.ipv8.attestation.trustchain.TrustChainSettings
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.messaging.Packet
import kotlin.random.Random

class LiteratureCommunity(
    settings: TrustChainSettings,
    database: TrustChainStore,
    crawler: TrustChainCrawler = TrustChainCrawler()
) : TrustChainCommunity(settings, database, crawler)  {
    override val serviceId: String = "0215eded9b27e6905a6d3fd02cc64d363c03a026"
    var swarmHealthMap = mutableMapOf<Sha1Hash, SwarmHealth>() // All recent swarm health data that

    object MessageID {
        const val DEBUG_MESSAGE = 1
        const val SEARCH_QUERY = 2
        const val SWARM_HEALTH_MESSAGE = 3
    }

    class Factory(
        private val settings: TrustChainSettings,
        private val database: TrustChainStore,
        private val crawler: TrustChainCrawler = TrustChainCrawler()
    ) : Overlay.Factory<LiteratureCommunity>(LiteratureCommunity::class.java) {
        override fun create(): LiteratureCommunity {
            return LiteratureCommunity(settings, database, crawler)
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun broadcastDebugMessage(message: String) {
        for (peer in getPeers()) {
            val packet = serializePacket(MessageID.DEBUG_MESSAGE, DebugMessage(message))
            send(peer.address, packet)
        }
    }

    /**
     * Communicate one literature to a peer neighbour
     * @return the amount of blocks that were sent
     */
    fun communicateLiterature(): Int {
        Log.i("litdao", "Communicating blocks")
        val peer = pickRandomPeer() ?: return 0
        val releaseBlocks = database.getBlocksWithType("literature")
        val maxBlocks = 1
        var count = 0
        releaseBlocks.shuffled().withIndex().forEach {
            count += 1
            if (it.index >= maxBlocks) return count
            sendBlock(it.value, peer, ttl = 3)
        }
        return count
    }

    private fun onDebugMessage(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(DebugMessage.Deserializer)
        Log.i("litdao", peer.mid + ": " + payload.message)
    }

    private fun onQueryMessage(packet: Packet) {
        // handle query message here
    }

    private fun pickRandomPeer(): Peer? {
        val peers = getPeers()
        if (peers.isEmpty()) return null
        var index = 0
        // Pick a random peer if we have more than 1 connected
        if (peers.size > 1) {
            index = Random.nextInt(peers.size - 1)
        }
        return peers[index]
    }

    /**
     * Send a SwarmHealth message to a random peer
     */
    fun sendSwarmHealthMessage(swarmHealth: SwarmHealth): Boolean {
        val peer = pickRandomPeer() ?: return false
        send(peer, serializePacket(MessageID.SWARM_HEALTH_MESSAGE, swarmHealth))
        return true
    }

    init {
        messageHandlers[MessageID.DEBUG_MESSAGE] = ::onDebugMessage
        messageHandlers[MessageID.SEARCH_QUERY] = ::onQueryMessage
    }


}
