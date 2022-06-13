package re.notifica.go.network.assets

import re.notifica.Notificare
import re.notifica.assets.ktx.assets
import re.notifica.go.models.Product

class AssetsService {
    suspend fun getProducts(): List<Product> {
        return Notificare.assets().fetch(group = "products").mapNotNull { asset ->
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
