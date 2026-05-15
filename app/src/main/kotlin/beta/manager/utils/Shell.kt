package beta.manager.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object Shell {

    sealed class Result {
        data class Success(val output: String, val exitCode: Int = 0) : Result()
        data class Error(val message: String, val exitCode: Int = -1) : Result()
    }

    suspend fun execute(command: String, timeout: Long = 30000L): Result = withContext(Dispatchers.IO) {
        try {
            val runtime = Runtime.getRuntime()
            val process = runtime.exec(arrayOf("sh", "-c", command))

            val stdout = StringBuilder()
            val stderr = StringBuilder()

            val stdoutReader = Thread {
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stdout.appendLine(line)
                    }
                }
            }

            val stderrReader = Thread {
                BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stderr.appendLine(line)
                    }
                }
            }

            stdoutReader.start()
            stderrReader.start()

            val finished = process.waitFor(timeout, java.util.concurrent.TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                return@withContext Result.Error("Command timed out after ${timeout}ms")
            }

            stdoutReader.join(1000)
            stderrReader.join(1000)

            val exitCode = process.exitValue()
            val output = stdout.toString().trim()

            if (exitCode == 0) {
                Result.Success(output)
            } else {
                Result.Error(
                    message = stderr.toString().trim().ifEmpty { output },
                    exitCode = exitCode
                )
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo root"))
            process.outputStream.close()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            process.waitFor()
            line == "root"
        } catch (e: Exception) {
            false
        }
    }

    suspend fun isAdbShell(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "whoami"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val user = reader.readLine()
            process.waitFor()
            user == "shell" || user == "root"
        } catch (e: Exception) {
            false
        }
    }
}
