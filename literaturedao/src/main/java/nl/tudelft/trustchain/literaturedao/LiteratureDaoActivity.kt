package nl.tudelft.trustchain.literaturedao
import nl.tudelft.trustchain.common.BaseActivity

import android.util.Log
import nl.tudelft.trustchain.literaturedao.controllers.FileStorageController
import java.io.InputStream

open class LiteratureDaoActivity : BaseActivity() {
    override val navigationGraph = R.navigation.nav_literaturedao
    override val bottomNavigationMenu = R.menu.literature_navigation_menu

    override fun onStart() {
        super.onStart()
        Log.d("litdao", "starting ...")

        Log.d("litdao", applicationContext.cacheDir.absolutePath)

        // This is how you initialise the FileStorageController
        // the applicationCOntext needs to be passed from a place where it's accessible, such as here
        val docController = FileStorageController(applicationContext.cacheDir.absolutePath)

        Log.d("litdao", docController.listFiles().size.toString())
    }
}
