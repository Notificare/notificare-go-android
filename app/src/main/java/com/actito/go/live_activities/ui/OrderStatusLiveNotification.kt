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
import com.actito.go.live_activities.models.OrderContentState
import com.actito.go.live_activities.models.OrderStatus

class OrderStatusLiveNotification(
    private val context: Context,
    private val contentState: OrderContentState,
) {

    private val title: String
        get() {
            return context.getString(
                when (contentState.status) {
                    OrderStatus.PREPARING -> R.string.order_headline_preparing_title
                    OrderStatus.SHIPPED -> R.string.order_headline_shipped_title
                    OrderStatus.DELIVERED -> R.string.order_headline_delivered_title
                }
            )
        }

    private val subtitle: String
        get() {
            return context.getString(
                when (contentState.status) {
                    OrderStatus.PREPARING -> R.string.order_headline_preparing_subtitle
                    OrderStatus.SHIPPED -> R.string.order_headline_shipped_subtitle
                    OrderStatus.DELIVERED -> R.string.order_headline_delivered_subtitle
                }
            )
        }


    fun build(): Notification {
        val deletePendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, PushReceiver::class.java).apply {
                action = PushReceiver.INTENT_ACTION_ORDER_STATUS_DISMISS
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
        val view = RemoteViews(context.packageName, R.layout.notification_order_status)

        view.setTextViewText(R.id.notification_title, title)
        view.setTextViewText(R.id.notification_subtitle, subtitle)

        return view
    }

    private fun createExpandedLayout(): RemoteViews {
        val view = RemoteViews(context.packageName, R.layout.notification_order_status_expanded)

        view.setTextViewText(R.id.notification_title, title)
        view.setTextViewText(R.id.notification_subtitle, subtitle)

        // region Preparing circle

        view.setBackgroundResource(
            R.id.notification_preparing_image,
            getBackgroundCircleResource(OrderStatus.PREPARING)
        )

        view.setColorFilter(
            R.id.notification_preparing_image,
            getForegroundColor(OrderStatus.PREPARING)
        )

        // endregion

        view.setColorFilter(
            R.id.notification_shipped_progress_image,
            getBackgroundColor(OrderStatus.SHIPPED)
        )

        // region Shipped circle

        view.setBackgroundResource(
            R.id.notification_shipped_image,
            getBackgroundCircleResource(OrderStatus.SHIPPED)
        )

        view.setColorFilter(
            R.id.notification_shipped_image,
            getForegroundColor(OrderStatus.SHIPPED)
        )

        // endregion

        view.setColorFilter(
            R.id.notification_delivered_progress_image,
            getBackgroundColor(OrderStatus.DELIVERED)
        )

        // region Delivered circle

        view.setBackgroundResource(
            R.id.notification_delivered_image,
            getBackgroundCircleResource(OrderStatus.DELIVERED)
        )

        view.setColorFilter(
            R.id.notification_delivered_image,
            getForegroundColor(OrderStatus.DELIVERED)
        )

        // endregion

        return view
    }

    private fun shouldHighlightView(
        currentState: OrderStatus,
        representedState: OrderStatus,
    ): Boolean = representedState.ordinal <= currentState.ordinal

    private fun getForegroundColor(representedState: OrderStatus): Int {
        val isHighlighted = shouldHighlightView(contentState.status, representedState)

        return context.getColor(
            if (isHighlighted) R.color.white
            else R.color.black
        )
    }

    private fun getBackgroundColor(representedState: OrderStatus): Int {
        val isHighlighted = shouldHighlightView(contentState.status, representedState)

        return context.getColor(
            if (isHighlighted) R.color.primary
            else R.color.disabled_grey
        )
    }

    private fun getBackgroundCircleResource(representedState: OrderStatus): Int {
        return if (shouldHighlightView(contentState.status, representedState)) {
            R.drawable.shape_order_status_circle_colored
        } else {
            R.drawable.shape_circle_disabled
        }
    }
}
