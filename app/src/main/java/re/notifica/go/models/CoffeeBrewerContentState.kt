package re.notifica.go.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CoffeeBrewerContentState(
    val state: CoffeeBrewingState,
    val remaining: Int,
)
