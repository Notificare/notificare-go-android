package com.actito.go.network.push.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Configuration(
    val demo: Demo,
) {

    @JsonClass(generateAdapter = true)
    data class Demo(
        val applicationKey: String,
        val applicationSecret: String,
        val loyaltyProgram: String?,
    )
}
