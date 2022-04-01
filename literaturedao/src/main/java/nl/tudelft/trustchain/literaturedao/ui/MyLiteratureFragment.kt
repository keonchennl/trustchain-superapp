package nl.tudelft.trustchain.literaturedao.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.frostwire.jlibtorrent.TorrentInfo
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.turn.ttorrent.client.SharedTorrent
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.common.DemoCommunity
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.literaturedao.R
import nl.tudelft.trustchain.literaturedao.controllers.FileStorageController
import nl.tudelft.trustchain.literaturedao.controllers.KeywordExtractor
import nl.tudelft.trustchain.literaturedao.controllers.PdfController
import nl.tudelft.trustchain.literaturedao.controllers.QueryHandler
import nl.tudelft.trustchain.literaturedao.ipv8.LiteratureCommunity
import java.io.*
import java.util.*
import kotlin.math.roundToInt


class MyLiteratureFragment : BaseFragment(R.layout.fragment_literature_overview) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        testSeeding()
//        lifecycleScope.launchWhenResumed {.
//            parsePDF()
//        }
    }

    private fun testSeeding() {

        val literatureCommunity = IPv8Android.getInstance().getOverlay<LiteratureCommunity>()!!
        printPeersInfo(literatureCommunity)
        val myName = literatureCommunity.myPeer.mid
        Log.i("litdao","I am $myName and Im broadcasting: hello")
        literatureCommunity.broadcastDebugMessage("hello")

        val demoCommunity = IPv8Android.getInstance().getOverlay<DemoCommunity>()!!
        val demoCommunityName = demoCommunity.myPeer.mid
        Log.i("personal","I am $demoCommunityName and Im broadcasting a message")
        demoCommunity.broadcastGreeting()

//        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
//        supportActionBar?.hide();

        Log.d("litdao", requireActivity().cacheDir.absolutePath.toString())

        // This is how you initialise the FileStorageController
        // the applicationCOntext needs to be passed from a place where it's accessible, such as here
        val docController = FileStorageController(requireActivity().cacheDir.absolutePath.toString())

        val tmpPath = requireActivity().cacheDir.resolve("literaturedao/documents/" + "1.pdf")
        val tmpFolder = requireActivity().cacheDir.resolve("literaturedao/documents")
        if( !tmpPath.exists() ) {
            requireContext().assets.open("1.pdf").use { inStream ->
                tmpPath.outputStream().use{ outStream ->
                    inStream.copyTo(outStream)
                }
            }
        }
        Log.d("litdao", docController.listFiles().size.toString())
        //test seeding
        val folderToShare = tmpFolder
        val tor = SharedTorrent.create(tmpFolder, listOf(tmpPath), 65535, listOf(), "TrustChain-Superapp")
        var torrentFile = "$folderToShare.torrent"
        if (!File(torrentFile).isFile) {
            torrentFile = "$folderToShare.torrent.added"
        }
        tor.save(FileOutputStream(torrentFile))
        val torrentInfo = TorrentInfo(File(torrentFile))
        val magnet = torrentInfo.makeMagnetUri()
        val torrentInfoName = torrentInfo.name()
        Log.d("litdao", magnet + torrentInfoName)
    }

    private fun printPeersInfo(overlay: Overlay) {
        val peers = overlay.getPeers()
        Log.i("litdao",overlay::class.simpleName + ": ${peers.size} peers")
        for (peer in peers) {
            val avgPing = peer.getAveragePing()
            val lastRequest = peer.lastRequest
            val lastResponse = peer.lastResponse

            val lastRequestStr = if (lastRequest != null)
                "" + ((Date().time - lastRequest.time) / 1000.0).roundToInt() + " s" else "?"

            val lastResponseStr = if (lastResponse != null)
                "" + ((Date().time - lastResponse.time) / 1000.0).roundToInt() + " s" else "?"

            val avgPingStr = if (!avgPing.isNaN()) "" + (avgPing * 1000).roundToInt() + " ms" else "? ms"
            Log.i("litdao", "${peer.mid} ${peer.address} (S: ${lastRequestStr}, R: ${lastResponseStr}, ${avgPingStr})")
        }
    }

    var tempStorage: MutableList<Pair<String, MutableList<Pair<String, Double>>>> = mutableListOf<Pair<String, MutableList<Pair<String, Double>>>>()


    fun parsePDF() {
        PDFBoxResourceLoader.init(context);
        var i = 1
        while (i < 3){
            val path = i.toString() + ".pdf"
            val stream: InputStream = requireContext().assets.open(path)
            val kws = getKWs(stream)
            save(path, kws)
            Log.e("litdao", "litdao: " + kws.toString())
            i += 1
        }
        // query test
        val handler = QueryHandler()
        Log.d("litdao", handler.scoreList("Clustering all the algorighems man", loadAll()).toString())
        Log.d("litdao", handler.scoreList("The pythagorean algorithms machine learning", loadAll()).toString())
    }

    fun getKWs(pdfIS: java.io.InputStream): MutableList<Pair<String, Double>>{
        var pdfController = PdfController()
        val keywordExtractorInput = pdfController.stripText(pdfIS)
        val csv: InputStream = requireContext().assets.open("stemmed_freqs.csv")
        val result = KeywordExtractor()
            .extract(keywordExtractorInput, csv)
        return result
    }

    fun save(path: String, KWList: MutableList<Pair<String, Double>>){
        tempStorage.add(Pair(path, KWList))
    }

    fun loadAll(): MutableList<Pair<String, MutableList<Pair<String, Double>>>>{
        return tempStorage
    }

}
