package com.theveloper.pixelplay.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.content.SharedPreferences
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data class representing a saved crash log entry.
 */
data class CrashLogData(
    val timestamp: Long,
    val formattedDate: String,
    val exceptionMessage: String,
    val stackTrace: String
) {
    /**
     * Returns the full crash log formatted for display or sharing.
     */
    fun getFullLog(): String {
        return buildString {
            appendLine("=== PixelPlayer Crash Report ===")
            appendLine("Date: $formattedDate")
            appendLine("Exception: $exceptionMessage")
            appendLine()
            appendLine("Stack Trace:")
            appendLine(stackTrace)
        }
    }
}

/**
 * Custom UncaughtExceptionHandler that saves crash information to SharedPreferences
 * so it can be displayed to the user when the app restarts.
 */
object CrashHandler : Thread.UncaughtExceptionHandler {

    private const val PREFS_NAME = "crash_handler_prefs"
    private const val KEY_HAS_CRASH = "has_crash"
    private const val KEY_TIMESTAMP = "crash_timestamp"
    private const val KEY_EXCEPTION_MESSAGE = "crash_exception_message"
    private const val KEY_STACK_TRACE = "crash_stack_trace"

    private lateinit var appContext: Context
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    private val prefs: SharedPreferences
        get() = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Installs this crash handler as the default uncaught exception handler.
     * Should be called in Application.onCreate().
     */
    fun install(context: Context) {
        appContext = context.applicationContext ?: context
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            saveCrashLog(throwable)
        } catch (_: Throwable) {
            // Never let diagnostics replace the original crash.
        }

        // Call the default handler to allow normal crash behavior
        defaultHandler?.uncaughtException(thread, throwable)
    }

    private fun saveCrashLog(throwable: Throwable) {
        val timestamp = System.currentTimeMillis()
        val stackTrace = getStackTraceString(throwable)
        val exceptionMessage = throwable.message ?: throwable.javaClass.simpleName

        // Also write a plain-text copy to the user's Downloads folder. This is
        // especially useful on Android 10 devices where adb/logcat access may
        // be unavailable. Try MediaStore first, then fall back to the app
        // external files directory.
        val fullLog = buildString {
            appendLine("=== PixelPlayer Crash Report ===")
            appendLine("Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date(timestamp))}")
            appendLine("Thread: ${Thread.currentThread().name}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Exception: $exceptionMessage")
            appendLine()
            appendLine("Stack Trace:")
            appendLine(stackTrace)
        }
        writeCrashFile(fullLog, timestamp)

        // Use commit() instead of apply() to ensure data is written synchronously
        // before the process terminates
        prefs.edit().apply {
            putBoolean(KEY_HAS_CRASH, true)
            putLong(KEY_TIMESTAMP, timestamp)
            putString(KEY_EXCEPTION_MESSAGE, exceptionMessage)
            putString(KEY_STACK_TRACE, stackTrace)
            commit() // Synchronous write - ensures data is saved before process dies
        }
    }

    private fun getStackTraceString(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        return sw.toString()
    }

    private fun writeCrashFile(content: String, timestamp: Long) {
        val fileName = "PixelPlayer-crash-$timestamp.txt"

        // First write to app-specific external storage. This path is available
        // without storage permission and is the most reliable option during a
        // crash, including failures very early in process startup.
        runCatching {
            val dir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (dir != null) {
                java.io.File(dir, fileName).writeText(content, Charsets.UTF_8)
            }
        }

        // Also try Downloads through MediaStore on Android 10+.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri: Uri = appContext.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    values
                ) ?: return@runCatching
                appContext.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(content.toByteArray(Charsets.UTF_8))
                    output.flush()
                }
                ContentValues().apply {
                    put(MediaStore.Downloads.IS_PENDING, 0)
                }.also {
                    appContext.contentResolver.update(uri, it, null, null)
                }
            }
        }
    }

    /**
     * Checks if there is a saved crash log from a previous session.
     */
    fun hasCrashLog(): Boolean {
        if (!::appContext.isInitialized) return false
        return prefs.getBoolean(KEY_HAS_CRASH, false)
    }

    /**
     * Retrieves the saved crash log data.
     * Returns null if no crash log exists.
     */
    fun getCrashLog(): CrashLogData? {
        if (!hasCrashLog()) return null

        val timestamp = prefs.getLong(KEY_TIMESTAMP, 0)
        val exceptionMessage = prefs.getString(KEY_EXCEPTION_MESSAGE, "Unknown error") ?: "Unknown error"
        val stackTrace = prefs.getString(KEY_STACK_TRACE, "") ?: ""

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(timestamp))

        return CrashLogData(
            timestamp = timestamp,
            formattedDate = formattedDate,
            exceptionMessage = exceptionMessage,
            stackTrace = stackTrace
        )
    }

    /**
     * Clears the saved crash log.
     * Should be called after the user has acknowledged the crash report.
     */
    fun clearCrashLog() {
        if (!::appContext.isInitialized) return
        prefs.edit().clear().apply()
    }
}
