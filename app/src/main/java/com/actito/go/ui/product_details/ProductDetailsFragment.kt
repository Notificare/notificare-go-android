package com.actito.go.ui.product_details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.actito.go.R
import com.actito.go.core.formatPrice
import com.actito.go.databinding.FragmentProductDetailsBinding
import com.actito.go.ktx.toHtml
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProductDetailsFragment : Fragment() {
    private lateinit var viewModel: ProductDetailsViewModel
    private lateinit var binding: FragmentProductDetailsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentProductDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this)[ProductDetailsViewModel::class.java]

        binding.addToCartButton.setOnClickListener {
            lifecycleScope.launch {
                try {
                    binding.addToCartButton.isEnabled = false

                    viewModel.addToCart()

                    Snackbar.make(binding.root, R.string.product_details_cart_success, Snackbar.LENGTH_SHORT).show()
                } catch (_: Exception) {
                    Snackbar.make(binding.root, R.string.product_details_cart_failure, Snackbar.LENGTH_SHORT).show()
                } finally {
                    binding.addToCartButton.isEnabled = true
                }
            }
        }

        viewModel.product.observe(viewLifecycleOwner) { product ->
            Glide.with(this)
                .load(product.imageUrl)
                .into(binding.showcaseImage)

            binding.nameLabel.text = product.name
            binding.priceLabel.text = product.price.let(::formatPrice)
            binding.descriptionLabel.text = product.description.toHtml()

            binding.content.isVisible = true
        }
    }
}
