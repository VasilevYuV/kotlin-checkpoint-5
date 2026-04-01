import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TaskRepository {
    private val tasks = mutableListOf<Task>()
    private var nextId = 1
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun addTask(title: String, description: String, priority: Priority): Task {
        val task = Task(
            id = nextId++,
            title = title,
            description = description,
            priority = priority,
            isDone = false,
            createdAt = LocalDateTime.now()
        )
        tasks.add(task)
        return task
    }

    fun getAllTasks(): List<Task> = tasks.toList()

    fun findById(id: Int): Task? = tasks.firstOrNull { it.id == id }

    fun editTask(
        id: Int,
        title: String? = null,
        description: String? = null,
        priority: Priority? = null,
        isDone: Boolean? = null
    ): Boolean {
        val task = findById(id) ?: return false
        if (title != null) task.title = title
        if (description != null) task.description = description
        if (priority != null) task.priority = priority
        if (isDone != null) task.isDone = isDone
        return true
    }

    fun deleteTask(id: Int): Boolean = tasks.removeIf { it.id == id }

    fun markTasksDone(ids: List<Int>, done: Boolean): Int {
        var changed = 0
        ids.distinct().forEach { id ->
            val task = findById(id)
            if (task != null && task.isDone != done) {
                task.isDone = done
                changed++
            }
        }
        return changed
    }

    fun search(
        titleContains: String? = null,
        priority: Priority? = null,
        isDone: Boolean? = null
    ): List<Task> {
        return tasks.filter { task ->
            val titleOk = titleContains.isNullOrBlank() || task.title.contains(titleContains, ignoreCase = true)
            val priorityOk = priority == null || task.priority == priority
            val doneOk = isDone == null || task.isDone == isDone
            titleOk && priorityOk && doneOk
        }
    }

    fun sortBy(field: SortField, order: SortOrder): List<Task> {
        val sorted = when (field) {
            SortField.CREATED_AT -> tasks.sortedBy { it.createdAt }
            SortField.PRIORITY -> tasks.sortedBy { it.priority.level }
            SortField.TITLE -> tasks.sortedBy { it.title.lowercase() }
        }
        return if (order == SortOrder.ASC) sorted else sorted.reversed()
    }

    fun saveToJsonFile(filePath: String): Result<Unit> {
        return runCatching {
            val file = File(filePath)
            val json = buildString {
                append("[\n")
                tasks.forEachIndexed { index, task ->
                    append("  ")
                    append(taskToJson(task))
                    if (index != tasks.lastIndex) append(",")
                    append("\n")
                }
                append("]")
            }
            file.writeText(json)
        }.onFailure { AppLogger.logError("Не удалось сохранить задачи в файл: $filePath", it as? Exception) }
    }

    fun loadFromJsonFile(filePath: String): Result<Unit> {
        return runCatching {
            val file = File(filePath)
            if (!file.exists()) error("Файл не найден: $filePath")
            val content = file.readText().trim()
            if (content.isBlank() || content == "[]") {
                tasks.clear()
                nextId = 1
                return@runCatching
            }

            val loaded = parseTasksFromJson(content)
            tasks.clear()
            tasks.addAll(loaded)
            nextId = (tasks.maxOfOrNull { it.id } ?: 0) + 1
        }.onFailure { AppLogger.logError("Не удалось загрузить задачи из файла: $filePath", it as? Exception) }
    }

    private fun taskToJson(task: Task): String {
        return """
{"id":${task.id},"title":"${escapeJson(task.title)}","description":"${escapeJson(task.description)}","priority":${task.priority.level},"isDone":${task.isDone},"createdAt":"${task.createdAt.format(dateFormatter)}"}
        """.trimIndent()
    }

    private fun parseTasksFromJson(content: String): List<Task> {
        val objectRegex = Regex("""\{[^{}]*}""")
        return objectRegex.findAll(content).map { match ->
            val obj = match.value
            val id = extractInt(obj, "id")
            val title = extractString(obj, "title")
            val description = extractString(obj, "description")
            val priority = Priority.fromLevel(extractInt(obj, "priority"))
                ?: error("Некорректный приоритет в задаче id=$id")
            val isDone = extractBoolean(obj, "isDone")
            val createdAtRaw = extractString(obj, "createdAt")
            val createdAt = LocalDateTime.parse(createdAtRaw, dateFormatter)
            Task(id, title, description, priority, isDone, createdAt)
        }.toList()
    }

    private fun extractInt(source: String, key: String): Int {
        val regex = Regex(""""$key"\s*:\s*(\d+)""")
        return regex.find(source)?.groupValues?.get(1)?.toIntOrNull()
            ?: error("Поле '$key' отсутствует или некорректно")
    }

    private fun extractBoolean(source: String, key: String): Boolean {
        val regex = Regex(""""$key"\s*:\s*(true|false)""")
        return regex.find(source)?.groupValues?.get(1)?.toBooleanStrictOrNull()
            ?: error("Поле '$key' отсутствует или некорректно")
    }

    private fun extractString(source: String, key: String): String {
        val regex = Regex(""""$key"\s*:\s*"((?:\\.|[^"\\])*)"""")
        val value = regex.find(source)?.groupValues?.get(1)
            ?: error("Поле '$key' отсутствует или некорректно")
        return unescapeJson(value)
    }

    private fun escapeJson(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun unescapeJson(value: String): String {
        return value
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }
}

enum class SortField {
    CREATED_AT,
    PRIORITY,
    TITLE
}

enum class SortOrder {
    ASC,
    DESC
}
