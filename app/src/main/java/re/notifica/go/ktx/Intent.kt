package re.notifica.go.ktx

import android.content.Intent
import android.os.Build
import android.os.Parcelable

internal inline fun <reified T : Parcelable> Intent.parcelable(name: String): T? {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelableExtra(name, T::class.java)
        else -> @Suppress("DEPRECATION") getParcelableExtra(name)
    }
}
