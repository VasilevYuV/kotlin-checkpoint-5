import java.time.format.DateTimeFormatter

class MenuController(private val repository: TaskRepository) {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun run() {
        println("=== Task Manager ===")
        while (true) {
            showMenu()
            when (readInt("Выберите пункт меню: ")) {
                1 -> createTask()
                2 -> showAllTasks()
                3 -> findTasks()
                4 -> editTask()
                5 -> deleteTask()
                6 -> sortTasks()
                7 -> saveTasks()
                8 -> loadTasks()
                9 -> markMultipleTasks()
                0 -> {
                    println("Выход из программы.")
                    return
                }
                else -> println("Неизвестный пункт меню. Попробуйте снова.")
            }
        }
    }

    private fun showMenu() {
        println(
            """
            
            1 - Создать задачу
            2 - Показать все задачи
            3 - Найти задачу
            4 - Редактировать задачу
            5 - Удалить задачу
            6 - Сортировка
            7 - Сохранить в файл
            8 - Загрузить из файла
            9 - Отметить несколько задач
            0 - Выход
            """.trimIndent()
        )
    }

    private fun createTask() {
        println("--- Создание задачи ---")
        val title = readNonBlank("Введите title: ")
        val description = readLineSafe("Введите description: ")
        val priority = readPriority("Введите priority (1-5): ")

        val task = repository.addTask(title, description, priority)
        println("Задача создана. ID: ${task.id}")
    }

    private fun showAllTasks() {
        println("--- Все задачи ---")
        printTasks(repository.getAllTasks())
    }

    private fun findTasks() {
        println("--- Поиск задач (можно комбинировать условия) ---")
        val title = readLineSafe("Название содержит (Enter - пропустить): ").ifBlank { null }
        val priority = readPriorityOptional("Приоритет (1-5, Enter - пропустить): ")
        val done = readBooleanOptional("Статус выполнено (y/n, Enter - пропустить): ")

        val found = repository.search(titleContains = title, priority = priority, isDone = done)
        println("Найдено: ${found.size}")
        printTasks(found)
    }

    private fun editTask() {
        println("--- Редактирование задачи ---")
        val id = readInt("Введите ID задачи: ")
        val task = repository.findById(id)
        if (task == null) {
            println("Задача с ID=$id не найдена.")
            return
        }

        println("Оставьте поле пустым, чтобы сохранить текущее значение.")
        val newTitle = readLineSafe("Новый title [${task.title}]: ").ifBlank { null }
        val newDescription = readLineSafe("Новый description [${task.description}]: ").ifBlank { null }
        val newPriority = readPriorityOptional("Новый priority (1-5) [${task.priority.level}]: ")
        val newDone = readBooleanOptional("Новый статус done (y/n) [${if (task.isDone) "y" else "n"}]: ")

        val updated = repository.editTask(
            id = id,
            title = newTitle,
            description = newDescription,
            priority = newPriority,
            isDone = newDone
        )
        if (updated) println("Задача обновлена.") else println("Не удалось обновить задачу.")
    }

    private fun deleteTask() {
        println("--- Удаление задачи ---")
        val id = readInt("Введите ID задачи: ")
        val task = repository.findById(id)
        if (task == null) {
            println("Задача с ID=$id не найдена.")
            return
        }

        val confirm = readYesNo("Вы уверены, что хотите удалить? (y/n): ")
        if (!confirm) {
            println("Удаление отменено.")
            return
        }

        if (repository.deleteTask(id)) println("Задача удалена.") else println("Ошибка удаления.")
    }

    private fun sortTasks() {
        println("--- Сортировка ---")
        val field = when (readInt("Поле: 1-дата, 2-приоритет, 3-название: ")) {
            1 -> SortField.CREATED_AT
            2 -> SortField.PRIORITY
            3 -> SortField.TITLE
            else -> {
                println("Некорректный выбор поля.")
                return
            }
        }

        val order = when (readInt("Порядок: 1-по возрастанию, 2-по убыванию: ")) {
            1 -> SortOrder.ASC
            2 -> SortOrder.DESC
            else -> {
                println("Некорректный выбор порядка.")
                return
            }
        }

        val sorted = repository.sortBy(field, order)
        printTasks(sorted)
    }

