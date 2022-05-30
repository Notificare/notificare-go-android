package re.notifica.go.ui.events

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import re.notifica.Notificare
import re.notifica.ktx.events
import javax.inject.Inject

@HiltViewModel
class EventsViewModel @Inject constructor(

) : ViewModel() {
    val _eventName = MutableStateFlow("")
    val eventName: LiveData<String> = _eventName.asLiveData()

    val _eventAttributes = MutableStateFlow(listOf(Attribute(name = "", value = "")))
    val eventAttributes: LiveData<List<Attribute>> = _eventAttributes.asLiveData()


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

        Notificare.events().logCustom(name, attributes.ifEmpty { null })

        _eventName.tryEmit("")
        _eventAttributes.tryEmit(listOf(Attribute(name = "", value = "")))
    }

    data class Attribute(
        var name: String,
        var value: String,
    )
}
