package beta.manager

import android.app.Application
import android.content.Intent
import beta.manager.ui.CrashReportActivity
import java.io.File
import java.io.StringWriter
import java.io.PrintWriter

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            try {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                e.printStackTrace(pw)
                pw.flush()
                val stackTrace = sw.toString()

                val crashDir = File(filesDir, "crashes")
                crashDir.mkdirs()
                File(crashDir, "crash_${System.currentTimeMillis()}.txt").writeText(stackTrace)

                val intent = Intent(this, CrashReportActivity::class.java).apply {
                    putExtra(CrashReportActivity.EXTRA_CRASH_INFO, stackTrace)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(intent)
            } catch (_: Exception) {}
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
}
