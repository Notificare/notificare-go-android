package re.notifica.go.ui.home

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.geo.NotificareGeo
import re.notifica.geo.ktx.geo
import re.notifica.geo.models.NotificareBeacon
import re.notifica.geo.models.NotificareRegion
import re.notifica.go.ktx.PageView
import re.notifica.go.ktx.logPageViewed
import re.notifica.go.models.Product
import re.notifica.go.storage.db.NotificareDatabase
import re.notifica.go.storage.db.mappers.toModel
import re.notifica.go.storage.preferences.NotificareSharedPreferences
import re.notifica.ktx.events
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val database: NotificareDatabase,
    private val preferences: NotificareSharedPreferences,
) : ViewModel(), NotificareGeo.Listener, DefaultLifecycleObserver {
    private val _products = MutableLiveData<List<Product>>()
    val products: LiveData<List<Product>> = _products

    private val _rangedBeacons = MutableLiveData<List<NotificareBeacon>>()
    val rangedBeacons: LiveData<List<NotificareBeacon>> = _rangedBeacons

    init {
        Notificare.geo().addListener(this)

        viewModelScope.launch {
            if (!preferences.hasStoreEnabled) {
                _products.postValue(emptyList())
                return@launch
            }

            database.products().getHighlightedFlow()
                .flowOn(Dispatchers.IO)
                .collect { products ->
                    _products.postValue(products.map { it.toModel() })
                }
        }
    }

    override fun onCleared() {
        Notificare.geo().removeListener(this)
    }

    // region NotificareGeo.Listener

    override fun onRegionEntered(region: NotificareRegion) {
        Timber.i("Entered region '${region.name}'.")
    }

    override fun onRegionExited(region: NotificareRegion) {
        Timber.i("Exited region '${region.name}'.")
    }

    override fun onBeaconsRanged(region: NotificareRegion, beacons: List<NotificareBeacon>) {
        _rangedBeacons.postValue(beacons)
    }

    // endregion

    override fun onCreate(owner: LifecycleOwner) {
        viewModelScope.launch {
            try {
                Notificare.events().logPageViewed(PageView.HOME)
            } catch (e: Exception) {
                Timber.e(e, "Failed to log a custom event.")
            }
        }
    }
}
