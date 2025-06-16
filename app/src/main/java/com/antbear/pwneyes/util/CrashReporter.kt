package com.antbear.pwneyes.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.os.Build
import android.os.Process
import android.util.Log
import com.antbear.pwneyes.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess

/**
 * Custom crash reporter that catches uncaught exceptions, logs them,
 * and emails the crash report on the next app start.
 */
class CrashReporter private constructor(private val context: Context) : Thread.UncaughtExceptionHandler {
    private val TAG = "CrashReporter"
    
    // Original exception handler to call after our processing
    private val defaultHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()
    
    // Email address to send crash reports to
    private val supportEmail = "pwneyes@proton.me"
    
    // Directory to store crash logs
    private val crashDir by lazy {
        File(context.filesDir, "crash_reports").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    companion object {
        private const val MAX_REPORTS = 5 // Maximum number of reports to keep
        private const val CRASH_REPORT_PREFIX = "crash_"
        private const val CRASH_EXTENSION = ".log"
        
        @Volatile
        private var INSTANCE: CrashReporter? = null
        
        fun initialize(context: Context) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = CrashReporter(context.applicationContext)
                        Thread.setDefaultUncaughtExceptionHandler(INSTANCE)
                        Log.i("CrashReporter", "Crash reporter initialized")
                    }
                }
            }
        }
        
        /**
         * Check for stored crash reports and send them via email
         * Call this from Application.onCreate()
         */
        fun checkForCrashReports(context: Context) {
            try {
                val reporter = INSTANCE ?: CrashReporter(context.applicationContext)
                val reports = reporter.getPendingCrashReports()
                
                if (reports.isNotEmpty()) {
                    Log.i("CrashReporter", "Found ${reports.size} crash reports to send")
                    reporter.sendCrashReports(reports)
                } else {
                    Log.d("CrashReporter", "No crash reports found")
                }
            } catch (e: Exception) {
                Log.e("CrashReporter", "Error checking for crash reports", e)
            }
        }
    }
    
    /**
     * Handle uncaught exceptions, save crash report
     */
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            Log.e(TAG, "Uncaught exception in thread ${thread.name}", throwable)
            
            // Save crash report to file
            val report = generateCrashReport(thread, throwable)
            saveCrashReportToFile(report)
            
            // Clean up old reports if we have too many
            cleanupOldReports()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling crash", e)
        } finally {
            // Let the default handler deal with the crash after our processing
            defaultHandler?.uncaughtException(thread, throwable) ?: run {
                // If no default handler, force exit the process
                Process.killProcess(Process.myPid())
                exitProcess(10)
            }
        }
    }
    
    /**
     * Generate a detailed crash report
     */
    private fun generateCrashReport(thread: Thread, throwable: Throwable): String {
        val result = StringWriter()
        val printWriter = PrintWriter(result)
        
        // App & Device info
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val timeString = dateFormat.format(Date())
        
        printWriter.println("*** PwnEyes Crash Report ***")
        printWriter.println("Date: $timeString")
        printWriter.println("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        printWriter.println("Build Type: ${BuildConfig.BUILD_TYPE}")
        printWriter.println("Flavor: ${BuildConfig.FLAVOR}")
        
        // Device information
        printWriter.println("\nDevice Information:")
        printWriter.println("Manufacturer: ${Build.MANUFACTURER}")
        printWriter.println("Model: ${Build.MODEL}")
        printWriter.println("Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        
        // Thread info
        printWriter.println("\nThread: ${thread.name}")
        
        // Exception stack trace
        printWriter.println("\nException:")
        throwable.printStackTrace(printWriter)
        
        // Additional stack trace information
        printWriter.println("\nCause:")
        var cause = throwable.cause
        while (cause != null) {
            cause.printStackTrace(printWriter)
            cause = cause.cause
        }
        
        // Memory information
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory() / (1024 * 1024)
        val totalMemory = runtime.totalMemory() / (1024 * 1024)
        val freeMemory = runtime.freeMemory() / (1024 * 1024)
        val usedMemory = totalMemory - freeMemory
        
        printWriter.println("\nMemory:")
        printWriter.println("Max Memory: $maxMemory MB")
        printWriter.println("Total Memory: $totalMemory MB")
        printWriter.println("Used Memory: $usedMemory MB")
        printWriter.println("Free Memory: $freeMemory MB")
        
        printWriter.flush()
        return result.toString()
    }
    
    /**
     * Save crash report to a file
     */
    private fun saveCrashReportToFile(report: String) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "$CRASH_REPORT_PREFIX$timestamp$CRASH_EXTENSION"
            val reportFile = File(crashDir, fileName)
            
            reportFile.writeText(report)
            Log.i(TAG, "Crash report saved to ${reportFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving crash report", e)
        }
    }
    
    /**
     * Clean up old reports, keeping only the most recent MAX_REPORTS
     */
    private fun cleanupOldReports() {
        try {
            val files = crashDir.listFiles { file ->
                file.isFile && file.name.startsWith(CRASH_REPORT_PREFIX) && file.name.endsWith(CRASH_EXTENSION)
            }
            
            if (files != null && files.size > MAX_REPORTS) {
                // Sort by last modified date, oldest first
                val sortedFiles = files.sortedBy { it.lastModified() }
                
                // Delete the oldest files, keeping only MAX_REPORTS
                val filesToDelete = sortedFiles.take(files.size - MAX_REPORTS)
                for (file in filesToDelete) {
                    if (file.delete()) {
                        Log.d(TAG, "Deleted old crash report: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up old reports", e)
        }
    }
    
    /**
     * Get list of pending crash report files
     */
    private fun getPendingCrashReports(): List<File> {
        return try {
            val files = crashDir.listFiles { file ->
                file.isFile && file.name.startsWith(CRASH_REPORT_PREFIX) && file.name.endsWith(CRASH_EXTENSION)
            }
            
            files?.toList() ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting pending crash reports", e)
            emptyList()
        }
    }
    
    /**
     * Send crash reports via email
     */
    private fun sendCrashReports(reports: List<File>) {
        try {
            if (reports.isEmpty()) return
            
            // Build email content
            val subject = "PwnEyes Crash Report (${BuildConfig.VERSION_NAME})"
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            
            val body = StringBuilder()
            body.append("PwnEyes crashed. Please find the crash reports attached.\n\n")
            body.append("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n")
            body.append("Device: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}\n")
            body.append("Time: ${dateFormat.format(Date())}\n\n")
            body.append("Reports attached: ${reports.size}\n")
            
            // Create email intent
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(supportEmail))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body.toString())
                
                // Add attachments
                val uris = ArrayList<android.net.Uri>()
                for (file in reports) {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    uris.add(uri)
                }
                
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                
                // Add flags to open from new task (since we might be called from Application.onCreate)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            // Start activity to send email
            context.startActivity(Intent.createChooser(intent, "Send Crash Report").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            
            // Reports will be deleted after successful email sending
            // We can't know for sure if the email was sent, so we'll keep them for now
            // They'll be cleaned up later if we exceed MAX_REPORTS
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending crash reports", e)
        }
    }
}
