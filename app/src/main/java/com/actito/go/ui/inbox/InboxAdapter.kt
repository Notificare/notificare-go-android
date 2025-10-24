package com.actito.go.ui.inbox

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.actito.go.R
import com.actito.go.databinding.ViewInboxItemBinding
import com.actito.go.databinding.ViewInboxSectionBinding
import com.actito.go.ktx.dp
import com.actito.inbox.models.ActitoInboxItem
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import java.text.SimpleDateFormat
import java.util.Locale

class InboxAdapter(
    private val onInboxItemClicked: (ActitoInboxItem) -> Unit,
    private val onInboxItemLongPressed: (ActitoInboxItem) -> Unit,
) : ListAdapter<InboxViewModel.InboxListEntry, RecyclerView.ViewHolder>(InboxDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (ViewType.entries[viewType]) {
            ViewType.SECTION -> SectionViewHolder(
                ViewInboxSectionBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
            ViewType.ITEM -> ItemViewHolder(
                ViewInboxItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is InboxViewModel.InboxListEntry.Section -> (holder as SectionViewHolder).bind(item)
            is InboxViewModel.InboxListEntry.Item -> (holder as ItemViewHolder).bind(item)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is InboxViewModel.InboxListEntry.Section -> ViewType.SECTION.ordinal
            is InboxViewModel.InboxListEntry.Item -> ViewType.ITEM.ordinal
        }
    }

    private enum class ViewType {
        SECTION,
        ITEM;
    }

    private class SectionViewHolder(
        private val binding: ViewInboxSectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(section: InboxViewModel.InboxListEntry.Section) {
            binding.titleLabel.text = section.title
        }
    }

    private inner class ItemViewHolder(
        private val binding: ViewInboxItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        private var inboxItem: ActitoInboxItem? = null

        private val dateFormatter: SimpleDateFormat
            get() = SimpleDateFormat("d MMM", Locale.getDefault())

        private val timeFormatter: SimpleDateFormat
            get() = SimpleDateFormat("HH:mm", Locale.getDefault())

        init {
            binding.root.setOnClickListener {
                inboxItem?.also { onInboxItemClicked(it) }
            }

            binding.root.setOnLongClickListener {
                inboxItem?.also { onInboxItemLongPressed(it) }
                return@setOnLongClickListener true
            }
        }

        fun bind(item: InboxViewModel.InboxListEntry.Item) {
            inboxItem = item.inboxItem

            Glide.with(binding.attachmentImage)
                .load(item.inboxItem.notification.attachments.firstOrNull()?.uri)
                .placeholder(R.drawable.shape_inbox_attachment_placeholder)
                .apply(RequestOptions.bitmapTransform(RoundedCorners(8.dp.toInt())))
                .into(binding.attachmentImage)

            binding.titleLabel.text = item.inboxItem.notification.title
            binding.titleLabel.isGone = item.inboxItem.notification.title.isNullOrBlank()

            binding.messageLabel.text = item.inboxItem.notification.message
            binding.messageLabel.maxLines = if (item.inboxItem.notification.title.isNullOrBlank()) 2 else 1

            binding.unreadIndicator.isInvisible = item.inboxItem.opened

            binding.timeLabel.text =
                if (DateUtils.isToday(item.inboxItem.time.time)) timeFormatter.format(item.inboxItem.time)
                else dateFormatter.format(item.inboxItem.time)
        }
    }
}

private class InboxDiffCallback : DiffUtil.ItemCallback<InboxViewModel.InboxListEntry>() {
    override fun areItemsTheSame(
        oldItem: InboxViewModel.InboxListEntry,
        newItem: InboxViewModel.InboxListEntry
    ): Boolean {
        if (oldItem is InboxViewModel.InboxListEntry.Section && newItem is InboxViewModel.InboxListEntry.Section) {
            return oldItem.title == newItem.title
        }

        if (oldItem is InboxViewModel.InboxListEntry.Item && newItem is InboxViewModel.InboxListEntry.Item) {
            return oldItem.inboxItem.id == newItem.inboxItem.id
        }

        return false
    }

    override fun areContentsTheSame(
        oldItem: InboxViewModel.InboxListEntry,
        newItem: InboxViewModel.InboxListEntry
    ): Boolean {
        if (oldItem is InboxViewModel.InboxListEntry.Section && newItem is InboxViewModel.InboxListEntry.Section) {
            return oldItem.title == newItem.title
        }

        if (oldItem is InboxViewModel.InboxListEntry.Item && newItem is InboxViewModel.InboxListEntry.Item) {
            return oldItem.inboxItem == newItem.inboxItem
        }

        return false
    }

}
