package com.actito.go.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.actito.go.R
import com.actito.go.databinding.FragmentHomeBinding
import com.actito.go.live_activities.models.CoffeeBrewingState
import com.actito.go.models.Product
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var binding: FragmentHomeBinding
    private val productsAdapter = ProductsAdapter(::onProductClicked)
    private val beaconsAdapter = BeaconsAdapter()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.productsList.adapter = productsAdapter
        binding.productsList.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        binding.beaconsList.adapter = beaconsAdapter
        binding.beaconsList.layoutManager = LinearLayoutManager(requireContext())

        setupListeners()
        setupObservers()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(viewModel)
    }

    private fun setupListeners() {
        binding.productsListButton.setOnClickListener {
            findNavController().navigate(R.id.home_to_products_list_action)
        }

        binding.eventsButton.setOnClickListener {
            findNavController().navigate(R.id.home_to_events_action)
        }
    }

    private fun setupObservers() {
        viewModel.products.observe(viewLifecycleOwner) { products ->
            productsAdapter.submitList(products)

            binding.productsGroup.isVisible = products.isNotEmpty()
        }

        viewModel.rangedBeacons.observe(viewLifecycleOwner) { beacons ->
            beaconsAdapter.submitList(beacons)

            binding.beaconsList.isVisible = beacons.isNotEmpty()
            binding.beaconsEmptyMessageLabel.isVisible = beacons.isEmpty()
        }

        viewModel.coffeeBrewerUiState.observe(viewLifecycleOwner) { uiState ->
            binding.coffeeBrewerButton.isVisible = uiState.brewingState != CoffeeBrewingState.SERVED
            binding.coffeeBrewerCancelButton.isVisible = uiState.brewingState != null

            when (uiState.brewingState) {
                null -> {
                    binding.coffeeBrewerButton.setText(R.string.home_coffee_brewer_create_button)
                    binding.coffeeBrewerButton.setOnClickListener {
                        viewModel.createCoffeeSession()
                    }
                }
                CoffeeBrewingState.GRINDING -> {
                    binding.coffeeBrewerButton.setText(R.string.home_coffee_brewer_brew_button)
                    binding.coffeeBrewerButton.setOnClickListener {
                        viewModel.continueCoffeeSession()
                    }
                }
                CoffeeBrewingState.BREWING -> {
                    binding.coffeeBrewerButton.setText(R.string.home_coffee_brewer_serve_button)
                    binding.coffeeBrewerButton.setOnClickListener {
                        viewModel.continueCoffeeSession()
                    }
                }
                CoffeeBrewingState.SERVED -> {}
            }

            binding.coffeeBrewerCancelButton.setOnClickListener {
                viewModel.cancelCoffeeSession()
            }
        }
    }

    private fun onProductClicked(product: Product) {
        findNavController().navigate(
            HomeFragmentDirections.homeToProductDetailsAction(product.id)
        )
    }
}
