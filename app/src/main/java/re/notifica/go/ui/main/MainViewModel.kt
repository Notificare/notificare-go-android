package re.notifica.go.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import re.notifica.go.storage.preferences.NotificareSharedPreferences
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    preferences: NotificareSharedPreferences,
) : ViewModel() {
    private val _storeEnabled = MutableLiveData(false)
    val storeEnabled: LiveData<Boolean> = _storeEnabled

    init {
        _storeEnabled.postValue(preferences.hasStoreEnabled)
    }
}
