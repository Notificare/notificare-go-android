package com.actito.go.ui.cart

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.actito.Actito
import com.actito.go.ktx.PageView
import com.actito.go.ktx.logCartCleared
import com.actito.go.ktx.logCartUpdated
import com.actito.go.ktx.logPageViewed
import com.actito.go.ktx.logPurchase
import com.actito.go.ktx.logRemoveFromCart
import com.actito.go.live_activities.LiveActivitiesController
import com.actito.go.live_activities.models.OrderContentState
import com.actito.go.live_activities.models.OrderStatus
import com.actito.go.storage.db.ActitoDatabase
import com.actito.go.storage.db.entities.CartEntryWithProduct
import com.actito.go.storage.db.mappers.toModel
import com.actito.ktx.events
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    private val database: ActitoDatabase,
    private val liveActivitiesController: LiveActivitiesController,
) : ViewModel(), DefaultLifecycleObserver {
    private val _entries = MutableLiveData<List<CartEntryWithProduct>>()
    val entries: LiveData<List<CartEntryWithProduct>> = _entries

    init {
        viewModelScope.launch {
            database.cartEntries().getEntriesWithProductFlow()
                .flowOn(Dispatchers.IO)
                .collect { entries ->
                    _entries.postValue(entries)
                }
        }
    }

    suspend fun purchase() {
        val entries = database.cartEntries().getEntriesWithProduct()
        Actito.events().logPurchase(entries.map { it.product.toModel() })

        database.cartEntries().clear()
        Actito.events().logCartCleared()

        liveActivitiesController.createOrderActivity(
            OrderContentState(
                status = OrderStatus.PREPARING
            )
        )
    }

    suspend fun remove(entry: CartEntryWithProduct) {
        database.cartEntries().remove(entry.cartEntry.id)

        val entries = database.cartEntries().getEntriesWithProduct()
        Actito.events().logRemoveFromCart(entry.product.toModel())
        Actito.events().logCartUpdated(entries.map { it.product.toModel() })

        if (entries.isEmpty()) {
            Actito.events().logCartCleared()
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        viewModelScope.launch {
            try {
                Actito.events().logPageViewed(PageView.CART)
            } catch (e: Exception) {
                Timber.e(e, "Failed to log a custom event.")
            }
        }
    }
}
