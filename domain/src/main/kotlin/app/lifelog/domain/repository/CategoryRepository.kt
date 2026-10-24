package app.lifelog.domain.repository

import app.lifelog.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    /** All categories in display order. */
    val categories: Flow<List<Category>>

    suspend fun count(): Int

    /** Inserts ([Category.id] == 0, appended at the end) or updates name and color. */
    suspend fun save(category: Category): Long

    /** Events in the category become uncategorized. */
    suspend fun delete(id: Long)
}
