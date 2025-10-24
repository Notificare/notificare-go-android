package com.actito.go.ui.products_list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.actito.go.databinding.FragmentProductsListBinding
import com.actito.go.models.Product
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProductsListFragment : Fragment() {
    private val viewModel: ProductsListViewModel by viewModels()
    private lateinit var binding: FragmentProductsListBinding
    private val adapter = ProductsAdapter(::onProductClicked)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentProductsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.list.adapter = adapter
        binding.list.layoutManager = LinearLayoutManager(requireContext())

        viewModel.products.observe(viewLifecycleOwner) { products ->
            adapter.submitList(products)
        }
    }

    private fun onProductClicked(product: Product) {
        findNavController().navigate(
            ProductsListFragmentDirections.productsListToProductDetailsAction(product.id)
        )
    }
}
