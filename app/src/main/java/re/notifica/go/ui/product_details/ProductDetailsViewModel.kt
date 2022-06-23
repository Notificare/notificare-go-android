package re.notifica.go.ui.product_details

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import re.notifica.Notificare
import re.notifica.go.ktx.*
import re.notifica.go.models.Product
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

    private val _product = MutableLiveData<Product>()
    val product: LiveData<Product> = _product

    init {
        viewModelScope.launch {
            try {
                Notificare.events().logPageViewed(PageView.PRODUCT_DETAILS)
            } catch (e: Exception) {
                Timber.e(e, "Failed to log a custom event.")
            }

            try {
                val product = withContext(Dispatchers.IO) {
                    database.products().getById(arguments.productId)?.toModel()
                } ?: return@launch

                _product.postValue(product)

                try {
                    Notificare.events().logProductView(product)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to log product viewed event.")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch the product from the database.")
            }
        }
    }

    suspend fun addToCart() {
        val product = product.value ?: return

        database.cartEntries().add(
            CartEntryEntity(
                id = 0,
                time = Date(),
                productId = product.id
            )
        )

        val entries = database.cartEntries().getEntriesWithProduct()
        Notificare.events().logAddToCart(product)
        Notificare.events().logCartUpdated(entries.map { it.product.toModel() })
    }
}
