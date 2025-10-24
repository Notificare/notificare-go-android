package com.actito.go.models

import android.net.Uri
import com.google.firebase.auth.FirebaseUser
import com.actito.go.core.getGravatarUrl

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
