package com.actito.go.ktx

import android.widget.RemoteViews

internal fun RemoteViews.setColorFilter(viewId: Int, color: Int) {
    setInt(viewId, "setColorFilter", color)
}

internal fun RemoteViews.setBackgroundResource(viewId: Int, resId: Int) {
    setInt(viewId, "setBackgroundResource", resId)
}
