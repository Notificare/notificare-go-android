package com.actito.go.live_activities.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class OrderStatus {
    @Json(name = "preparing")
    PREPARING,

    @Json(name = "shipped")
    SHIPPED,

    @Json(name = "delivered")
    DELIVERED,
}
