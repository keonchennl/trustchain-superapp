package nl.tudelft.trustchain.literaturedao.controllers

import android.util.Log
import com.frostwire.jlibtorrent.swig.operation_t.file
import java.io.*

/**
 * Controller for file storage.
 * Save new files, get existing files, remove files.
 */
class FileStorageController(val cacheDir : String) {

    val directory: String = cacheDir + "/literaturedao/documents/"

    init {
        val file = File(directory)
        if (!(file.exists() && file.isDirectory)) {
            file.mkdirs()
        }
    }

    /**
     * This method saves a file in the internal android storage and returns path to the file (uri).
     *
     * Throws an exception if file with the same name already exists.
     */
    fun saveFile(file: File): String {

        val from = file.inputStream()
        val to = File(directory + file.name).outputStream()

        from.copyTo(to)

        return file.name
    }

    fun saveFileStream(fileName: String, fileStream: FileInputStream) {
        val to = File(directory + fileName).outputStream()

        fileStream.copyTo(to)
    }

    /**
     * This method retrieves a file by uri.
     *
     * Throws exception if no file found.
     */
    fun getFile(fileName: String): File {

        val file = File(directory + fileName)
        if (!file.isFile) {
            throw IOException("No file with name '$fileName' found in literaturedao document folder.")
        }

        return file
    }

    /**
     * This method removes a file by uri.
     *
     * Throws exception if no file found at uri.
     */
    fun removeFile(fileName: String) {
        File(directory + fileName).delete()
    }

    /**
     * Returns a list of all files in the literaturedao document folder.
     */
    fun listFiles(): Array<File> {
        return File(directory).listFiles()
    }

}
