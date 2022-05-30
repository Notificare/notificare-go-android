package re.notifica.go.ui.cart

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.go.ktx.logCartCleared
import re.notifica.go.ktx.logCartUpdated
import re.notifica.go.ktx.logPurchase
import re.notifica.go.ktx.logRemoveFromCart
import re.notifica.go.storage.db.NotificareDatabase
import re.notifica.go.storage.db.entities.CartEntryWithProduct
import re.notifica.go.storage.db.mappers.toModel
import re.notifica.ktx.events
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    private val database: NotificareDatabase,
) : ViewModel() {
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
        Notificare.events().logPurchase(entries.map { it.product.toModel() })

        database.cartEntries().clear()
        Notificare.events().logCartCleared()
    }

    suspend fun remove(entry: CartEntryWithProduct) {
        database.cartEntries().remove(entry.cartEntry.id)

        val entries = database.cartEntries().getEntriesWithProduct()
        Notificare.events().logRemoveFromCart(entry.product.toModel())
        Notificare.events().logCartUpdated(entries.map { it.product.toModel() })

        if (entries.isEmpty()) {
            Notificare.events().logCartCleared()
        }
    }
}
