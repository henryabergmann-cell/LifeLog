package app.lifelog.data.mapper

import app.lifelog.data.db.entity.CategoryEntity
import app.lifelog.domain.model.Category

internal fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    colorArgb = colorArgb,
    isCustomColor = isCustomColor,
    sortOrder = sortOrder,
)
