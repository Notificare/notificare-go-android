package com.actito.go

import android.app.Application
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.actito.Actito
import com.actito.go.live_activities.LiveActivitiesController
import com.actito.go.storage.preferences.ActitoSharedPreferences
import com.actito.push.ktx.push
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class ActitoApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var preferences: ActitoSharedPreferences

    @Inject
    lateinit var liveActivitiesController: LiveActivitiesController

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Apply dynamic colouring based on the user's wallpaper.
        DynamicColors.applyToActivitiesIfAvailable(this)

        // Plant a debug tree. 🌱
        Timber.plant(Timber.DebugTree())

        // Configure Actito if there is a stored configuration set.
        val configuration = preferences.appConfiguration
        if (configuration != null) {
            Actito.configure(this, configuration.applicationKey, configuration.applicationSecret)
        }

        Actito.push().intentReceiver = PushReceiver::class.java

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            liveActivitiesController.registerLiveActivitiesChannel()
        }
    }
}
