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
import re.notifica.models.NotificareDoNotDisturb
import re.notifica.models.NotificareTime
import re.notifica.push.ktx.push
import timber.log.Timber

class SettingsViewModel : ViewModel(), DefaultLifecycleObserver {
    private val _userInfo = MutableLiveData<UserInfo>()
    val userInfo: LiveData<UserInfo> = _userInfo

    private val _notificationsEnabled = MutableLiveData(hasNotificationsEnabled)
    val notificationsEnabled: LiveData<Boolean> = _notificationsEnabled

    private val _dndEnabled = MutableLiveData(hasDndEnabled)
    val dndEnabled: LiveData<Boolean> = _dndEnabled

    private val _dnd = MutableLiveData(Notificare.device().currentDevice?.dnd ?: NotificareDoNotDisturb.default)
    val dnd: LiveData<NotificareDoNotDisturb> = _dnd

    private val _locationUpdatesEnabled = MutableLiveData(hasLocationUpdatesEnabled)
    val locationUpdatesEnabled: LiveData<Boolean> = _locationUpdatesEnabled

    private val _announcementsTopicEnabled = MutableLiveData(false)
    val announcementsTopicEnabled: LiveData<Boolean> = _announcementsTopicEnabled

    private val _marketingTopicEnabled = MutableLiveData(false)
    val marketingTopicEnabled: LiveData<Boolean> = _marketingTopicEnabled

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

    private val hasDndEnabled: Boolean
        get() = hasNotificationsEnabled && Notificare.device().currentDevice?.dnd != null

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
                val dnd = Notificare.device().fetchDoNotDisturb()
                _dnd.postValue(dnd ?: NotificareDoNotDisturb.default)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch the do not disturb settings.")
            }
        }

        viewModelScope.launch {
            try {
                val tags = Notificare.device().fetchTags()

                _announcementsTopicEnabled.postValue(tags.contains(Topic.ANNOUNCEMENTS.rawValue))
                _marketingTopicEnabled.postValue(tags.contains(Topic.MARKETING.rawValue))
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
        viewModelScope.launch {
            try {
                if (enabled) {
                    Notificare.push().enableRemoteNotifications()
                } else {
                    Notificare.push().disableRemoteNotifications()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to update remote notifications registration.")
            }
        }
    }

    fun changeDoNotDisturbEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                if (enabled) {
                    Notificare.device().updateDoNotDisturb(NotificareDoNotDisturb.default)
                } else {
                    Notificare.device().clearDoNotDisturb()
                }

                _dndEnabled.postValue(enabled)
                _dnd.postValue(NotificareDoNotDisturb.default)
            } catch (e: Exception) {
                Timber.e(e, "Failed to update the do not disturb settings.")
            }
        }
    }

    fun changeDoNotDisturb(dnd: NotificareDoNotDisturb) {
        viewModelScope.launch {
            try {
                Notificare.device().updateDoNotDisturb(dnd)
                _dnd.postValue(dnd)
            } catch (e: Exception) {
                Timber.e(e, "Failed to update the do not disturb settings.")
            }
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
                Topic.MARKETING -> _marketingTopicEnabled
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
        MARKETING,
        BEST_PRACTICES,
        PRODUCT_UPDATES,
        ENGINEERING,
        STAFF;

        val rawValue: String
            get() = when (this) {
                ANNOUNCEMENTS -> "topic_announcements"
                MARKETING -> "topic_marketing"
                BEST_PRACTICES -> "topic_best_practices"
                PRODUCT_UPDATES -> "topic_product_updates"
                ENGINEERING -> "topic_engineering"
                STAFF -> "topic_staff"
            }
    }

    private val NotificareDoNotDisturb.Companion.default: NotificareDoNotDisturb
        get() = NotificareDoNotDisturb(
            NotificareDoNotDisturb.defaultStart,
            NotificareDoNotDisturb.defaultEnd,
        )

    private val NotificareDoNotDisturb.Companion.defaultStart: NotificareTime
        get() = NotificareTime(hours = 23, minutes = 0)

    private val NotificareDoNotDisturb.Companion.defaultEnd: NotificareTime
        get() = NotificareTime(hours = 8, minutes = 0)
}
