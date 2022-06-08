package re.notifica.go.storage.preferences

import android.content.Context
import androidx.core.content.edit
import com.squareup.moshi.Moshi
import re.notifica.go.ktx.getMoshi
import re.notifica.go.ktx.putMoshi
import re.notifica.go.models.AppConfiguration

class NotificareSharedPreferences(
    context: Context,
    private val moshi: Moshi,
) {
    private val preferences = context.getSharedPreferences("re.notifica.go.preferences", Context.MODE_PRIVATE)

    var appConfiguration: AppConfiguration?
        get() = preferences.getMoshi(moshi, "app_configuration")
        set(value) = preferences.edit { putMoshi(moshi, "app_configuration", value) }

    var hasIntroFinished: Boolean
        get() = preferences.getBoolean("intro_finished", false)
        set(value) = preferences.edit { putBoolean("intro_finished", value) }

    var hasStoreEnabled: Boolean
        get() = preferences.getBoolean("has_store_enabled", false)
        set(value) = preferences.edit { putBoolean("has_store_enabled", value) }

    var membershipCardUrl: String?
        get() = preferences.getString("membership_card_url", null)
        set(value) = preferences.edit { putString("membership_card_url", value) }
}
