package com.actito.go.ui.settings

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.actito.Actito
import com.actito.geo.ktx.geo
import com.actito.go.ktx.PageView
import com.actito.go.ktx.hasLocationTrackingCapabilities
import com.actito.go.ktx.logPageViewed
import com.actito.go.models.UserInfo
import com.actito.ktx.device
import com.actito.ktx.events
import com.actito.models.ActitoDoNotDisturb
import com.actito.models.ActitoTime
import com.actito.push.ktx.push
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber

class SettingsViewModel : ViewModel(), DefaultLifecycleObserver {
    private val _userInfo = MutableLiveData<UserInfo>()
    val userInfo: LiveData<UserInfo> = _userInfo

    private val _notificationsEnabled = MutableLiveData(hasNotificationsEnabled)
    val notificationsEnabled: LiveData<Boolean> = _notificationsEnabled

    private val _dndEnabled = MutableLiveData(hasDndEnabled)
    val dndEnabled: LiveData<Boolean> = _dndEnabled

    private val _dnd = MutableLiveData(Actito.device().currentDevice?.dnd ?: ActitoDoNotDisturb.default)
    val dnd: LiveData<ActitoDoNotDisturb> = _dnd

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
        get() = Actito.push().hasRemoteNotificationsEnabled && Actito.push().allowedUI

    private val hasDndEnabled: Boolean
        get() = hasNotificationsEnabled && Actito.device().currentDevice?.dnd != null

    private val hasLocationUpdatesEnabled: Boolean
        get() = Actito.geo().hasLocationTrackingCapabilities

    init {
        val user = Firebase.auth.currentUser
        if (user != null) {
            _userInfo.postValue(UserInfo(user))
        }

        viewModelScope.launch {
            Actito.push().observableAllowedUI
                .asFlow()
                .distinctUntilChanged()
                .collect { enabled ->
                    _notificationsEnabled.postValue(enabled)
                }
        }

        viewModelScope.launch {
            try {
                val dnd = Actito.device().fetchDoNotDisturb()
                _dnd.postValue(dnd ?: ActitoDoNotDisturb.default)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch the do not disturb settings.")
            }
        }

        viewModelScope.launch {
            try {
                val tags = Actito.device().fetchTags()

                _announcementsTopicEnabled.postValue(tags.contains(Topic.ANNOUNCEMENTS.rawValue))
                _marketingTopicEnabled.postValue(tags.contains(Topic.MARKETING.rawValue))
                _bestPracticesTopicEnabled.postValue(tags.contains(Topic.BEST_PRACTICES.rawValue))
                _productUpdatesTopicEnabled.postValue(tags.contains(Topic.PRODUCT_UPDATES.rawValue))
                _engineeringTopicEnabled.postValue(tags.contains(Topic.ENGINEERING.rawValue))
                _staffTopicEnabled.postValue(tags.contains(Topic.STAFF.rawValue))
            } catch (_: Exception) {
                Timber.e("Failed to fetch the device tags.")
            }
        }
    }

    fun changeRemoteNotifications(enabled: Boolean) {
        viewModelScope.launch {
            try {
                if (enabled) {
                    Actito.push().enableRemoteNotifications()
                } else {
                    Actito.push().disableRemoteNotifications()
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
                    Actito.device().updateDoNotDisturb(ActitoDoNotDisturb.default)
                } else {
                    Actito.device().clearDoNotDisturb()
                }

                _dndEnabled.postValue(enabled)
                _dnd.postValue(ActitoDoNotDisturb.default)
            } catch (e: Exception) {
                Timber.e(e, "Failed to update the do not disturb settings.")
            }
        }
    }

    fun changeDoNotDisturb(dnd: ActitoDoNotDisturb) {
        viewModelScope.launch {
            try {
                Actito.device().updateDoNotDisturb(dnd)
                _dnd.postValue(dnd)
            } catch (e: Exception) {
                Timber.e(e, "Failed to update the do not disturb settings.")
            }
        }
    }

    fun changeLocationUpdates(enabled: Boolean) {
        if (enabled) {
            Actito.geo().enableLocationUpdates()
        } else {
            Actito.geo().disableLocationUpdates()
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
                    Actito.device().addTag(topic.rawValue)
                } else {
                    Actito.device().removeTag(topic.rawValue)
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
                Actito.events().logPageViewed(PageView.SETTINGS)
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

    private val ActitoDoNotDisturb.Companion.default: ActitoDoNotDisturb
        get() = ActitoDoNotDisturb(
            ActitoDoNotDisturb.defaultStart,
            ActitoDoNotDisturb.defaultEnd,
        )

    private val ActitoDoNotDisturb.Companion.defaultStart: ActitoTime
        get() = ActitoTime(hours = 23, minutes = 0)

    private val ActitoDoNotDisturb.Companion.defaultEnd: ActitoTime
        get() = ActitoTime(hours = 8, minutes = 0)
}
