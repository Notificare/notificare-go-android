package com.actito.go.live_activities

enum class LiveActivity {
    COFFEE_BREWER,
    ORDER_STATUS;

    val identifier: String
        get() = when (this) {
            COFFEE_BREWER -> "coffee-brewer"
            ORDER_STATUS -> "order-status"
        }

    companion object {
        fun from(identifier: String): LiveActivity? {
            return entries.firstOrNull { it.identifier == identifier }
        }
    }
}
