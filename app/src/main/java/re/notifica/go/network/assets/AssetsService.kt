package re.notifica.go.network.assets

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import re.notifica.Notificare
import re.notifica.assets.ktx.assets
import re.notifica.go.models.Product

class AssetsService(moshi: Moshi) {
    private val adapter = moshi.adapter<List<Product>>(
        Types.newParameterizedType(List::class.java, Product::class.java)
    )

    suspend fun getProducts(): List<Product> {
        val json = Notificare.assets().fetch(group = "products")
            .firstOrNull()
            ?.extra
            ?.get("products")

        return adapter.fromJsonValue(json) ?: emptyList()
    }
}
