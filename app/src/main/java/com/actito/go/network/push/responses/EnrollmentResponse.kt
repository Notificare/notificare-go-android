package com.actito.go.network.push.responses

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EnrollmentResponse(
    val pass: Pass,
    val saveLinks: SaveLinks,
) {

    @JsonClass(generateAdapter = true)
    data class Pass(
        val serial: String,
        val barcode: String,
    )

    @JsonClass(generateAdapter = true)
    data class SaveLinks(
        val appleWallet: String,
        val googlePay: String,
    )
}
