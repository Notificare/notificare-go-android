package com.actito.go.ui.events

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.actito.Actito
import com.actito.go.ktx.PageView
import com.actito.go.ktx.logPageViewed
import com.actito.ktx.events
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class EventsViewModel @Inject constructor(

) : ViewModel() {
    val _eventName = MutableStateFlow("")
    val eventName: LiveData<String> = _eventName.asLiveData()

    val _eventAttributes = MutableStateFlow(listOf(Attribute(name = "", value = "")))
    val eventAttributes: LiveData<List<Attribute>> = _eventAttributes.asLiveData()

    init {
        viewModelScope.launch {
            try {
                Actito.events().logPageViewed(PageView.EVENTS)
            } catch (e: Exception) {
                Timber.e(e, "Failed to log a custom event.")
            }
        }
    }

    fun createAttribute() {
        val attributes = _eventAttributes.value.toMutableList()
        attributes.add(Attribute(name = "", value = ""))

        _eventAttributes.tryEmit(attributes)
    }

    suspend fun submitEvent() {
        val name = _eventName.value.trim()
        val attributes = _eventAttributes.value
            .associate { it.name to it.value }
            .mapKeys { it.key.trim() }
            .mapValues { it.value.trim() }
            .filter { it.key.isNotBlank() && it.value.isNotBlank() }

        Actito.events().logCustom(name, attributes.ifEmpty { null })

        _eventName.tryEmit("")
        _eventAttributes.tryEmit(listOf(Attribute(name = "", value = "")))
    }

    data class Attribute(
        var name: String,
        var value: String,
    )
}
