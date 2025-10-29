package com.actito.go.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.actito.go.live_activities.LiveActivitiesController
import com.actito.go.live_activities.LiveActivity

@HiltWorker
class CoffeeBrewerDismissalWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val liveActivitiesController: LiveActivitiesController,
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        return try {
            liveActivitiesController.notificationManager.activeNotifications
                .filter { it.tag == LiveActivity.COFFEE_BREWER.identifier }
                .forEach {
                    liveActivitiesController.notificationManager.cancel(
                        LiveActivity.COFFEE_BREWER.identifier,
                        it.id
                    )
                }
            Result.success()

        } catch (_: Exception) {
            Result.failure()
        }
    }
}

@HiltWorker
class OrderStatusDismissalWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val liveActivitiesController: LiveActivitiesController,
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        return try {
            liveActivitiesController.notificationManager.activeNotifications
                .filter { it.tag == LiveActivity.ORDER_STATUS.identifier }
                .forEach {
                    liveActivitiesController.notificationManager.cancel(
                        LiveActivity.ORDER_STATUS.identifier,
                        it.id
                    )
                }
            Result.success()

        } catch (_: Exception) {
            Result.failure()
        }
    }
}
