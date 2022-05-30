package re.notifica.go.ui.intro

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResult
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import re.notifica.Notificare
import re.notifica.geo.ktx.geo
import re.notifica.go.core.loadRemoteConfig
import re.notifica.go.storage.preferences.NotificareSharedPreferences
import re.notifica.ktx.device
import re.notifica.push.ktx.push
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class IntroViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val preferences: NotificareSharedPreferences,
) : ViewModel() {

    private val _currentPage = MutableLiveData(IntroPage.WELCOME)
    val currentPage: LiveData<IntroPage> = _currentPage

    val loginClient = Identity.getSignInClient(context)

    fun moveTo(to: IntroPage) {
        _currentPage.postValue(to)
    }

    fun enableRemoteNotifications() {
        Notificare.push().enableRemoteNotifications()
        moveTo(IntroPage.LOCATION)
    }

    fun enableLocationUpdates() {
        Notificare.geo().enableLocationUpdates()
        moveTo(IntroPage.LOGIN)
    }

    suspend fun handleLoginResult(result: ActivityResult) {
        if (result.resultCode != Activity.RESULT_OK) throw IllegalStateException("Login request result NOT OK.")

        val credential = loginClient.getSignInCredentialFromIntent(result.data)
        val token = credential.googleIdToken
            ?: throw IllegalArgumentException("Invalid googleIdToken extracted from the intent.")

        val firebaseCredential = GoogleAuthProvider.getCredential(token, null)
        val authenticationResult = Firebase.auth.signInWithCredential(firebaseCredential).await()

        loadRemoteConfig(preferences)

        val user = authenticationResult.user
        if (user == null) {
            Timber.w("Authentication result yielded no user.")
        } else {
            try {
                Notificare.device().register(user.uid, user.displayName)
            } catch (e: Exception) {
                // TODO: handle error scenario.
            }
        }

        preferences.hasIntroFinished = true
    }
}
