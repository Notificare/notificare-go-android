package com.actito.go.ktx

import android.content.Intent
import android.os.Parcelable
import androidx.core.content.IntentCompat

internal inline fun <reified T : Parcelable> Intent.parcelableExtra(name: String): T? {
    return IntentCompat.getParcelableExtra(this, name, T::class.java)
}
