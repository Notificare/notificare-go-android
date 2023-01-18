package re.notifica.go.ui.home

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.geo.NotificareGeo
import re.notifica.geo.ktx.geo
import re.notifica.geo.models.NotificareBeacon
import re.notifica.geo.models.NotificareRegion
import re.notifica.go.ktx.PageView
import re.notifica.go.ktx.logPageViewed
import re.notifica.go.live_activities.LiveActivitiesController
import re.notifica.go.live_activities.models.CoffeeBrewerContentState
import re.notifica.go.live_activities.models.CoffeeBrewingState
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
    private val liveActivitiesController: LiveActivitiesController,
) : ViewModel(), NotificareGeo.Listener, DefaultLifecycleObserver {
    private val _products = MutableLiveData<List<Product>>()
    val products: LiveData<List<Product>> = _products

    private val _rangedBeacons = MutableLiveData<List<NotificareBeacon>>()
    val rangedBeacons: LiveData<List<NotificareBeacon>> = _rangedBeacons

    val coffeeBrewerUiState: LiveData<CoffeeBrewerUiState> = liveActivitiesController.coffeeActivityStream
        .map { CoffeeBrewerUiState(it?.state) }
        .asLiveData()

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

    fun createCoffeeSession() {
        viewModelScope.launch {
            try {
                val contentState = CoffeeBrewerContentState(
                    state = CoffeeBrewingState.GRINDING,
                    remaining = 5,
                )

                liveActivitiesController.createCoffeeActivity(contentState)
                Timber.i("Live activity presented.")
            } catch (e: Exception) {
                Timber.e(e, "Failed to create the live activity.")
            }
        }
    }

    fun continueCoffeeSession() {
        val currentBrewingState = coffeeBrewerUiState.value?.brewingState ?: return

        val contentState = when (currentBrewingState) {
            CoffeeBrewingState.GRINDING -> CoffeeBrewerContentState(
                state = CoffeeBrewingState.BREWING,
                remaining = 4,
            )
            CoffeeBrewingState.BREWING -> CoffeeBrewerContentState(
                state = CoffeeBrewingState.SERVED,
                remaining = 0,
            )
            CoffeeBrewingState.SERVED -> return
        }

        viewModelScope.launch {
            try {
                liveActivitiesController.updateCoffeeActivity(contentState)
            } catch (e: Exception) {
                Timber.e(e, "Failed to update the live activity.")
            }
        }
    }

    fun cancelCoffeeSession() {
        viewModelScope.launch {
            try {
                liveActivitiesController.clearCoffeeActivity()
            } catch (e: Exception) {
                Timber.e(e, "Failed to end the live activity.")
            }
        }
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
