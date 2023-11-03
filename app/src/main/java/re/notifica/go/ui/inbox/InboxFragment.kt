package re.notifica.go.ui.inbox

import android.os.Bundle
import android.view.*
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.go.R
import re.notifica.go.databinding.FragmentInboxBinding
import re.notifica.inbox.models.NotificareInboxItem
import re.notifica.push.ui.ktx.pushUI
import timber.log.Timber

@AndroidEntryPoint
class InboxFragment : Fragment(), MenuProvider {
    private val viewModel: InboxViewModel by viewModels()
    private lateinit var binding: FragmentInboxBinding
    private val adapter = InboxAdapter(::onInboxItemClicked, ::onInboxItemLongPressed)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.inbox_menu, menu)
    }

    override fun onPrepareMenu(menu: Menu) {
        val menuEnabled = !viewModel.items.value.isNullOrEmpty()
        menu.findItem(R.id.action_mark_all_as_read).isVisible = menuEnabled
        menu.findItem(R.id.action_remove_all).isVisible = menuEnabled
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_mark_all_as_read -> {
                lifecycleScope.launch {
                    try {
                        viewModel.markAllAsRead()
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to mark all items as read.")
                    }
                }
            }

            R.id.action_remove_all -> {
                lifecycleScope.launch {
                    try {
                        viewModel.clear()
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to clear the inbox.")
                    }
                }
            }

            else -> return false
        }

        return true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentInboxBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.list.adapter = adapter
        binding.list.layoutManager = LinearLayoutManager(requireContext())

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        viewModel.items.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)

            binding.emptyMessageLabel.isVisible = items.isEmpty()
            binding.list.isVisible = items.isNotEmpty()

            activity?.invalidateOptionsMenu()
        }
    }


    private fun onInboxItemClicked(item: NotificareInboxItem) {
        lifecycleScope.launch {
            try {
                val notification = viewModel.open(item)
                Notificare.pushUI().presentNotification(requireActivity(), notification)
            } catch (e: Exception) {
                Timber.e(e, "Failed to open an inbox item.")
                // TODO: handle error scenario
            }
        }
    }

    private fun onInboxItemLongPressed(item: NotificareInboxItem) {
        InboxItemOptionsBottomSheet(
            onOpenClicked = { onInboxItemClicked(item) },
            onMarkAsReadClicked = { onMarkItemAsReadClicked(item) },
            onRemoveClicked = { onRemoveItemClicked(item) }
        ).show(childFragmentManager, "options-bottom-sheet")
    }

    private fun onMarkItemAsReadClicked(item: NotificareInboxItem) {
        lifecycleScope.launch {
            try {
                viewModel.markAsRead(item)
            } catch (e: Exception) {
                Timber.e(e, "Failed to mark an item as read.")
                // TODO: handle error scenario
            }
        }
    }

    private fun onRemoveItemClicked(item: NotificareInboxItem) {
        lifecycleScope.launch {
            try {
                viewModel.remove(item)
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove an item.")
                // TODO: handle error scenario
            }
        }
    }
}
