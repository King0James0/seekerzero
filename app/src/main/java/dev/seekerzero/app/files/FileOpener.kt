package dev.seekerzero.app.files

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import dev.seekerzero.app.api.MobileApiClient
import dev.seekerzero.app.util.LogCollector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Fetches a file from a0prod via /mobile/files/get, caches it under the
 * app's downloads cache, and launches ACTION_VIEW so the system's chosen
 * viewer (PDF reader, image viewer, text editor, etc.) opens it.
 *
 * Cache key is sha256(serverPath) so repeat taps on the same path reuse
 * the file. The cache is in `cacheDir/downloads/<hash>/<basename>` and is
 * cleared by Android when the OS reclaims space — fine for read-mostly
 * documents.
 */
object FileOpener {
    private const val TAG = "FileOpener"

    /**
     * High-level entry point. Suspends until the file is on disk, then
     * fires the system intent. Toasts on failure. Returns true if a
     * viewer was launched, false otherwise.
     */
    suspend fun openFromServer(context: Context, serverPath: String): Boolean {
        val cached = withContext(Dispatchers.IO) { ensureCached(context, serverPath) }
        cached.onFailure { err ->
            LogCollector.w(TAG, "fetch failed for $serverPath: ${err.message}")
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Couldn't fetch file: ${err.message ?: "unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
            }
            return false
        }
        val file = cached.getOrThrow()
        return withContext(Dispatchers.Main) { launchViewer(context, file) }
    }

    private fun ensureCached(context: Context, serverPath: String): Result<File> = runCatching {
        val dest = cacheTargetFor(context, serverPath)
        if (dest.exists() && dest.length() > 0L) {
            return@runCatching dest
        }
        // Run the suspend fetch synchronously inside a runBlocking is fine
        // because we're already off the main thread (IO dispatcher).
        kotlinx.coroutines.runBlocking {
            MobileApiClient.fetchFile(serverPath, dest).getOrThrow()
        }
    }

    private fun cacheTargetFor(context: Context, serverPath: String): File {
        val hash = sha256(serverPath).take(16)
        val basename = serverPath.substringAfterLast('/').ifBlank { "file.bin" }
        return File(File(context.cacheDir, "downloads/$hash"), basename)
    }

    private fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(s.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun launchViewer(context: Context, file: File): Boolean {
        val authority = "${context.packageName}.fileprovider"
        val uri: Uri = try {
            FileProvider.getUriForFile(context, authority, file)
        } catch (t: Throwable) {
            LogCollector.w(TAG, "FileProvider.getUriForFile failed: ${t.message}")
            Toast.makeText(context, "Cannot share fetched file: ${t.message}", Toast.LENGTH_LONG).show()
            return false
        }
        val mime = mimeFromName(file.name) ?: "application/octet-stream"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(Intent.createChooser(intent, "Open with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        } catch (t: Throwable) {
            LogCollector.w(TAG, "ACTION_VIEW launch failed: ${t.message}")
            Toast.makeText(
                context,
                "No app on this phone can open ${file.name}",
                Toast.LENGTH_LONG
            ).show()
            false
        }
    }

    private fun mimeFromName(name: String): String? {
        val ext = name.substringAfterLast('.', "").lowercase()
        if (ext.isEmpty()) return null
        val map = MimeTypeMap.getSingleton()
        return map.getMimeTypeFromExtension(ext)
            // Fill in a few that MimeTypeMap doesn't know on every device.
            ?: when (ext) {
                "md" -> "text/markdown"
                "log", "txt" -> "text/plain"
                "yml", "yaml" -> "application/x-yaml"
                "json" -> "application/json"
                "csv" -> "text/csv"
                "tsv" -> "text/tab-separated-values"
                "py" -> "text/x-python"
                "kt", "kts" -> "text/x-kotlin"
                else -> null
            }
    }
}
