package com.antbear.pwneyes.util

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.antbear.pwneyes.R
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * Manages release notes content and displaying release notes to users.
 */
class ReleaseNotesManager(
    private val context: Context,
    private val versionManager: VersionManager
) {
    private val TAG = "ReleaseNotesManager"
    
    /**
     * Shows the "What's New" dialog with the latest release notes
     * @return true if the dialog was shown, false otherwise
     */
    fun showWhatsNewDialog(): Boolean {
        try {
            val releaseNotes = getReleaseNotesForCurrentVersion()
            if (releaseNotes.isNullOrBlank()) {
                Log.w(TAG, "Release notes content was empty")
                return false
            }
            
            // Create and show the dialog
            AlertDialog.Builder(context)
                .setTitle("🎉 What's New in ${versionManager.getCurrentVersionName()}")
                .setMessage(releaseNotes)
                .setPositiveButton("OK") { dialog, _ ->
                    // Mark as seen when the user dismisses the dialog
                    versionManager.markWhatsNewAsSeen()
                    dialog.dismiss()
                }
                .setCancelable(false) // Force the user to press OK
                .show()
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error showing what's new dialog", e)
            return false
        }
    }
    
    /**
     * Get the release notes content for the current version
     */
    private fun getReleaseNotesForCurrentVersion(): String? {
        try {
            // Try to find a matching release notes file in the raw resources folder
            // The file should be named release_notes_vX.Y.Z.txt where X.Y.Z is the version name
            val versionName = versionManager.getCurrentVersionName()
            val rawResourceId = findReleaseNotesResourceId(versionName)
            
            if (rawResourceId != 0) {
                // Read from raw resource file
                return readRawResourceContent(rawResourceId)
            } else {
                // Fallback to a generic release notes text
                return createGenericReleaseNotes()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting release notes for current version", e)
            return null
        }
    }
    
    /**
     * Find the resource ID for release notes matching the given version name
     */
    private fun findReleaseNotesResourceId(versionName: String): Int {
        val resourceName = "release_notes_v${versionName.replace(".", "_")}"
        return context.resources.getIdentifier(
            resourceName, "raw", context.packageName
        )
    }
    
    /**
     * Read content from a raw resource file
     */
    private fun readRawResourceContent(resourceId: Int): String {
        val inputStream = context.resources.openRawResource(resourceId)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val stringBuilder = StringBuilder()
        
        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                stringBuilder.append(line).append('\n')
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error reading raw resource", e)
        } finally {
            try {
                inputStream.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error closing input stream", e)
            }
        }
        
        return stringBuilder.toString()
    }
    
    /**
     * Create a generic release notes message when specific notes aren't available
     */
    private fun createGenericReleaseNotes(): String {
        return """
            Thank you for updating PwnEyes!
            
            This update includes:
            • Performance improvements
            • Bug fixes
            • Enhanced stability
            • Better user experience
            
            We're continuously working to make PwnEyes better for you.
        """.trimIndent()
    }
    
    /**
     * Extracts the latest release notes from the provided release_notes_v10.33.md file
     * This is a special parser for the specific format used in this project
     */
    fun extractReleaseNotesFromFile(filePath: String): String {
        try {
            val fileContent = java.io.File(filePath).readText()
            
            // Extract the title and content from the markdown
            val lines = fileContent.lines()
            
            // Skip the first line (# Title) and build a formatted string
            val builder = StringBuilder()
            
            var i = 1 // Start after the title
            while (i < lines.size) {
                val line = lines[i].trim()
                
                // Add section titles (## headings)
                if (line.startsWith("##")) {
                    val title = line.substring(2).trim()
                    builder.append("\n📌 $title\n")
                } 
                // Add subsection titles (### headings)
                else if (line.startsWith("###")) {
                    val subtitle = line.substring(3).trim()
                    builder.append("\n• $subtitle:\n")
                }
                // Convert bullet points (- items)
                else if (line.startsWith("-")) {
                    val bulletPoint = line.substring(1).trim()
                    builder.append("  ✓ $bulletPoint\n")
                }
                // Include regular text paragraphs
                else if (line.isNotEmpty()) {
                    builder.append("$line\n")
                }
                
                i++
            }
            
            return builder.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting release notes from file", e)
            return createGenericReleaseNotes()
        }
    }
}
