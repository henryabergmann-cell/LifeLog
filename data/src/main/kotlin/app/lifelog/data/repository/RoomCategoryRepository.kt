package app.lifelog.data.repository

import app.lifelog.data.db.dao.CategoryDao
import app.lifelog.data.db.entity.CategoryEntity
import app.lifelog.data.mapper.toDomain
import app.lifelog.domain.model.Category
import app.lifelog.domain.repository.CategoryRepository
import app.lifelog.domain.time.TimeSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class RoomCategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val timeSource: TimeSource,
) : CategoryRepository {

    override val categories: Flow<List<Category>> =
        categoryDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun count(): Int = categoryDao.count()

    override suspend fun save(category: Category): Long {
        val existing = if (category.id != 0L) categoryDao.getById(category.id) else null
        if (existing != null) {
            categoryDao.update(
                existing.copy(name = category.name, colorArgb = category.colorArgb, isCustomColor = category.isCustomColor),
            )
            return existing.id
        }
        return categoryDao.insert(
            CategoryEntity(
                name = category.name,
                colorArgb = category.colorArgb,
                isCustomColor = category.isCustomColor,
                sortOrder = categoryDao.maxSortOrder() + 1,
                createdAt = timeSource.now().toEpochMilli(),
            ),
        )
    }

    override suspend fun delete(id: Long) = categoryDao.delete(id)
}
