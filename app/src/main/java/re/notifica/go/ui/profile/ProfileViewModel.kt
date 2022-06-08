package re.notifica.go.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import re.notifica.Notificare
import re.notifica.go.models.UserInfo
import re.notifica.go.storage.preferences.NotificareSharedPreferences
import re.notifica.ktx.device
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    preferences: NotificareSharedPreferences,
) : ViewModel() {

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
        // Register the device as anonymous.
        Notificare.device().register(userId = null, userName = null)

        // Remove the Firebase user.
        val user = checkNotNull(Firebase.auth.currentUser)
        user.delete().await()
    }

    data class UserDataField(
        val key: String,
        val label: String,
        val type: String,
        var value: String,
    )
}
