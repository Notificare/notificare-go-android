package re.notifica.go.ui.inbox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.go.databinding.FragmentInboxBinding
import re.notifica.inbox.models.NotificareInboxItem
import re.notifica.push.ui.ktx.pushUI

@AndroidEntryPoint
class InboxFragment : Fragment() {
    private val viewModel: InboxViewModel by viewModels()
    private lateinit var binding: FragmentInboxBinding
    private val adapter = InboxAdapter(::onInboxItemClicked)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentInboxBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.list.adapter = adapter
        binding.list.layoutManager = LinearLayoutManager(requireContext())

        viewModel.items.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)

            binding.emptyMessageLabel.isVisible = items.isEmpty()
            binding.list.isVisible = items.isNotEmpty()
        }
    }


    private fun onInboxItemClicked(item: NotificareInboxItem) {
        lifecycleScope.launch {
            try {
                val notification = viewModel.open(item)
                Notificare.pushUI().presentNotification(requireActivity(), notification)
            } catch (e: Exception) {
                // TODO: handle error scenario
            }
        }
    }
}
