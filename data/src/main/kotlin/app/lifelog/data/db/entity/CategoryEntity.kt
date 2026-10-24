package app.lifelog.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
internal data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorArgb: Int,
    /** True when the color was picked with the Premium custom color picker. */
    val isCustomColor: Boolean,
    val sortOrder: Int,
    val createdAt: Long,
)
