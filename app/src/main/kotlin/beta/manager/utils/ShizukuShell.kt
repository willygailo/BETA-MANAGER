package beta.manager.utils

import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

object ShizukuShell {

    suspend fun isAvailable(): Boolean = withContext(Dispatchers.Main) {
        try {
            Shizuku.pingBinder()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getVersion(): Int = withContext(Dispatchers.Main) {
        try {
            Shizuku.getVersion()
        } catch (_: Exception) {
            -1
        }
    }

    suspend fun isShellUid(): Boolean = withContext(Dispatchers.Main) {
        try {
            Shizuku.getUid() == 2000
        } catch (_: Exception) {
            false
        }
    }

    suspend fun isRootUid(): Boolean = withContext(Dispatchers.Main) {
        try {
            Shizuku.getUid() == 0
        } catch (_: Exception) {
            false
        }
    }
}
