package re.notifica.go.ui.product_details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import re.notifica.go.R
import re.notifica.go.core.formatPrice
import re.notifica.go.databinding.FragmentProductDetailsBinding

@AndroidEntryPoint
class ProductDetailsFragment : Fragment() {
    private lateinit var viewModel: ProductDetailsViewModel
    private val args: ProductDetailsFragmentArgs by navArgs()
    private lateinit var binding: FragmentProductDetailsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentProductDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this)[ProductDetailsViewModel::class.java]

        Glide.with(this)
            .load(args.product.imageUrl)
            .into(binding.showcaseImage)

        binding.nameLabel.text = args.product.name
        binding.priceLabel.text = args.product.price.let(::formatPrice)
        binding.descriptionLabel.text = args.product.description

        binding.addToCartButton.setOnClickListener {
            lifecycleScope.launch {
                try {
                    binding.addToCartButton.isEnabled = false

                    viewModel.addToCart()

                    Snackbar.make(binding.root, R.string.product_details_cart_success, Snackbar.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Snackbar.make(binding.root, R.string.product_details_cart_failure, Snackbar.LENGTH_SHORT).show()
                } finally {
                    binding.addToCartButton.isEnabled = true
                }
            }
        }
    }
}
