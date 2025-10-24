package com.actito.go.core

import android.net.Uri
import androidx.core.net.toUri

fun getGravatarUrl(email: String): Uri {
    return "https://gravatar.com/avatar".toUri()
        .buildUpon()
        .appendPath(md5(email.lowercase()))
        .appendQueryParameter("s", "400")
        .appendQueryParameter("d", "retro")
        .build()
}
