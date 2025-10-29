package com.actito.go.ui.home

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.actito.Actito
import com.actito.geo.ActitoGeo
import com.actito.geo.ktx.geo
import com.actito.geo.models.ActitoBeacon
import com.actito.geo.models.ActitoRegion
import com.actito.go.ktx.PageView
import com.actito.go.ktx.logPageViewed
import com.actito.go.live_activities.LiveActivitiesController
import com.actito.go.live_activities.models.CoffeeBrewerContentState
import com.actito.go.live_activities.models.CoffeeBrewingState
import com.actito.go.models.Product
import com.actito.go.storage.db.ActitoDatabase
import com.actito.go.storage.db.mappers.toModel
import com.actito.go.storage.preferences.ActitoSharedPreferences
import com.actito.ktx.events
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val database: ActitoDatabase,
    private val preferences: ActitoSharedPreferences,
    private val liveActivitiesController: LiveActivitiesController,
) : ViewModel(), ActitoGeo.Listener, DefaultLifecycleObserver {
    private val _products = MutableLiveData<List<Product>>()
    val products: LiveData<List<Product>> = _products

    private val _rangedBeacons = MutableLiveData<List<ActitoBeacon>>()
    val rangedBeacons: LiveData<List<ActitoBeacon>> = _rangedBeacons

    val coffeeBrewerUiState: LiveData<CoffeeBrewerUiState> = liveActivitiesController.coffeeActivityStream
        .map { CoffeeBrewerUiState(it?.state) }
        .asLiveData()

    init {
        Actito.geo().addListener(this)

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
        Actito.geo().removeListener(this)
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

    // region ActitoGeo.Listener

    override fun onRegionEntered(region: ActitoRegion) {
        Timber.i("Entered region '${region.name}'.")
    }

    override fun onRegionExited(region: ActitoRegion) {
        Timber.i("Exited region '${region.name}'.")
    }

    override fun onBeaconsRanged(region: ActitoRegion, beacons: List<ActitoBeacon>) {
        _rangedBeacons.postValue(beacons)
    }

    // endregion

    override fun onCreate(owner: LifecycleOwner) {
        viewModelScope.launch {
            try {
                Actito.events().logPageViewed(PageView.HOME)
            } catch (e: Exception) {
                Timber.e(e, "Failed to log a custom event.")
            }
        }
    }
}
