package com.actito.go.ui.intro

import android.annotation.SuppressLint
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialResponse
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.actito.Actito
import com.actito.geo.ktx.geo
import com.actito.go.R
import com.actito.go.core.createDynamicShortcuts
import com.actito.go.core.loadRemoteConfig
import com.actito.go.ktx.logIntroFinished
import com.actito.go.network.push.PushService
import com.actito.go.network.push.payloads.EnrollmentPayload
import com.actito.go.storage.preferences.ActitoSharedPreferences
import com.actito.iam.ktx.inAppMessaging
import com.actito.ktx.device
import com.actito.ktx.events
import com.actito.push.ktx.push
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class IntroViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferences: ActitoSharedPreferences,
    private val pushService: PushService,
) : ViewModel() {

    private val _currentPage = MutableLiveData(IntroPage.WELCOME)
    val currentPage: LiveData<IntroPage> = _currentPage

    val credentialManager = CredentialManager.create(context)

    fun moveTo(to: IntroPage) {
        _currentPage.postValue(to)
    }

    fun enableRemoteNotifications() {
        viewModelScope.launch {
            try {
                Actito.push().enableRemoteNotifications()
                moveTo(IntroPage.LOCATION)
            } catch (e: Exception) {
                Timber.e(e, "Failed to enable remote notifications.")
            }
        }
    }

    fun enableLocationUpdates() {
        Actito.geo().enableLocationUpdates()
        moveTo(IntroPage.LOGIN)
    }

    suspend fun handleSignInResult(result: GetCredentialResponse) {
        val credential = result.credential

        if (
            credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            try {
                val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(token, null)
                val authenticationResult = Firebase.auth.signInWithCredential(firebaseCredential).await()

                loadRemoteConfig(preferences)
                createDynamicShortcuts(context, preferences)

                val user = authenticationResult.user
                if (user == null) {
                    Timber.w("Authentication result yielded no user.")
                } else {
                    Actito.device().updateUser(user.uid, user.displayName)

                    val programId = preferences.appConfiguration?.loyaltyProgramId
                    if (programId != null) {
                        Timber.d("Creating loyalty program enrollment.")

                        val response = pushService.createEnrollment(
                            programId = programId,
                            payload = EnrollmentPayload(
                                userId = user.uid,
                                memberId = user.uid,
                                fields = listOf(
                                    EnrollmentPayload.Field(
                                        key = "name",
                                        value = user.displayName
                                            ?: context.getString(R.string.settings_anonymous_user_name),
                                    ),
                                    EnrollmentPayload.Field(
                                        key = "email",
                                        value = user.email ?: "",
                                    ),
                                ),
                            ),
                        )

                        preferences.membershipCardUrl = response.saveLinks.googlePay
                    }
                }
            } catch (e: GoogleIdTokenParsingException) {
                throw IllegalArgumentException("Received an invalid google id token response", e)
            }
        } else {
            throw IllegalArgumentException("Unexpected type of credential")
        }

        try {
            Actito.events().logIntroFinished()
        } catch (e: Exception) {
            Timber.e(e, "Failed to log a custom event.")
        }

        preferences.hasIntroFinished = true
        Actito.inAppMessaging().hasMessagesSuppressed = false
    }

}
