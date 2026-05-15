package beta.manager.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("beta_boot_prefs", Context.MODE_PRIVATE)
            val autoStart = prefs.getBoolean("auto_start", false)
            if (autoStart) {
                val serviceIntent = Intent(context, BetaService::class.java)
                context.startForegroundService(serviceIntent)
            }
        }
    }
}
