package com.actito.go.ktx

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.actito.Actito
import com.actito.ActitoEventData
import com.actito.ActitoEventsModule
import com.actito.geo.ActitoGeo
import com.actito.go.core.formatPrice
import com.actito.go.models.Product

val ActitoGeo.hasLocationTrackingCapabilities: Boolean
    get() {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        val hasLocationPermissions = ContextCompat.checkSelfPermission(
            Actito.requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED

        return hasLocationServicesEnabled && hasLocationPermissions
    }

val ActitoGeo.hasGeofencingCapabilities: Boolean
    get() {
        val permission = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> Manifest.permission.ACCESS_BACKGROUND_LOCATION
            else -> Manifest.permission.ACCESS_FINE_LOCATION
        }

        val hasLocationPermissions = ContextCompat.checkSelfPermission(
            Actito.requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED

        return hasLocationServicesEnabled && hasLocationPermissions
    }

suspend fun ActitoEventsModule.logIntroFinished() {
    logCustom("intro_finished")
}

suspend fun ActitoEventsModule.logPageViewed(page: PageView) {
    logCustom("page_viewed.${page.rawValue}")
}

suspend fun ActitoEventsModule.logAddToCart(product: Product) {
    val data: ActitoEventData = mapOf(
        "product" to product.let(::transformProduct)
    )

    logCustom("add_to_cart", data)
}

suspend fun ActitoEventsModule.logRemoveFromCart(product: Product) {
    val data: ActitoEventData = mapOf(
        "product" to product.let(::transformProduct)
    )

    logCustom("remove_from_cart", data)
}

suspend fun ActitoEventsModule.logCartUpdated(products: List<Product>) {
    val data: ActitoEventData = mapOf(
        "products" to products.map { it.let(::transformProduct) }
    )

    logCustom("cart_updated", data)
}

suspend fun ActitoEventsModule.logCartCleared() {
    logCustom("cart_cleared")
}

suspend fun ActitoEventsModule.logPurchase(products: List<Product>) {
    val total = products.sumOf { it.price }
    val data: ActitoEventData = mapOf(
        "total_price" to total,
        "total_price_formatted" to formatPrice(total),
        "total_items" to products.size,
        "products" to products.map { it.let(::transformProduct) }
    )

    logCustom("purchase", data)
}

suspend fun ActitoEventsModule.logProductView(product: Product) {
    val data: ActitoEventData = mapOf(
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

enum class PageView {
    HOME,
    CART,
    SETTINGS,
    INBOX,
    USER_PROFILE,
    EVENTS,
    PRODUCTS,
    PRODUCT_DETAILS;

    val rawValue: String
        get() = when (this) {
            HOME -> "home"
            CART -> "cart"
            SETTINGS -> "settings"
            INBOX -> "inbox"
            USER_PROFILE -> "user_profile"
            EVENTS -> "events"
            PRODUCTS -> "products"
            PRODUCT_DETAILS -> "product_details"
        }
}
