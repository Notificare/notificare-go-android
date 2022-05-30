package re.notifica.go.network.assets

import re.notifica.Notificare
import re.notifica.assets.ktx.assets
import re.notifica.go.models.Product

class AssetsService {
    suspend fun getProducts(): List<Product> {
        return Notificare.assets().fetch(group = "products").map { asset ->
            Product(
                id = checkNotNull(asset.extra["id"] as? String),
                name = asset.title,
                description = checkNotNull(asset.description),
                price = checkNotNull(asset.extra["price"] as? Double),
                imageUrl = checkNotNull(asset.url),
                highlighted = checkNotNull(asset.extra["highlighted"] as? Boolean),
            )
        }
    }
}
