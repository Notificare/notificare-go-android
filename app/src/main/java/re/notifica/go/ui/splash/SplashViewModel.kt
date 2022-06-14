package re.notifica.go.ui.splash

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.geo.ktx.geo
import re.notifica.go.core.loadRemoteConfig
import re.notifica.go.storage.preferences.NotificareSharedPreferences
import re.notifica.go.workers.UpdateProductsWorker
import re.notifica.models.NotificareApplication
import re.notifica.push.ktx.push
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class SplashViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: NotificareSharedPreferences,
    private val workManager: WorkManager,
) : ViewModel(), Notificare.Listener {

    private val _navigationFlow = MutableSharedFlow<NavigationOption>(1, 0)
    val navigationFlow: Flow<NavigationOption> = _navigationFlow

    init {
        Notificare.addListener(this)

        val appConfiguration = preferences.appConfiguration

        if (appConfiguration == null) {
            Timber.d("No configuration available.")
            viewModelScope.launch {
                _navigationFlow.emit(NavigationOption.SCANNER)
            }
        } else {
            if (!Notificare.isConfigured) {
                Notificare.configure(context, appConfiguration.applicationKey, appConfiguration.applicationSecret)
            }

            // Let's get started! 🚀
            Notificare.launch()
        }
    }

    override fun onCleared() {
        Notificare.removeListener(this)
    }

    // region Notificare.Listener

    override fun onReady(application: NotificareApplication) {
        // Schedule a worker that updates the products database.
        workManager.enqueue(
            OneTimeWorkRequestBuilder<UpdateProductsWorker>()
                .build()
        )

        val user = Firebase.auth.currentUser
        if (!preferences.hasIntroFinished || user == null) {
            _navigationFlow.tryEmit(NavigationOption.INTRO)
            return
        }

        if (Notificare.push().hasRemoteNotificationsEnabled) {
            Notificare.push().enableRemoteNotifications()
        }

        if (Notificare.geo().hasLocationServicesEnabled) {
            Notificare.geo().enableLocationUpdates()
        }

        viewModelScope.launch {
            loadRemoteConfig(preferences)
            _navigationFlow.tryEmit(NavigationOption.MAIN)
        }
    }

    // endregion

    enum class NavigationOption {
        SCANNER,
        INTRO,
        MAIN;
    }
}
