package com.actito.go.core

import android.content.Intent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

class DeepLinksService {
    val deepLinkIntent = MutableSharedFlow<Intent?>(1, 0, BufferOverflow.DROP_OLDEST)
}
