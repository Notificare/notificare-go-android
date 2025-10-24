package com.actito.go.ui.products_list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.actito.Actito
import com.actito.go.ktx.PageView
import com.actito.go.ktx.logPageViewed
import com.actito.go.models.Product
import com.actito.go.storage.db.ActitoDatabase
import com.actito.go.storage.db.mappers.toModel
import com.actito.ktx.events
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ProductsListViewModel @Inject constructor(
    private val database: ActitoDatabase,
) : ViewModel() {
    private val _products = MutableLiveData<List<Product>>()
    val products: LiveData<List<Product>> = _products

    init {
        viewModelScope.launch {
            try {
                Actito.events().logPageViewed(PageView.PRODUCTS)
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
