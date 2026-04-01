import java.time.LocalDateTime

data class Task(
    val id: Int,
    var title: String,
    var description: String,
    var priority: Priority,
    var isDone: Boolean,
    val createdAt: LocalDateTime
)
