package com.actito.go.live_activities.ui

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.actito.go.PushReceiver
import com.actito.go.R
import com.actito.go.ktx.setBackgroundResource
import com.actito.go.ktx.setColorFilter
import com.actito.go.live_activities.LiveActivitiesController
import com.actito.go.live_activities.models.CoffeeBrewerContentState
import com.actito.go.live_activities.models.CoffeeBrewingState

class CoffeeLiveNotification(
    private val context: Context,
    private val contentState: CoffeeBrewerContentState,
) {

    private val title: String
        get() {
            return when (contentState.state) {
                CoffeeBrewingState.SERVED -> context.getString(R.string.coffee_headline_served_title)
                else -> context.resources.getQuantityString(
                    R.plurals.coffee_headline_pick_up_title,
                    contentState.remaining,
                    contentState.remaining
                )
            }
        }

    private val subtitle: String
        get() {
            return context.getString(
                when (contentState.state) {
                    CoffeeBrewingState.GRINDING -> R.string.coffee_headline_grinding_subtitle
                    CoffeeBrewingState.BREWING -> R.string.coffee_headline_brewing_subtitle
                    CoffeeBrewingState.SERVED -> R.string.coffee_headline_served_subtitle
                }
            )
        }


    fun build(): Notification {
        val deletePendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, PushReceiver::class.java).apply {
                action = PushReceiver.INTENT_ACTION_COFFEE_BREWER_DISMISS
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, LiveActivitiesController.CHANNEL_LIVE_ACTIVITIES)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(createStandardLayout())
            .setCustomBigContentView(createExpandedLayout())
            .setDeleteIntent(deletePendingIntent)
            .build()
    }


    private fun createStandardLayout(): RemoteViews {
        val view = RemoteViews(context.packageName, R.layout.notification_coffee_brewer)

        view.setTextViewText(R.id.notification_title, title)
        view.setTextViewText(R.id.notification_subtitle, subtitle)

        return view
    }

    private fun createExpandedLayout(): RemoteViews {
        val view = RemoteViews(context.packageName, R.layout.notification_coffee_brewer_expanded)

        view.setTextViewText(R.id.notification_title, title)
        view.setTextViewText(R.id.notification_subtitle, subtitle)

        // region Grinding circle

        view.setBackgroundResource(
            R.id.notification_grinding_image,
            getBackgroundCircleResource(CoffeeBrewingState.GRINDING)
        )

        view.setColorFilter(
            R.id.notification_grinding_image,
            getForegroundColor(CoffeeBrewingState.GRINDING)
        )

        // endregion

        view.setColorFilter(
            R.id.notification_brewing_progress_image,
            getBackgroundColor(CoffeeBrewingState.BREWING)
        )

        // region Brewing circle

        view.setBackgroundResource(
            R.id.notification_brewing_image,
            getBackgroundCircleResource(CoffeeBrewingState.BREWING)
        )

        view.setColorFilter(
            R.id.notification_brewing_image,
            getForegroundColor(CoffeeBrewingState.BREWING)
        )

        // endregion

        view.setColorFilter(
            R.id.notification_serving_progress_image,
            getBackgroundColor(CoffeeBrewingState.SERVED)
        )

        // region Served circle

        view.setBackgroundResource(
            R.id.notification_serving_image,
            getBackgroundCircleResource(CoffeeBrewingState.SERVED)
        )

        view.setColorFilter(
            R.id.notification_serving_image,
            getForegroundColor(CoffeeBrewingState.SERVED)
        )

        // endregion

        return view
    }

    private fun shouldHighlightView(
        currentState: CoffeeBrewingState,
        representedState: CoffeeBrewingState,
    ): Boolean = representedState.ordinal <= currentState.ordinal

    private fun getForegroundColor(representedState: CoffeeBrewingState): Int {
        val isHighlighted = shouldHighlightView(contentState.state, representedState)

        return context.getColor(
            if (isHighlighted) R.color.white
            else R.color.black
        )
    }

    private fun getBackgroundColor(representedState: CoffeeBrewingState): Int {
        val isHighlighted = shouldHighlightView(contentState.state, representedState)

        return context.getColor(
            if (isHighlighted) R.color.coffee
            else R.color.disabled_grey
        )
    }

    private fun getBackgroundCircleResource(representedState: CoffeeBrewingState): Int {
        return if (shouldHighlightView(contentState.state, representedState)) {
            R.drawable.shape_coffee_brewer_circle_colored
        } else {
            R.drawable.shape_circle_disabled
        }
    }
}
