enum class Priority(val level: Int) {
    VERY_LOW(1),
    LOW(2),
    MEDIUM(3),
    HIGH(4),
    CRITICAL(5);

    companion object {
        fun fromLevel(level: Int): Priority? = entries.firstOrNull { it.level == level }
    }
}
