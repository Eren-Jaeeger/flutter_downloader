package vn.hunghd.flutterdownloader

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.URLConnection
import java.util.*

object IntentUtils {
    private fun buildIntent(context: Context, file: File, mime: String?): Intent {
        val intent = Intent(Intent.ACTION_VIEW)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".flutter_downloader.provider",
                file
            )
            intent.setDataAndType(uri, mime)
        } else {
            intent.setDataAndType(Uri.fromFile(file), mime)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return intent
    }

    @Synchronized
    fun validatedFileIntent(context: Context, path: String, contentType: String?): Intent? {
        val file = File(path)
        var mime: String? = contentType

        // Step 1: Check if initial contentType is valid
        if (mime.isNullOrBlank() || mime == "*/*" || mime == "application/octet-stream") {
            // Step 2: Try to guess from file stream
            try {
                FileInputStream(path).use { input ->
                    mime = URLConnection.guessContentTypeFromStream(input)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Step 3: Fallback to extension-based MIME detection
            if (mime.isNullOrBlank()) {
                mime = URLConnection.guessContentTypeFromName(path)
            }
            if (mime.isNullOrBlank()) {
                mime = getMimeTypeFromExtension(path)
            }

            if (mime.isNullOrBlank()) {
                mime = "*/*" // Final fallback
            }
        }

        var intent = buildIntent(context, file, mime)
        return if (canBeHandled(context, intent)) intent else null
    }

    private fun canBeHandled(context: Context, intent: Intent): Boolean {
        val manager = context.packageManager
        val results = manager.queryIntentActivities(intent, 0)
        return results.isNotEmpty()
    }

    private fun getMimeTypeFromExtension(path: String): String? {
        val extension = MimeTypeMap.getFileExtensionFromUrl(path)
            ?: File(path).extension
            ?: return null

        return MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(extension.lowercase(Locale.getDefault()))
    }
}