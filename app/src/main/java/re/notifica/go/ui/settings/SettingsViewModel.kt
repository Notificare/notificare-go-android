package re.notifica.go.ui.settings

import androidx.lifecycle.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.geo.ktx.geo
import re.notifica.go.ktx.PageView
import re.notifica.go.ktx.hasLocationTrackingCapabilities
import re.notifica.go.ktx.logPageViewed
import re.notifica.go.models.UserInfo
import re.notifica.ktx.device
import re.notifica.ktx.events
import re.notifica.push.ktx.push
import timber.log.Timber

class SettingsViewModel : ViewModel(), DefaultLifecycleObserver {
    private val _userInfo = MutableLiveData<UserInfo>()
    val userInfo: LiveData<UserInfo> = _userInfo

    private val _notificationsEnabled = MutableLiveData(hasNotificationsEnabled)
    val notificationsEnabled: LiveData<Boolean> = _notificationsEnabled

    private val _locationUpdatesEnabled = MutableLiveData(hasLocationUpdatesEnabled)
    val locationUpdatesEnabled: LiveData<Boolean> = _locationUpdatesEnabled

    private val _announcementsTopicEnabled = MutableLiveData(false)
    val announcementsTopicEnabled: LiveData<Boolean> = _announcementsTopicEnabled

    private val _bestPracticesTopicEnabled = MutableLiveData(false)
    val bestPracticesTopicEnabled: LiveData<Boolean> = _bestPracticesTopicEnabled

    private val _productUpdatesTopicEnabled = MutableLiveData(false)
    val productUpdatesTopicEnabled: LiveData<Boolean> = _productUpdatesTopicEnabled

    private val _engineeringTopicEnabled = MutableLiveData(false)
    val engineeringTopicEnabled: LiveData<Boolean> = _engineeringTopicEnabled

    private val _staffTopicEnabled = MutableLiveData(false)
    val staffTopicEnabled: LiveData<Boolean> = _staffTopicEnabled

    private val hasNotificationsEnabled: Boolean
        get() = Notificare.push().hasRemoteNotificationsEnabled && Notificare.push().allowedUI

    private val hasLocationUpdatesEnabled: Boolean
        get() = Notificare.geo().hasLocationTrackingCapabilities

    init {
        val user = Firebase.auth.currentUser
        if (user != null) {
            _userInfo.postValue(UserInfo(user))
        }

        viewModelScope.launch {
            Notificare.push().observableAllowedUI
                .asFlow()
                .distinctUntilChanged()
                .collect { enabled ->
                    _notificationsEnabled.postValue(enabled)
                }
        }

        viewModelScope.launch {
            try {
                val tags = Notificare.device().fetchTags()

                _announcementsTopicEnabled.postValue(tags.contains(Topic.ANNOUNCEMENTS.rawValue))
                _bestPracticesTopicEnabled.postValue(tags.contains(Topic.BEST_PRACTICES.rawValue))
                _productUpdatesTopicEnabled.postValue(tags.contains(Topic.PRODUCT_UPDATES.rawValue))
                _engineeringTopicEnabled.postValue(tags.contains(Topic.ENGINEERING.rawValue))
                _staffTopicEnabled.postValue(tags.contains(Topic.STAFF.rawValue))
            } catch (e: Exception) {
                Timber.e("Failed to fetch the device tags.")
            }
        }
    }

    fun changeRemoteNotifications(enabled: Boolean) {
        if (enabled) {
            Notificare.push().enableRemoteNotifications()
        } else {
            Notificare.push().disableRemoteNotifications()
        }
    }

    fun changeLocationUpdates(enabled: Boolean) {
        if (enabled) {
            Notificare.geo().enableLocationUpdates()
        } else {
            Notificare.geo().disableLocationUpdates()
        }

        _locationUpdatesEnabled.postValue(hasLocationUpdatesEnabled)
    }

    fun changeTopicSubscription(topic: Topic, subscribed: Boolean) {
        viewModelScope.launch {
            val data: MutableLiveData<Boolean> = when (topic) {
                Topic.ANNOUNCEMENTS -> _announcementsTopicEnabled
                Topic.BEST_PRACTICES -> _bestPracticesTopicEnabled
                Topic.PRODUCT_UPDATES -> _productUpdatesTopicEnabled
                Topic.ENGINEERING -> _engineeringTopicEnabled
                Topic.STAFF -> _staffTopicEnabled
            }

            try {
                if (subscribed) {
                    Notificare.device().addTag(topic.rawValue)
                } else {
                    Notificare.device().removeTag(topic.rawValue)
                }

                data.postValue(subscribed)
            } catch (e: Exception) {
                Timber.e(e, "Failed to subscribe to a topic.")
            }
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        viewModelScope.launch {
            try {
                Notificare.events().logPageViewed(PageView.SETTINGS)
            } catch (e: Exception) {
                Timber.e(e, "Failed to log a custom event.")
            }
        }
    }


    enum class Topic {
        ANNOUNCEMENTS,
        BEST_PRACTICES,
        PRODUCT_UPDATES,
        ENGINEERING,
        STAFF;

        val rawValue: String
            get() = when (this) {
                ANNOUNCEMENTS -> "topic_announcements"
                BEST_PRACTICES -> "topic_best_practices"
                PRODUCT_UPDATES -> "topic_product_updates"
                ENGINEERING -> "topic_engineering"
                STAFF -> "topic_staff"
            }
    }
}
