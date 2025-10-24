package com.actito.go.ui

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.actito.Actito
import com.actito.go.core.createDynamicShortcuts
import com.actito.go.core.loadRemoteConfig
import com.actito.go.models.AppConfiguration
import com.actito.go.network.push.PushService
import com.actito.go.storage.preferences.ActitoSharedPreferences
import com.actito.go.workers.UpdateProductsWorker
import com.actito.iam.ktx.inAppMessaging
import com.actito.ktx.device
import com.actito.models.ActitoApplication
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class MainViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferences: ActitoSharedPreferences,
    private val pushService: PushService,
    private val workManager: WorkManager,
) : ViewModel(), Actito.Listener {

    private val navigationChannel = Channel<NavigationOption>(Channel.BUFFERED)
    val navigationFlow: Flow<NavigationOption> = navigationChannel.receiveAsFlow()

    private val hasConfiguration: Boolean
        get() {
            return preferences.appConfiguration != null
        }


    init {
        if (!Actito.isReady) {
            // Ensure we don't leave the app in an unstable state.
            // When the app is recreated, ie due to permission changes,
            // we should reset to the splash while Actito is launching.
            navigationChannel.trySend(NavigationOption.SPLASH)
        }

        Actito.addListener(this)

        if (!hasConfiguration) {
            Timber.d("No configuration available.")
            navigationChannel.trySend(NavigationOption.SCANNER)
        } else {
            launch()
        }
    }

    override fun onCleared() {
        Actito.removeListener(this)
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
            ?: throw IllegalStateException("Cannot launch Actito before the application has been configured.")

        if (!Actito.isConfigured) {
            Actito.configure(context, configuration.applicationKey, configuration.applicationSecret)
        }

        // Let's get started! 🚀
        viewModelScope.launch {
            try {
                Actito.launch()
            } catch (e: Exception) {
                Timber.e(e, "Failed to launch Actito.")
            }
        }
    }


    // region Actito.Listener

    override fun onReady(application: ActitoApplication) {
        // Schedule a worker that updates the products database.
        workManager.enqueue(
            OneTimeWorkRequestBuilder<UpdateProductsWorker>()
                .build()
        )

        val user = Firebase.auth.currentUser
        if (!preferences.hasIntroFinished || user == null) {
            Actito.inAppMessaging().hasMessagesSuppressed = true
            navigationChannel.trySend(NavigationOption.INTRO)
            return
        }

        viewModelScope.launch {
            loadRemoteConfig(preferences)
            createDynamicShortcuts(context, preferences)

            try {
                Actito.device().updateUser(user.uid, user.displayName)
            } catch (e: Exception) {
                Timber.e(e, "Failed to update user.")
            }

            navigationChannel.trySend(NavigationOption.MAIN)
        }
    }

    // endregion

    enum class NavigationOption {
        SPLASH,
        SCANNER,
        INTRO,
        MAIN;
    }

    enum class ConfigurationResult {
        ALREADY_CONFIGURED,
        SUCCESS,
    }
}
