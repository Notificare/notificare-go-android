package com.actito.go.storage.db.mappers

import com.actito.go.models.Product
import com.actito.go.storage.db.entities.ProductEntity

internal fun ProductEntity.toModel(): Product {
    return Product(
        id = id,
        name = name,
        description = description,
        price = price,
        imageUrl = imageUrl,
        highlighted = isHighlighted
    )
}

internal fun Product.toEntity(): ProductEntity {
    return ProductEntity(
        id = id,
        name = name,
        description = description,
        price = price,
        imageUrl = imageUrl,
        isHighlighted = highlighted
    )
}
