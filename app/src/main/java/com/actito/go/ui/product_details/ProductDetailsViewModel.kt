package com.actito.go.ui.product_details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.actito.Actito
import com.actito.go.ktx.PageView
import com.actito.go.ktx.logAddToCart
import com.actito.go.ktx.logCartUpdated
import com.actito.go.ktx.logPageViewed
import com.actito.go.ktx.logProductView
import com.actito.go.models.Product
import com.actito.go.storage.db.ActitoDatabase
import com.actito.go.storage.db.entities.CartEntryEntity
import com.actito.go.storage.db.mappers.toModel
import com.actito.ktx.events
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class ProductDetailsViewModel @Inject constructor(
    private val database: ActitoDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val arguments = ProductDetailsFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _product = MutableLiveData<Product>()
    val product: LiveData<Product> = _product

    init {
        viewModelScope.launch {
            try {
                Actito.events().logPageViewed(PageView.PRODUCT_DETAILS)
            } catch (e: Exception) {
                Timber.e(e, "Failed to log a custom event.")
            }

            try {
                val product = withContext(Dispatchers.IO) {
                    database.products().getById(arguments.productId)?.toModel()
                } ?: return@launch

                _product.postValue(product)

                try {
                    Actito.events().logProductView(product)
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
        Actito.events().logAddToCart(product)
        Actito.events().logCartUpdated(entries.map { it.product.toModel() })
    }
}
