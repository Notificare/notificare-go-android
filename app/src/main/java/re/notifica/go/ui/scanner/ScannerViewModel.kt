package re.notifica.go.ui.scanner

import android.net.Uri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import re.notifica.go.core.extractConfigurationCode
import re.notifica.go.models.AppConfiguration
import re.notifica.go.network.push.PushService
import re.notifica.go.storage.preferences.NotificareSharedPreferences
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val pushService: PushService,
    private val preferences: NotificareSharedPreferences,
) : ViewModel() {

    suspend fun configure(barcode: String): Unit = withContext(Dispatchers.IO) {
        val code = extractConfigurationCode(Uri.parse(barcode))

        if (code == null) {
            Timber.w("Invalid URI code query parameter.")
            throw IllegalArgumentException("Invalid URI code query parameter.")
        }

        val configuration = pushService.getConfiguration(code)

        // Persist the configuration.
        preferences.appConfiguration = AppConfiguration(
            applicationKey = configuration.demo.applicationKey,
            applicationSecret = configuration.demo.applicationSecret,
        )
    }
}
