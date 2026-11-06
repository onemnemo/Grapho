package com.onemnemo.grapho

import android.content.Context
import android.os.Build
import com.onemnemo.grapho.engine.NativeCrash
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Keeps crash reports on the device. A crash writes a text file to the app's own folder, and the next launch
 * offers it to the person to share. Nothing is sent by the app.
 */
object CrashReports {
    private const val MAX_FILES = 5
    private const val MAX_CHARS = 20_000

    private fun folder(context: Context) = File(context.filesDir, "crashes")

    /** Starts catching crashes. Call once from [android.app.Application.onCreate]. */
    fun install(context: Context) {
        val dir = folder(context)
        dir.mkdirs()
        try {
            NativeCrash.nativeInstall(dir.absolutePath)
        } catch (e: UnsatisfiedLinkError) {
            // The report for Java crashes still works without the native library.
        }
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val files = dir.listFiles()?.size ?: 0
                if (files < MAX_FILES) {
                    File(dir, "java-${System.currentTimeMillis()}.txt").writeText(javaReport(thread, error))
                }
            } catch (e: Exception) {
                // A failure to write the report must not hide the crash itself.
            }
            previous?.uncaughtException(thread, error)
        }
    }

    /** The text of the newest saved report, or null when there is none. */
    fun pending(context: Context): String? {
        val newest = folder(context).listFiles()?.maxByOrNull { it.lastModified() } ?: return null
        val body = try {
            newest.readText().take(MAX_CHARS)
        } catch (e: Exception) {
            return null
        }
        return header(context) + "\n" + body
    }

    /** Removes every saved report. */
    fun clear(context: Context) {
        folder(context).listFiles()?.forEach { it.delete() }
    }

    private fun javaReport(thread: Thread, error: Throwable): String {
        val trace = StringWriter()
        error.printStackTrace(PrintWriter(trace))
        return "Crash on thread ${thread.name}\n\n$trace"
    }

    private fun header(context: Context): String {
        val version = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            "unknown"
        }
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        return "Grapho $version, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}), " +
            "${Build.MANUFACTURER} ${Build.MODEL}, read $stamp\n"
    }
}
