package com.actito.go.ui.cart

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.actito.go.R
import com.actito.go.core.formatPrice
import com.actito.go.databinding.FragmentCartBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Date

@AndroidEntryPoint
class CartFragment : Fragment() {
    private val viewModel: CartViewModel by viewModels()
    private lateinit var binding: FragmentCartBinding
    private val adapter = CartAdapter()
    private val swipeHelper = ItemTouchHelper(CartItemTouchCallback(::onItemSwiped))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(viewModel)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.list.adapter = adapter
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        swipeHelper.attachToRecyclerView(binding.list)

        binding.purchaseButton.setOnClickListener {
            lifecycleScope.launch {
                try {
                    binding.purchaseButton.isEnabled = false
                    viewModel.purchase()

                    Snackbar.make(binding.root, R.string.cart_purchase_success, Snackbar.LENGTH_SHORT).show()
                } catch (_: Exception) {
                    Snackbar.make(binding.root, R.string.cart_purchase_failure, Snackbar.LENGTH_SHORT).show()
                } finally {
                    binding.purchaseButton.isEnabled = true
                }
            }
        }

        binding.productsListButton.setOnClickListener {
            findNavController().navigate(R.id.cart_to_products_list_action)
        }

        viewModel.entries.observe(viewLifecycleOwner) { entries ->
            adapter.submitList(entries)

            binding.contentGroup.isVisible = entries.isNotEmpty()
            binding.emptyMessageGroup.isVisible = entries.isEmpty()

            if (entries.isNotEmpty()) {
                binding.totalAmountLabel.text = entries.sumOf { it.product.price }.let(::formatPrice)
                binding.lastModificationLabel.text = DateUtils.getRelativeTimeSpanString(
                    entries.minOf { it.cartEntry.time }.time,
                    Date().time,
                    DateUtils.SECOND_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_ALL
                )
            }
        }
    }

    private fun onItemSwiped(position: Int) {
        val item = adapter.currentList[position]

        lifecycleScope.launch {
            try {
                viewModel.remove(item)
            } catch (e: Exception) {
                Snackbar.make(binding.root, e.toString(), Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}
