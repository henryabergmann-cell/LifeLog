package app.lifelog.domain.model

data class Category(
    val id: Long = 0,
    val name: String,
    val colorArgb: Int,
    /** Picked with the Premium custom color picker rather than from the preset palette. */
    val isCustomColor: Boolean = false,
    val sortOrder: Int = 0,
)
