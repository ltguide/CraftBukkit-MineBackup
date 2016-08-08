package ltguide.minebackup.utils

import com.dropbox.core.*
import ltguide.minebackup.MineBackup
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.util.*
import javax.naming.AuthenticationException

class DropBoxUtils(private val plugin: MineBackup) {

    companion object {

        val MAX_DROPBOX_PUSH_SIZE = 150 * 1024 * 1024.toLong()

        private val APP_KEY = "jbx8av4f9v22nol"
        private val APP_SECRET = ""
        private val IDENTIFIER = "MineBackupV2"
        private val DROPBOX_UPLOAD_CHUNK_SIZE = 2 * 1024 * 1024
    }

    private var appInfo: DbxAppInfo? = null
    private var webAuth: DbxWebAuthNoRedirect? = null
    private var requestConfig: DbxRequestConfig? = null

    init {
        if (!APP_SECRET.isEmpty()) {
            this.appInfo = DbxAppInfo(APP_KEY, APP_SECRET)
            this.requestConfig = DbxRequestConfig(IDENTIFIER, Locale.getDefault().toString())
        } else {
            plugin.warning("The Dropbox feature is disabled, because of a missing appsecret for this build of Minebackup.")
        }
    }

    fun isActivated(): Boolean {
        return appInfo != null
    }

    /**
     * Starts authorisation for dropbox OAuth2.

     * @return Returns the link to allows the access.
     */
    fun authoriseStart(): String {
        if (isActivated()) {
            webAuth = DbxWebAuthNoRedirect(requestConfig, appInfo)
            return webAuth!!.start()
        } else {
            plugin.warning("Dropbox feature is not activated!")
            return ""
        }
    }

    fun authoriseEnd(code: String): Boolean {

        if (!isActivated() || webAuth == null) {
            return false
        }

        val authFinish: DbxAuthFinish
        try {
            authFinish = webAuth!!.finish(code)
        } catch (ex: DbxException) {
            plugin.logException(ex, "Error in DbxWebAuth.start: " + ex.message)
            return false
        }

        // Save auth information to output file.
        plugin.persist.dropboxToken = authFinish.accessToken

        return true
    }

    @Throws(AuthenticationException::class)
    fun upload(file: File, authToken: String) {
        if (!isActivated()) {
            plugin.warning("Dropbox Feature is not activated!")
            return
        }

        val dbxClient = DbxClient(requestConfig, authToken)
        val path = file.path.substring((plugin.config.get("directories.destination") as String).length)

        try {
            if (file.length() > MAX_DROPBOX_PUSH_SIZE) {
                uploadChunked(dbxClient, file, path)
            } else {
                uploadPush(dbxClient, file, path)
            }

            plugin.info("Uploaded " + file.name + " to dropbox.")
        } catch (ex: DbxException) {
            plugin.logException(ex, "Error uploading to Dropbox")
        } catch (ex: IOException) {
            plugin.logException(ex, "Error reading from file \"" + file)
        }

    }

    @Throws(IOException::class, DbxException::class)
    private fun uploadPush(dbxClient: DbxClient, file: File, path: String) {
        FileInputStream(file).use { `in` -> dbxClient.uploadFile(path, DbxWriteMode.add(), file.length(), `in`) }
    }

    @Throws(IOException::class, DbxException::class)
    private fun uploadChunked(dbxClient: DbxClient, file: File, path: String) {

        val raf = RandomAccessFile(file, "r")
        val fileBuffer = ByteArray(DROPBOX_UPLOAD_CHUNK_SIZE)
        raf.read(fileBuffer, 0, DROPBOX_UPLOAD_CHUNK_SIZE)

        val uploadId = dbxClient.chunkedUploadFirst(fileBuffer)
        var lastUpdate = Date()
        var currentPosition: Long = DROPBOX_UPLOAD_CHUNK_SIZE.toLong()

        while (currentPosition < file.length()) {
            val length: Int = if (currentPosition + DROPBOX_UPLOAD_CHUNK_SIZE < file.length()) DROPBOX_UPLOAD_CHUNK_SIZE else (file.length() - currentPosition).toInt()
            raf.read(fileBuffer, 0, length)
            dbxClient.chunkedUploadAppend(uploadId, currentPosition, fileBuffer)
            currentPosition += length

            if (Date().time - lastUpdate.time > 60 * 1000) {
                lastUpdate = Date()
                plugin.info(String.format("%.2f%% of file uploaded.", currentPosition.toDouble() / file.length() * 100))
            }
        }

        dbxClient.chunkedUploadFinish(path, DbxWriteMode.add(), uploadId)
    }


}
