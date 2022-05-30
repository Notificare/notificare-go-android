package re.notifica.go.core

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import re.notifica.Notificare
import re.notifica.assets.ktx.assets
import re.notifica.go.storage.preferences.NotificareSharedPreferences
import re.notifica.internal.network.NetworkException
import timber.log.Timber

fun extractConfigurationCode(uri: Uri): String? {
    if (uri.scheme != "https") {
        Timber.w("Invalid URI scheme.")
        return null
    }

    if (uri.host != "go-demo.ntc.re" && uri.host != "go-demo-dev.ntc.re") {
        Timber.w("Invalid URI host.")
        return null
    }

    val code = uri.getQueryParameter("referrer") ?: run {
        Timber.w("Invalid URI code query parameter.")
        return null
    }

    return code
}

suspend fun loadRemoteConfig(preferences: NotificareSharedPreferences): Unit = withContext(Dispatchers.IO) {
    try {
        val assets = Notificare.assets().fetch(group = "config")
        val storeEnabled = assets.firstOrNull()?.extra?.get("storeEnabled") as? Boolean

        if (storeEnabled != null) {
            preferences.hasStoreEnabled = storeEnabled
            return@withContext
        }
    } catch (e: Exception) {
        if (e is NetworkException.ValidationException && e.response.code == 404) {
            // The config asset group is not available. The store can be enabled.
            preferences.hasStoreEnabled = true
            return@withContext
        }

        Timber.e(e, "Failed to fetch the remote config.")
    }

    preferences.hasStoreEnabled = false
}
