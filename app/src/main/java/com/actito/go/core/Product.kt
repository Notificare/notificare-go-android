package com.actito.go.core

fun formatPrice(price: Double): String {
    val hasDecimals = price % 1 != 0.0

    if (!hasDecimals) {
        return "€%.0f".format(price)
    }

    return "€%.2f".format(price)
}
