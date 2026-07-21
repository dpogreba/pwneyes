package com.antbear.pwneyes.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.antbear.pwneyes.UpdateChecker
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Downloads a release APK and hands it to the system installer.
 *
 * Android cannot install silently without device-owner/system privileges, so the
 * user always confirms in the system installer UI. This only removes the browser
 * detour, it does not bypass any consent step.
 */
object ApkUpdater {

    private const val APK_MIME = "application/vnd.android.package-archive"
    private const val FILE_NAME = "pwneyes-update.apk"

    private fun updatesDir(context: Context) =
        File(context.getExternalFilesDir(null), "updates").apply { mkdirs() }

    /**
     * Downloads [url] to app-private storage, reporting 0..100 progress.
     * Returns the file, or null on any failure.
     *
     * Blocking — call from a background thread. Refuses any URL that isn't an
     * HTTPS GitHub release asset.
     */
    fun downloadApk(context: Context, url: String, onProgress: (Int) -> Unit): File? {
        if (!UpdateChecker.isTrustedApkUrl(url)) return null

        val dir = updatesDir(context)
        dir.listFiles()?.forEach { it.delete() }   // prune previous downloads
        val out = File(dir, FILE_NAME)

        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true   // never downgrades https -> http
                connectTimeout = 15_000
                readTimeout = 30_000
            }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return null

            val total = conn.contentLength.toLong()
            conn.inputStream.use { input ->
                out.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    var downloaded = 0L
                    while (true) {
                        val read = input.read(buf)
                        if (read == -1) break
                        output.write(buf, 0, read)
                        downloaded += read
                        if (total > 0) onProgress(((downloaded * 100) / total).toInt())
                    }
                }
            }
            if (out.length() == 0L) null else out
        } catch (_: Exception) {
            out.delete()
            null
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * True only if the downloaded APK is signed with the same certificate as the
     * running app. A tampered or foreign APK is never handed to the installer.
     * (The system would reject a mismatched update anyway; this fails earlier and
     * with a clear message rather than a generic installer error.)
     */
    fun signatureMatchesInstalledApp(context: Context, apk: File): Boolean {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            PackageManager.GET_SIGNING_CERTIFICATES else @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES

        val downloaded = certDigests(pm.getPackageArchiveInfo(apk.absolutePath, flags))
        val installed = certDigests(
            try { pm.getPackageInfo(context.packageName, flags) } catch (_: Exception) { null }
        )
        return downloaded.isNotEmpty() && installed.isNotEmpty() && downloaded == installed
    }

    @Suppress("DEPRECATION")
    private fun certDigests(info: PackageInfo?): Set<String> {
        if (info == null) return emptySet()
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners
        } else {
            info.signatures
        } ?: return emptySet()

        val sha = MessageDigest.getInstance("SHA-256")
        return signatures.mapNotNull { sig ->
            runCatching { sha.digest(sig.toByteArray()).joinToString("") { "%02x".format(it) } }
                .getOrNull()
        }.toSet()
    }

    /** Android 8+ requires per-app "install unknown apps" consent before we can launch the installer. */
    fun canRequestInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()

    /** Sends the user to the system screen where they grant this app install permission. */
    fun installPermissionIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
            .setData(Uri.parse("package:${context.packageName}"))

    /** The system installer confirmation. The user's tap there is what actually installs. */
    fun installIntent(context: Context, apk: File): Intent {
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", apk
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
