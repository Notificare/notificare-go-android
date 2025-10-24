package com.actito.go.network.assets

import com.actito.Actito
import com.actito.assets.ktx.assets
import com.actito.go.models.Product

class AssetsService {
    suspend fun getProducts(): List<Product> {
        return Actito.assets().fetch(group = "products").mapNotNull { asset ->
            Product(
                id = asset.extra["id"] as? String ?: return@mapNotNull null,
                name = asset.title,
                description = asset.description ?: return@mapNotNull null,
                price = asset.extra["price"] as? Double ?: return@mapNotNull null,
                imageUrl = asset.url ?: return@mapNotNull null,
                highlighted = asset.extra["highlighted"] as? Boolean ?: return@mapNotNull null,
            )
        }
    }
}
