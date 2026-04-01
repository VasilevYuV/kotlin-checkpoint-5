fun main() {
    val repository = TaskRepository()
    val controller = MenuController(repository)
    controller.run()
}