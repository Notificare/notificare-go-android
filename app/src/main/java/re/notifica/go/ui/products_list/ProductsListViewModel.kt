package re.notifica.go.ui.products_list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.go.ktx.PageView
import re.notifica.go.ktx.logPageViewed
import re.notifica.go.models.Product
import re.notifica.go.storage.db.NotificareDatabase
import re.notifica.go.storage.db.mappers.toModel
import re.notifica.ktx.events
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ProductsListViewModel @Inject constructor(
    private val database: NotificareDatabase,
) : ViewModel() {
    private val _products = MutableLiveData<List<Product>>()
    val products: LiveData<List<Product>> = _products

    init {
        viewModelScope.launch {
            try {
                Notificare.events().logPageViewed(PageView.PRODUCTS)
            } catch (e: Exception) {
                Timber.e(e, "Failed to log a custom event.")
            }

            database.products().getAllFlow()
                .flowOn(Dispatchers.IO)
                .collect { products ->
                    _products.postValue(products.map { it.toModel() })
                }
        }
    }
}
