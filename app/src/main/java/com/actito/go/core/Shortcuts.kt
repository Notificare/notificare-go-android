package com.actito.go.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import com.actito.go.BuildConfig
import com.actito.go.R
import com.actito.go.storage.preferences.ActitoSharedPreferences
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import timber.log.Timber

enum class ShortcutAction {
    CART,
    SETTINGS,
    EVENTS;

    val rawValue: String
        get() = when (this) {
            CART -> "cart"
            SETTINGS -> "settings"
            EVENTS -> "events"
        }

    val deepLink: Uri
        get() = when (this) {
            CART -> "${BuildConfig.APPLICATION_ID}://notifica.re/cart".toUri()
            SETTINGS -> "${BuildConfig.APPLICATION_ID}://notifica.re/settings".toUri()
            EVENTS -> "${BuildConfig.APPLICATION_ID}://notifica.re/events".toUri()
        }
}

fun createDynamicShortcuts(context: Context, preferences: ActitoSharedPreferences) {
    try {
        Timber.d("Creating the shortcuts.")
        val shortcuts = mutableListOf<ShortcutInfoCompat>()

        if (preferences.hasStoreEnabled) {
            shortcuts.add(
                ShortcutInfoCompat.Builder(context, ShortcutAction.CART.rawValue)
                    .setShortLabel(context.getString(R.string.shortcut_cart))
                    .setIcon(IconCompat.createWithResource(context, R.drawable.ic_baseline_shopping_cart_24))
                    .setIntent(Intent(Intent.ACTION_VIEW, ShortcutAction.CART.deepLink))
                    .build()
            )
        }

        shortcuts.add(
            ShortcutInfoCompat.Builder(context, ShortcutAction.SETTINGS.rawValue)
                .setShortLabel(context.getString(R.string.shortcut_settings))
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_baseline_settings_24))
                .setIntent(Intent(Intent.ACTION_VIEW, ShortcutAction.SETTINGS.deepLink))
                .build()
        )

        shortcuts.add(
            ShortcutInfoCompat.Builder(context, ShortcutAction.EVENTS.rawValue)
                .setShortLabel(context.getString(R.string.shortcut_events))
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_baseline_event_note_24))
                .setIntent(Intent(Intent.ACTION_VIEW, ShortcutAction.EVENTS.deepLink))
                .build()
        )

        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    } catch (e: Exception) {
        Timber.e(e, "Failed to create the shortcuts.")
        Firebase.crashlytics.recordException(e)
    }
}
