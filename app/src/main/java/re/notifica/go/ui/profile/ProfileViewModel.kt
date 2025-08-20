package re.notifica.go.ui.profile

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialResponse
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import re.notifica.Notificare
import re.notifica.go.ktx.PageView
import re.notifica.go.ktx.logPageViewed
import re.notifica.go.models.UserInfo
import re.notifica.go.storage.preferences.NotificareSharedPreferences
import re.notifica.ktx.device
import re.notifica.ktx.events
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext context: Context,
    preferences: NotificareSharedPreferences,
) : ViewModel() {

    val credentialManager = CredentialManager.create(context)

    private val _membershipCard = MutableLiveData<String?>(preferences.membershipCardUrl)
    val membershipCard: LiveData<String?> = _membershipCard

    private val _userInfo = MutableLiveData<UserInfo>()
    val userInfo: LiveData<UserInfo> = _userInfo

    private val _userDataFields = MutableLiveData<List<UserDataField>>()
    val userDataFields: LiveData<List<UserDataField>> = _userDataFields

    val userDataFieldChanges = MutableSharedFlow<List<UserDataField>>(1, 0, BufferOverflow.DROP_OLDEST)

    init {
        val user = Firebase.auth.currentUser
        if (user != null) _userInfo.postValue(UserInfo(user))

        viewModelScope.launch {
            try {
                Notificare.events().logPageViewed(PageView.USER_PROFILE)
            } catch (e: Exception) {
                Timber.e(e, "Failed to log a custom event.")
            }

            try {
                val fields = Notificare.fetchApplication().userDataFields
                val userData = Notificare.device().fetchUserData()

                _userDataFields.postValue(
                    fields.map { field ->
                        UserDataField(
                            key = field.key,
                            label = field.label,
                            type = field.type,
                            value = userData[field.key] ?: ""
                        )
                    }
                )
            } catch (e: Exception) {
                // TODO: handle error
            }
        }

        viewModelScope.launch {
            @Suppress("OPT_IN_USAGE")
            userDataFieldChanges
                .debounce(1500)
                .collect { userDataFields ->
                    Timber.d("Updating user data.")

                    try {
                        val userData = userDataFields.associate { it.key to it.value }
                        Timber.d("user data = $userDataFields")
                        Notificare.device().updateUserData(userData)

                        _userDataFields.postValue(userDataFields)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to update the user data.")
                    }
                }
        }
    }


    suspend fun deleteAccount() = withContext(Dispatchers.IO) {
        // Remove the Firebase user.
        val user = checkNotNull(Firebase.auth.currentUser)
        user.delete().await()

        // Register the device as anonymous.
        Notificare.device().updateUser(userId = null, userName = null)
    }

    suspend fun handleAuthenticationResult(result: GetCredentialResponse) {
        val credential = result.credential

        if (
            credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            try {
                val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(token, null)

                val user = checkNotNull(Firebase.auth.currentUser)
                user.reauthenticate(firebaseCredential).await()
            } catch (e: GoogleIdTokenParsingException) {
                throw IllegalArgumentException("Received an invalid Google ID token response", e)
            }
        } else {
            throw IllegalArgumentException("Unexpected type of credential")
        }
    }

    data class UserDataField(
        val key: String,
        val label: String,
        val type: String,
        var value: String,
    )
}
