package re.notifica.go.ktx

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import re.notifica.Notificare
import re.notifica.NotificareEventsModule
import re.notifica.geo.NotificareGeo
import re.notifica.go.core.formatPrice
import re.notifica.go.models.Product
import re.notifica.models.NotificareEventData

val NotificareGeo.hasLocationTrackingCapabilities: Boolean
    get() {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        val hasLocationPermissions = ContextCompat.checkSelfPermission(
            Notificare.requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED

        return hasLocationServicesEnabled && hasLocationPermissions
    }

val NotificareGeo.hasGeofencingCapabilities: Boolean
    get() {
        val permission = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> Manifest.permission.ACCESS_BACKGROUND_LOCATION
            else -> Manifest.permission.ACCESS_FINE_LOCATION
        }

        val hasLocationPermissions = ContextCompat.checkSelfPermission(
            Notificare.requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED

        return hasLocationServicesEnabled && hasLocationPermissions
    }


suspend fun NotificareEventsModule.logAddToCart(product: Product) {
    val data: NotificareEventData = mapOf(
        "product" to product.let(::transformProduct)
    )

    logCustom("add_to_cart", data)
}

suspend fun NotificareEventsModule.logRemoveFromCart(product: Product) {
    val data: NotificareEventData = mapOf(
        "product" to product.let(::transformProduct)
    )

    logCustom("remove_from_cart", data)
}

suspend fun NotificareEventsModule.logCartUpdated(products: List<Product>) {
    val data: NotificareEventData = mapOf(
        "products" to products.map { it.let(::transformProduct) }
    )

    logCustom("cart_updated", data)
}

suspend fun NotificareEventsModule.logCartCleared() {
    logCustom("cart_cleared")
}

suspend fun NotificareEventsModule.logPurchase(products: List<Product>) {
    val total = products.sumOf { it.price }
    val data: NotificareEventData = mapOf(
        "total_price" to total,
        "total_price_formatted" to formatPrice(total),
        "total_items" to products.size,
        "products" to products.map { it.let(::transformProduct) }
    )

    logCustom("purchase", data)
}

suspend fun NotificareEventsModule.logProductView(product: Product) {
    val data: NotificareEventData = mapOf(
        "product" to product.let(::transformProduct)
    )

    logCustom("product_viewed", data)
}


private fun transformProduct(product: Product): Map<String, Any> {
    return mapOf(
        "id" to product.id,
        "name" to product.name,
        "price" to product.price,
        "price_formatted" to product.price.let(::formatPrice),
    )
}
