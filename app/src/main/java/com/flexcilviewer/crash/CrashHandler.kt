package com.flexcilviewer.crash

import android.content.Context
import android.content.Intent
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrashHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val report = buildCrashReport(thread, throwable)

            val intent = Intent(context, CrashActivity::class.java).apply {
                putExtra(CrashActivity.EXTRA_CRASH_REPORT, report)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
            }
            context.startActivity(intent)

            // Give the new activity time to start before the process dies
            Thread.sleep(500)
        } catch (_: Exception) {
            // If our handler itself fails, fall back to the system handler
            defaultHandler?.uncaughtException(thread, throwable)
        }

        android.os.Process.killProcess(android.os.Process.myPid())
    }

    private fun buildCrashReport(thread: Thread, throwable: Throwable): String {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTrace = sw.toString()

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val packageInfo = try {
            val pm = context.packageManager
            val pi = pm.getPackageInfo(context.packageName, 0)
            "Version: ${pi.versionName} (${pi.longVersionCode})"
        } catch (_: Exception) { "Version: unknown" }

        val deviceInfo = buildString {
            appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine("Android: ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})")
            appendLine("App: ${context.packageName}")
            appendLine(packageInfo)
            appendLine("Thread: ${thread.name}")
            appendLine("Timestamp: $timestamp")
        }

        return buildString {
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("  FLEXCIL VIEWER — CRASH REPORT")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine()
            appendLine("── Device Info ──────────────────────────")
            append(deviceInfo)
            appendLine()
            appendLine("── Exception ────────────────────────────")
            appendLine(throwable::class.qualifiedName ?: throwable::class.simpleName ?: "UnknownException")
            appendLine(throwable.message ?: "(no message)")
            appendLine()
            appendLine("── Stack Trace ──────────────────────────")
            appendLine(stackTrace)
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        }
    }
}
