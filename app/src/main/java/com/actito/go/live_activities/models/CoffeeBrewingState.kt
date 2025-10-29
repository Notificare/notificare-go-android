package com.actito.go.live_activities.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class CoffeeBrewingState {
    @Json(name = "grinding")
    GRINDING,

    @Json(name = "brewing")
    BREWING,

    @Json(name = "served")
    SERVED,
}
