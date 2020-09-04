package com.example.musicdao.ipv8

import android.util.Log
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCrawler
import nl.tudelft.ipv8.attestation.trustchain.TrustChainSettings
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.messaging.Packet
import java.util.*

class MusicCommunity(
    settings: TrustChainSettings,
    database: TrustChainStore,
    crawler: TrustChainCrawler = TrustChainCrawler()
) : TrustChainCommunity(settings, database, crawler) {
    override val serviceId = "29384902d2938f34872398758cf7ca9238ccc333"

    class Factory(
        private val settings: TrustChainSettings,
        private val database: TrustChainStore,
        private val crawler: TrustChainCrawler = TrustChainCrawler()
    ) : Overlay.Factory<MusicCommunity>(MusicCommunity::class.java) {
        override fun create(): MusicCommunity {
            return MusicCommunity(settings, database, crawler)
        }
    }

    init {
        messageHandlers[MessageId.KEYWORD_SEARCH_MESSAGE] = ::onKeywordSearch
    }

    override fun bootstrap() {
        super.bootstrap()

        // Connect to some initial addresses to reduce 'empty content' page
        for (address in INITIAL_ADDRESSES) {
            walkTo(address)
        }
    }

    fun performRemoteKeywordSearch(keyword: String, ttl: Int = 2, originPublicKey: ByteArray = myPeer.publicKey.keyToBin()) {
        val maxPeersToAsk = 5 // This is a magic number, tweak during/after experiments
        for ((index, peer) in getPeers().withIndex()) {
            if (index >= maxPeersToAsk) break
            val packet = serializePacket(
                MessageId.KEYWORD_SEARCH_MESSAGE,
                KeywordSearchMessage(originPublicKey, ttl, keyword)
            )
            send(peer, packet)
        }
    }

    /**
     * When a peer asks for some music content with keyword, browse through my local collection of
     * blocks to find whether I have something. If I do, send the corresponding block directly back
     * to the original asker. If I don't, I will ask my peers to find it
     */
    private fun onKeywordSearch(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(KeywordSearchMessage.Deserializer)
        val keyword = payload.keyword.toLowerCase()
        var success = false
        database.getAllBlocks().forEach {
            val transaction = it.transaction
            val title = transaction["title"]?.toString()?.toLowerCase(Locale.ROOT)
            val artists = transaction["artists"]?.toString()?.toLowerCase(Locale.ROOT)
            if (title != null && title.contains(keyword)) {
                sendBlock(it, peer)
                success = true
            } else if (artists != null && artists.contains(keyword)) {
                sendBlock(it, peer)
                success = true
            }
        }
        if (!success) {
            if (!payload.checkTTL()) return
            performRemoteKeywordSearch(keyword, payload.ttl, payload.originPublicKey)
        }
        Log.i("KeywordSearch", peer.mid + ": " + payload.keyword)
    }

    companion object {
        // These are initial addresses for some peers that have initial content,
        // in the case that no content can be found on the first run of the app.
        val INITIAL_ADDRESSES: List<IPv4Address> = listOf(
            IPv4Address("83.84.32.175", 35376)
        )
    }

    object MessageId {
        const val KEYWORD_SEARCH_MESSAGE = 10
    }
}
