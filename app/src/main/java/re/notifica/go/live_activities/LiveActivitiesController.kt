package re.notifica.go.live_activities

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.withContext
import re.notifica.Notificare
import re.notifica.go.R
import re.notifica.go.live_activities.ui.CoffeeLiveNotification
import re.notifica.go.models.CoffeeBrewerContentState
import re.notifica.go.storage.datastore.NotificareDataStore
import re.notifica.ktx.events
import re.notifica.push.ktx.push
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveActivitiesController @Inject constructor(
    private val application: Application,
    private val dataStore: NotificareDataStore,
) {

    private val notificationManager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val coffeeActivityStream: Flow<CoffeeBrewerContentState?> = dataStore.coffeeBrewerContentStateStream

    @RequiresApi(Build.VERSION_CODES.O)
    fun registerLiveActivitiesChannel() {
        val channel = NotificationChannel(
            CHANNEL_LIVE_ACTIVITIES,
            application.getString(R.string.notification_channel_live_activities_title),
            NotificationManager.IMPORTANCE_DEFAULT
        )

        channel.description = application.getString(R.string.notification_channel_live_activities_description)

        notificationManager.createNotificationChannel(channel)
    }

    suspend fun handleTokenChanged(): Unit = withContext(Dispatchers.IO) {
        val coffeeBrewerContentState = coffeeActivityStream.lastOrNull()
        if (coffeeBrewerContentState != null) {
            Notificare.push().registerLiveActivity(LiveActivity.COFFEE_BREWER.identifier)
        }
    }

    suspend fun createCoffeeActivity(
        contentState: CoffeeBrewerContentState
    ): Unit = withContext(Dispatchers.IO) {
        // Present the notification UI.
        updateCoffeeActivity(contentState)

        // Track a custom event for analytics purposes.
        Notificare.events().logCustom(
            event = "live_activity_started",
            data = mapOf(
                "activity" to LiveActivity.COFFEE_BREWER.identifier,
                "activityId" to UUID.randomUUID().toString(),
            )
        )

        // Register on Notificare to receive updates.
        Notificare.push().registerLiveActivity(LiveActivity.COFFEE_BREWER.identifier)
    }

    suspend fun updateCoffeeActivity(
        contentState: CoffeeBrewerContentState
    ): Unit = withContext(Dispatchers.IO) {
        // Present the notification UI.
        val ongoingNotification = notificationManager.activeNotifications
            .firstOrNull { it.tag == LiveActivity.COFFEE_BREWER.identifier }

        notificationManager.notify(
            LiveActivity.COFFEE_BREWER.identifier,
            ongoingNotification?.id ?: notificationCounter.incrementAndGet(),
            CoffeeLiveNotification(application, contentState).build()
        )

        // Persist the state to storage.
        dataStore.updateCoffeeBrewerContentState(contentState)
    }

    suspend fun clearCoffeeActivity(): Unit = withContext(Dispatchers.IO) {
        // Dismiss the notification.
        notificationManager.activeNotifications
            .filter { it.tag == LiveActivity.COFFEE_BREWER.identifier }
            .forEach { notificationManager.cancel(LiveActivity.COFFEE_BREWER.identifier, it.id) }

        // Persist the state to storage.
        dataStore.updateCoffeeBrewerContentState(null)

        // End on Notificare to stop receiving updates.
        Notificare.push().endLiveActivity(LiveActivity.COFFEE_BREWER.identifier)
    }

    companion object {
        private val notificationCounter = AtomicInteger(0)

        const val CHANNEL_LIVE_ACTIVITIES = "live-activities"
    }
}
