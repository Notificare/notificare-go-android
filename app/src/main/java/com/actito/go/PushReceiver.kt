package com.actito.go

import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.actito.go.live_activities.LiveActivitiesController
import com.actito.go.live_activities.LiveActivity
import com.actito.go.live_activities.models.CoffeeBrewerContentState
import com.actito.go.live_activities.models.OrderContentState
import com.actito.go.storage.preferences.ActitoSharedPreferences
import com.actito.go.workers.CoffeeBrewerDismissalWorker
import com.actito.go.workers.OrderStatusDismissalWorker
import com.actito.push.ActitoPushIntentReceiver
import com.actito.push.models.ActitoLiveActivityUpdate
import com.actito.push.models.ActitoPushSubscription
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class PushReceiver : ActitoPushIntentReceiver() {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject
    lateinit var liveActivitiesController: LiveActivitiesController

    @Inject
    lateinit var preferences: ActitoSharedPreferences

    @Inject
    lateinit var workManager: WorkManager

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            INTENT_ACTION_COFFEE_BREWER_DISMISS -> dismissLiveActivity(LiveActivity.COFFEE_BREWER)
            INTENT_ACTION_ORDER_STATUS_DISMISS -> dismissLiveActivity(LiveActivity.ORDER_STATUS)
        }
    }

    override fun onSubscriptionChanged(
        context: Context,
        subscription: ActitoPushSubscription?
    ) {
        coroutineScope.launch {
            try {
                liveActivitiesController.handleTokenChanged()
            } catch (e: Exception) {
                Timber.e(e, "Failed to update registered live activities.")
            }
        }
    }

    override fun onLiveActivityUpdate(context: Context, update: ActitoLiveActivityUpdate) {
        coroutineScope.launch {
            try {
                when (LiveActivity.from(update.activity)) {
                    LiveActivity.COFFEE_BREWER -> {
                        val contentState = update.content<CoffeeBrewerContentState>()
                            ?: return@launch

                        liveActivitiesController.updateCoffeeActivity(contentState)

                        if (update.final) {
                            var delay = DEFAULT_DISMISSAL_MILLISECONDS
                            val dismissalDate = update.dismissalDate

                            if (dismissalDate != null) {
                                delay = if (dismissalDate.time <= System.currentTimeMillis()) {
                                    0
                                } else {
                                    dismissalDate.time - System.currentTimeMillis()
                                }
                            }

                            val request = OneTimeWorkRequestBuilder<CoffeeBrewerDismissalWorker>()
                                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                                .build()

                            workManager.enqueue(request)

                            liveActivitiesController.updateCoffeeBrewerState(null)
                        }
                    }

                    LiveActivity.ORDER_STATUS -> {
                        val contentState = update.content<OrderContentState>()
                            ?: return@launch

                        liveActivitiesController.updateOrderActivity(contentState)

                        if (update.final) {
                            var delay = DEFAULT_DISMISSAL_MILLISECONDS
                            val dismissalDate = update.dismissalDate

                            if (dismissalDate != null) {
                                delay = if (dismissalDate.time <= System.currentTimeMillis()) {
                                    0
                                } else {
                                    dismissalDate.time - System.currentTimeMillis()
                                }
                            }

                            val request = OneTimeWorkRequestBuilder<OrderStatusDismissalWorker>()
                                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                                .build()

                            workManager.enqueue(request)

                            liveActivitiesController.updateOrderState(null)
                        }
                    }

                    null -> {}
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to update the live activity.")
            }
        }
    }

    private fun dismissLiveActivity(activity: LiveActivity) {
        coroutineScope.launch {
            try {
                when (activity) {
                    LiveActivity.COFFEE_BREWER -> liveActivitiesController.clearCoffeeActivity()
                    LiveActivity.ORDER_STATUS -> liveActivitiesController.clearOrderActivity()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to end the live activity.")
            }
        }
    }

    companion object {
        const val INTENT_ACTION_COFFEE_BREWER_DISMISS =
            "re.notifica.go.intent.action.CoffeeBrewerDismiss"
        const val INTENT_ACTION_ORDER_STATUS_DISMISS =
            "re.notifica.go.intent.action.OrderStatusDismiss"

        private const val DEFAULT_DISMISSAL_MILLISECONDS: Long = 4 * 60 * 60 * 1000
    }
}
