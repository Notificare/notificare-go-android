package com.actito.go.ui.inbox

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.actito.Actito
import com.actito.go.R
import com.actito.go.core.sevenDaysAgo
import com.actito.go.core.today
import com.actito.go.core.yesterday
import com.actito.go.ktx.PageView
import com.actito.go.ktx.logPageViewed
import com.actito.inbox.ktx.inbox
import com.actito.inbox.models.ActitoInboxItem
import com.actito.ktx.events
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar
import java.util.Locale
import java.util.SortedSet
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class InboxViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ViewModel() {
    private val _items = MutableLiveData<List<InboxListEntry>>()
    val items: LiveData<List<InboxListEntry>> = _items

    init {
        viewModelScope.launch {
            try {
                Actito.events().logPageViewed(PageView.INBOX)
            } catch (e: Exception) {
                Timber.e(e, "Failed to log a custom event.")
            }
        }

        viewModelScope.launch {
            Actito.inbox().observableItems
                .asFlow()
                .collect { items ->
                    val sectioned = createSections(items)
                    _items.postValue(sectioned)
                }
        }
    }

    suspend fun open(item: ActitoInboxItem) = Actito.inbox().open(item)

    suspend fun markAsRead(item: ActitoInboxItem) = Actito.inbox().markAsRead(item)

    suspend fun markAllAsRead() = Actito.inbox().markAllAsRead()

    suspend fun remove(item: ActitoInboxItem) = Actito.inbox().remove(item)

    suspend fun clearInbox() = Actito.inbox().clear()

    private fun createSections(items: SortedSet<ActitoInboxItem>): List<InboxListEntry> {
        val result = mutableListOf<InboxListEntry>()

        val itemsToday = items.filter { it.time >= today() }
        if (itemsToday.isNotEmpty()) {
            result.add(InboxListEntry.Section(context.getString(R.string.inbox_section_today)))
            result.addAll(itemsToday.map { InboxListEntry.Item(it) }.sortedByDescending { it.inboxItem.time })
        }

        val itemsYesterday = items.filter { it.time >= yesterday() && it.time < today() }
        if (itemsYesterday.isNotEmpty()) {
            result.add(InboxListEntry.Section(context.getString(R.string.inbox_section_yesterday)))
            result.addAll(itemsYesterday.map { InboxListEntry.Item(it) }.sortedByDescending { it.inboxItem.time })
        }

        val itemsSevenDaysAgo = items.filter { it.time >= sevenDaysAgo() && it.time < yesterday() }
        if (itemsSevenDaysAgo.isNotEmpty()) {
            result.add(InboxListEntry.Section(context.getString(R.string.inbox_section_last_seven_days)))
            result.addAll(itemsSevenDaysAgo.map { InboxListEntry.Item(it) }.sortedByDescending { it.inboxItem.time })
        }

        // Remaining items are grouped by year/month descending (most recent items come first).
        items.filter { it.time < sevenDaysAgo() }
            .groupBy {
                val calendar = Calendar.getInstance().apply { time = it.time }
                val month = calendar.get(Calendar.MONTH)
                val year = calendar.get(Calendar.YEAR)

                year to month
            }
            .filter { it.value.isNotEmpty() }
            .toSortedMap(compareByDescending<Pair<Int, Int>> { it.first }.thenByDescending { it.second })
            .forEach {
                val month = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.MONTH, it.key.second)
                    set(Calendar.YEAR, it.key.first)
                }.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())

                result.add(InboxListEntry.Section("$month ${it.key.first}"))
                result.addAll(
                    it.value.map { item -> InboxListEntry.Item(item) }
                        .sortedByDescending { item -> item.inboxItem.time }
                )
            }

        return result
    }

    sealed class InboxListEntry {
        data class Section(
            val title: String,
        ) : InboxListEntry()

        data class Item(
            val inboxItem: ActitoInboxItem,
        ) : InboxListEntry()
    }
}
