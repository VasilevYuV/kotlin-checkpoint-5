import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object AppLogger {
    private const val LOG_FILE_NAME = "errors.log"
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun logError(message: String, exception: Exception? = null) {
        runCatching {
            val file = File(LOG_FILE_NAME)
            val details = exception?.let { " | ${it::class.simpleName}: ${it.message}" } ?: ""
            val logLine = "[${LocalDateTime.now().format(formatter)}] ERROR: $message$details\n"
            file.appendText(logLine)
        }
    }
}
