package re.notifica.go.models

import android.net.Uri
import com.google.firebase.auth.FirebaseUser
import re.notifica.go.core.getGravatarUrl

data class UserInfo(
    val id: String,
    val name: String?,
    val pictureUrl: Uri?,
) {
    companion object {
        operator fun invoke(user: FirebaseUser): UserInfo {
            return UserInfo(
                id = user.uid,
                name = user.displayName,
                pictureUrl = user.email?.let { getGravatarUrl(it) },
            )
        }
    }
}
