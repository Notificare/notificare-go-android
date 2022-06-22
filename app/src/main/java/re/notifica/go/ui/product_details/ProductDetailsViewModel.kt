package re.notifica.go.ui.product_details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.go.ktx.*
import re.notifica.go.storage.db.NotificareDatabase
import re.notifica.go.storage.db.entities.CartEntryEntity
import re.notifica.go.storage.db.mappers.toModel
import re.notifica.ktx.events
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ProductDetailsViewModel @Inject constructor(
    private val database: NotificareDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val arguments = ProductDetailsFragmentArgs.fromSavedStateHandle(savedStateHandle)

    init {
        viewModelScope.launch {
            try {
                Notificare.events().logPageViewed(PageView.PRODUCT_DETAILS)
            } catch (e: Exception) {
                Timber.e(e, "Failed to log a custom event.")
            }

            try {
                Notificare.events().logProductView(arguments.product)
            } catch (e: Exception) {
                Timber.e(e, "Failed to log product viewed event.")
            }
        }
    }

    suspend fun addToCart() {
        database.cartEntries().add(
            CartEntryEntity(
                id = 0,
                time = Date(),
                productId = arguments.product.id
            )
        )

        val entries = database.cartEntries().getEntriesWithProduct()
        Notificare.events().logAddToCart(arguments.product)
        Notificare.events().logCartUpdated(entries.map { it.product.toModel() })
    }
}