    private fun saveTasks() {
        println("--- Сохранение в JSON ---")
        val path = readLineSafe("Путь к файлу (например tasks.json): ").ifBlank { "tasks.json" }
        repository.saveToJsonFile(path)
            .onSuccess { println("Задачи сохранены в $path") }
            .onFailure { println("Ошибка сохранения. Подробности в errors.log") }
    }

    private fun loadTasks() {
        println("--- Загрузка из JSON ---")
        val path = readLineSafe("Путь к файлу (например tasks.json): ").ifBlank { "tasks.json" }
        repository.loadFromJsonFile(path)
            .onSuccess { println("Задачи загружены из $path") }
            .onFailure { println("Ошибка загрузки. Подробности в errors.log") }
    }

    private fun markMultipleTasks() {
        println("--- Массовая отметка задач ---")
        printTasks(repository.getAllTasks())
        val rawIds = readLineSafe("Введите ID через запятую (например 1,2,5): ")
        val ids = rawIds.split(",")
            .mapNotNull { it.trim().toIntOrNull() }

        if (ids.isEmpty()) {
            println("Список ID пуст или некорректен.")
            return
        }

        val done = readYesNo("Пометить как выполненные? (y/n): ")
        val changed = repository.markTasksDone(ids, done)
        println("Обновлено задач: $changed")
    }

    private fun printTasks(tasks: List<Task>) {
        if (tasks.isEmpty()) {
            println("Список задач пуст.")
            return
        }

        println(
            String.format(
                "%-4s %-25s %-8s %-6s %-17s",
                "ID",
                "Title",
                "Prio",
                "Done",
                "CreatedAt"
            )
        )
        println("-".repeat(65))

        tasks.forEach { task ->
            val shortTitle = if (task.title.length > 24) task.title.take(21) + "..." else task.title
            println(
                String.format(
                    "%-4d %-25s %-8d %-6s %-17s",
                    task.id,
                    shortTitle,
                    task.priority.level,
                    if (task.isDone) "Yes" else "No",
                    task.createdAt.format(dateFormatter)
                )
            )
        }
    }

    private fun readInt(prompt: String): Int {
        while (true) {
            val value = readLineSafe(prompt).trim()
            val parsed = value.toIntOrNull()
            if (parsed != null) return parsed
            println("Введите целое число.")
        }
    }

    private fun readPriority(prompt: String): Priority {
        while (true) {
            val level = readInt(prompt)
            val priority = Priority.fromLevel(level)
            if (priority != null) return priority
            println("Приоритет должен быть от 1 до 5.")
        }
    }

    private fun readPriorityOptional(prompt: String): Priority? {
        while (true) {
            val value = readLineSafe(prompt).trim()
            if (value.isEmpty()) return null
            val parsed = value.toIntOrNull()
            val priority = parsed?.let { Priority.fromLevel(it) }
            if (priority != null) return priority
            println("Введите число от 1 до 5 или оставьте пустым.")
        }
    }

    private fun readBooleanOptional(prompt: String): Boolean? {
        while (true) {
            val value = readLineSafe(prompt).trim().lowercase()
            if (value.isEmpty()) return null
            if (value == "y" || value == "yes") return true
            if (value == "n" || value == "no") return false
            println("Введите y/n или оставьте пустым.")
        }
    }

    private fun readYesNo(prompt: String): Boolean {
        while (true) {
            val value = readLineSafe(prompt).trim().lowercase()
            if (value == "y" || value == "yes") return true
            if (value == "n" || value == "no") return false
            println("Введите y или n.")
        }
    }

    private fun readNonBlank(prompt: String): String {
        while (true) {
            val value = readLineSafe(prompt).trim()
            if (value.isNotEmpty()) return value
            println("Поле не может быть пустым.")
        }
    }

    private fun readLineSafe(prompt: String): String {
        print(prompt)
        return readlnOrNull()?.trimEnd() ?: ""
    }
}
