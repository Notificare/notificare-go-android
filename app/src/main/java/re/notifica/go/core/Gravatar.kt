package re.notifica.go.core

import android.net.Uri

fun getGravatarUrl(email: String): Uri {
    return Uri.parse("https://gravatar.com/avatar")
        .buildUpon()
        .appendPath(md5(email.lowercase()))
        .appendQueryParameter("s", "400")
        .appendQueryParameter("d", "retro")
        .build()
}
