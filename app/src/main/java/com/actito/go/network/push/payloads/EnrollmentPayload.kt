package com.actito.go.network.push.payloads

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EnrollmentPayload(
    @param:Json(name = "userID") val userId: String,
    val memberId: String,
    val fields: List<Field>,
) {

    @JsonClass(generateAdapter = true)
    data class Field(
        val key: String,
        val value: String,
    )
}
