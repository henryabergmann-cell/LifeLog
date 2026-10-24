package app.lifelog.domain.usecase

import app.lifelog.domain.model.Category
import app.lifelog.domain.premium.FeatureGate
import app.lifelog.domain.repository.CategoryRepository
import javax.inject.Inject

class SaveCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository,
    private val featureGate: FeatureGate,
) {
    suspend operator fun invoke(category: Category): SaveCategoryResult {
        val name = category.name.trim()
        if (name.isEmpty()) return SaveCategoryResult.NameMissing
        if (category.id == 0L) {
            val limits = featureGate.currentLimits()
            if (!limits.canCreateCategory(repository.count())) {
                return SaveCategoryResult.LimitReached(limits.maxCategories)
            }
        }
        return SaveCategoryResult.Saved(repository.save(category.copy(name = name)))
    }
}

sealed interface SaveCategoryResult {
    data class Saved(val categoryId: Long) : SaveCategoryResult
    data object NameMissing : SaveCategoryResult
    data class LimitReached(val limit: Int) : SaveCategoryResult
}
