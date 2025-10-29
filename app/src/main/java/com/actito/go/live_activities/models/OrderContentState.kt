package com.actito.go.live_activities.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OrderContentState(
    val status: OrderStatus,
)
