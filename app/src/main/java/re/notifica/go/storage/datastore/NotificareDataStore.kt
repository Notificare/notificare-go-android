package re.notifica.go.storage.datastore

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import re.notifica.go.models.CoffeeBrewerContentState
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "re.notifica.go.datastore")
private val KEY_COFFEE_BREWER_CONTENT_STATE = stringPreferencesKey("coffee_brewer_content_state")

class NotificareDataStore @Inject constructor(
    private val application: Application,
    private val moshi: Moshi,
) {

    val coffeeBrewerContentStateStream: Flow<CoffeeBrewerContentState?> =
        application.dataStore.data.map { preferences ->
            val str = preferences[KEY_COFFEE_BREWER_CONTENT_STATE] ?: return@map null

            val adapter = moshi.adapter(CoffeeBrewerContentState::class.java)
            return@map adapter.fromJson(str)
        }

    suspend fun updateCoffeeBrewerContentState(
        contentState: CoffeeBrewerContentState?
    ): Unit = withContext(Dispatchers.IO) {
        application.dataStore.edit { preferences ->
            val str = contentState?.let {
                val adapter = moshi.adapter(CoffeeBrewerContentState::class.java)
                adapter.toJson(it)
            }

            if (str != null) {
                preferences[KEY_COFFEE_BREWER_CONTENT_STATE] = str
            } else {
                preferences.remove(KEY_COFFEE_BREWER_CONTENT_STATE)
            }
        }
    }
}
