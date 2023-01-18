package re.notifica.go

import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import re.notifica.go.live_activities.LiveActivitiesController
import re.notifica.go.live_activities.LiveActivity
import re.notifica.go.models.CoffeeBrewerContentState
import re.notifica.go.storage.preferences.NotificareSharedPreferences
import re.notifica.push.NotificarePushIntentReceiver
import re.notifica.push.models.NotificareLiveActivityUpdate
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class PushReceiver : NotificarePushIntentReceiver() {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject
    lateinit var liveActivitiesController: LiveActivitiesController

    @Inject
    lateinit var preferences: NotificareSharedPreferences

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            INTENT_ACTION_COFFEE_BREWER_DISMISS -> onDismissCoffeeBrewer()
        }
    }

    override fun onTokenChanged(context: Context, token: String) {
        coroutineScope.launch {
            try {
                liveActivitiesController.handleTokenChanged()
            } catch (e: Exception) {
                Timber.e(e, "Failed to update registered live activities.")
            }
        }
    }

    override fun onLiveActivityUpdate(context: Context, update: NotificareLiveActivityUpdate) {
        coroutineScope.launch {
            try {
                when (LiveActivity.from(update.activity)) {
                    LiveActivity.COFFEE_BREWER -> {
                        val contentState = update.content<CoffeeBrewerContentState>()
                            ?: return@launch

                        liveActivitiesController.updateCoffeeActivity(contentState)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to update the live activity.")
            }
        }
    }

    private fun onDismissCoffeeBrewer() {
        coroutineScope.launch {
            try {
                liveActivitiesController.clearCoffeeActivity()
            } catch (e: Exception) {
                Timber.e(e, "Failed to end the live activity.")
            }
        }
    }

    companion object {
        const val INTENT_ACTION_COFFEE_BREWER_DISMISS = "re.notifica.go.intent.action.CoffeeBrewerDismiss"
    }
}
