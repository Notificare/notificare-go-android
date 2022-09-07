package re.notifica.go.ui

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import re.notifica.Notificare
import re.notifica.geo.ktx.geo
import re.notifica.go.core.createDynamicShortcuts
import re.notifica.go.core.loadRemoteConfig
import re.notifica.go.models.AppConfiguration
import re.notifica.go.network.push.PushService
import re.notifica.go.storage.preferences.NotificareSharedPreferences
import re.notifica.go.workers.UpdateProductsWorker
import re.notifica.iam.ktx.inAppMessaging
import re.notifica.models.NotificareApplication
import re.notifica.push.ktx.push
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: NotificareSharedPreferences,
    private val pushService: PushService,
    private val workManager: WorkManager,
) : ViewModel(), Notificare.Listener {

    private val navigationChannel = Channel<NavigationOption>(Channel.BUFFERED)
    val navigationFlow: Flow<NavigationOption> = navigationChannel.receiveAsFlow()

    private val hasConfiguration: Boolean
        get() {
            return preferences.appConfiguration != null
        }


    init {
        Notificare.addListener(this)

        if (!hasConfiguration) {
            Timber.d("No configuration available.")
            navigationChannel.trySend(NavigationOption.SCANNER)
        } else {
            launch()
        }
    }

    override fun onCleared() {
        Notificare.removeListener(this)
    }


    suspend fun configure(code: String): ConfigurationResult = withContext(Dispatchers.IO) {
        if (hasConfiguration) return@withContext ConfigurationResult.ALREADY_CONFIGURED

        val configuration = pushService.getConfiguration(code).let {
            AppConfiguration(
                applicationKey = it.demo.applicationKey,
                applicationSecret = it.demo.applicationSecret,
                loyaltyProgramId = it.demo.loyaltyProgram,
            )
        }

        configure(configuration)
        ConfigurationResult.SUCCESS
    }

    fun configure(configuration: AppConfiguration) {
        // Persist the configuration.
        preferences.appConfiguration = configuration
    }

    fun launch() {
        val configuration = preferences.appConfiguration
            ?: throw IllegalStateException("Cannot launch Notificare before the application has been configured.")

        if (!Notificare.isConfigured) {
            Notificare.configure(context, configuration.applicationKey, configuration.applicationSecret)
        }

        // Let's get started! 🚀
        Notificare.launch()
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
            Notificare.inAppMessaging().hasMessagesSuppressed = true
            navigationChannel.trySend(NavigationOption.INTRO)
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
            createDynamicShortcuts(context, preferences)
            navigationChannel.trySend(NavigationOption.MAIN)
        }
    }

    // endregion

    enum class NavigationOption {
        SCANNER,
        INTRO,
        MAIN;
    }

    enum class ConfigurationResult {
        ALREADY_CONFIGURED,
        SUCCESS,
    }
}
