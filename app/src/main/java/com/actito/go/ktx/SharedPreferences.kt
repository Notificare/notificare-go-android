package com.actito.go.ktx

import android.content.SharedPreferences
import com.squareup.moshi.Moshi

internal inline fun <reified T> SharedPreferences.getMoshi(moshi: Moshi, key: String): T? {
    val str = getString(key, null) ?: return null
    val adapter = moshi.adapter(T::class.java)

    return adapter.fromJson(str)
}

internal inline fun <reified T> SharedPreferences.Editor.putMoshi(
    moshi: Moshi,
    key: String,
    value: T?
): SharedPreferences.Editor {
    if (value == null) return remove(key)

    val adapter = moshi.adapter(T::class.java)
    val str = adapter.toJson(value)

    return putString(key, str)
}
