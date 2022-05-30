package re.notifica.go.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AppConfiguration(
    val applicationKey: String,
    val applicationSecret: String,
    // val loyaltyProgram: String,
)
