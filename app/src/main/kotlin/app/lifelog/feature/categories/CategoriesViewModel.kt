package app.lifelog.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lifelog.designsystem.component.CategoryPalette
import app.lifelog.domain.model.Category
import app.lifelog.domain.premium.FeatureGate
import app.lifelog.domain.premium.FeatureLimits
import app.lifelog.domain.repository.CategoryRepository
import app.lifelog.domain.usecase.SaveCategoryResult
import app.lifelog.domain.usecase.SaveCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryDialogState(
    val id: Long = 0,
    val name: String = "",
    val colorArgb: Int,
    val isCustomColor: Boolean = false,
    val nameError: Boolean = false,
) {
    val isNew: Boolean get() = id == 0L
}

data class CategoriesUiState(
    val isLoading: Boolean = true,
    val categories: List<Category> = emptyList(),
    val limits: FeatureLimits = FeatureLimits(isPremium = false),
    val dialog: CategoryDialogState? = null,
    val pendingDelete: Category? = null,
    /** Set when the free-tier limit blocked adding a category. */
    val limitReached: Int? = null,
) {
    val canAdd: Boolean get() = limits.canCreateCategory(categories.size)
}

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val repository: CategoryRepository,
    private val saveCategory: SaveCategoryUseCase,
    featureGate: FeatureGate,
) : ViewModel() {

    private data class Local(
        val dialog: CategoryDialogState? = null,
        val pendingDelete: Category? = null,
        val limitReached: Int? = null,
    )

    private val local = MutableStateFlow(Local())

    val uiState: StateFlow<CategoriesUiState> =
        combine(repository.categories, featureGate.limits, local) { categories, limits, local ->
            CategoriesUiState(
                isLoading = false,
                categories = categories,
                limits = limits,
                dialog = local.dialog,
                pendingDelete = local.pendingDelete,
                limitReached = local.limitReached,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    fun onAdd() {
        val state = uiState.value
        if (!state.canAdd) {
            local.update { it.copy(limitReached = state.limits.maxCategories) }
            return
        }
        // Suggest the first preset color that isn't used yet.
        val used = state.categories.map { it.colorArgb }.toSet()
        val color = CategoryPalette.presets.firstOrNull { it.argb !in used } ?: CategoryPalette.presets.first()
        local.update { it.copy(dialog = CategoryDialogState(colorArgb = color.argb)) }
    }

    fun onEdit(category: Category) = local.update {
        it.copy(
            dialog = CategoryDialogState(
                id = category.id,
                name = category.name,
                colorArgb = category.colorArgb,
                isCustomColor = category.isCustomColor,
            ),
        )
    }

    fun onNameChange(name: String) = local.update { it.copy(dialog = it.dialog?.copy(name = name, nameError = false)) }

    fun onColorChange(argb: Int) = local.update { it.copy(dialog = it.dialog?.copy(colorArgb = argb, isCustomColor = false)) }

    fun onDismissDialog() = local.update { it.copy(dialog = null) }

    fun onSaveDialog() {
        val dialog = local.value.dialog ?: return
        viewModelScope.launch {
            val result = saveCategory(
                Category(id = dialog.id, name = dialog.name, colorArgb = dialog.colorArgb, isCustomColor = dialog.isCustomColor),
            )
            when (result) {
                is SaveCategoryResult.Saved -> local.update { it.copy(dialog = null) }
                SaveCategoryResult.NameMissing -> local.update { it.copy(dialog = it.dialog?.copy(nameError = true)) }
                is SaveCategoryResult.LimitReached -> local.update { it.copy(dialog = null, limitReached = result.limit) }
            }
        }
    }

    fun onDeleteRequest(category: Category) = local.update { it.copy(pendingDelete = category) }

    fun onDismissDelete() = local.update { it.copy(pendingDelete = null) }

    fun onConfirmDelete() {
        val category = local.value.pendingDelete ?: return
        local.update { it.copy(pendingDelete = null) }
        viewModelScope.launch { repository.delete(category.id) }
    }

    fun onLimitMessageDismissed() = local.update { it.copy(limitReached = null) }
}
