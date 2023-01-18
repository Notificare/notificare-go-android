package re.notifica.go

import android.app.Application
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp
import re.notifica.Notificare
import re.notifica.go.live_activities.LiveActivitiesController
import re.notifica.go.storage.preferences.NotificareSharedPreferences
import re.notifica.push.ktx.push
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class NotificareApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var preferences: NotificareSharedPreferences

    @Inject
    lateinit var liveActivitiesController: LiveActivitiesController

    override fun onCreate() {
        super.onCreate()

        // Apply dynamic colouring based on the user's wallpaper.
        DynamicColors.applyToActivitiesIfAvailable(this)

        // Plant a debug tree. 🌱
        Timber.plant(Timber.DebugTree())

        // Configure Notificare if there is a stored configuration set.
        val configuration = preferences.appConfiguration
        if (configuration != null) {
            Notificare.configure(this, configuration.applicationKey, configuration.applicationSecret)
        }

        Notificare.push().intentReceiver = PushReceiver::class.java

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            liveActivitiesController.registerLiveActivitiesChannel()
        }
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }
}
