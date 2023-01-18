package re.notifica.go.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import re.notifica.Notificare
import re.notifica.go.R
import re.notifica.go.databinding.FragmentHomeBinding
import re.notifica.go.live_activities.models.CoffeeBrewingState
import re.notifica.go.models.Product
import re.notifica.push.ui.ktx.pushUI
import re.notifica.scannables.NotificareScannables
import re.notifica.scannables.NotificareUserCancelledScannableSessionException
import re.notifica.scannables.ktx.scannables
import re.notifica.scannables.models.NotificareScannable

@AndroidEntryPoint
class HomeFragment : Fragment(), NotificareScannables.ScannableSessionListener {
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var binding: FragmentHomeBinding
    private val productsAdapter = ProductsAdapter(::onProductClicked)
    private val beaconsAdapter = BeaconsAdapter()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
        Notificare.scannables().addListener(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.productsList.adapter = productsAdapter
        binding.productsList.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        if (Notificare.scannables().canStartNfcScannableSession) {
            binding.scannablesPrimaryButton.setText(R.string.home_scan_nfc_button)
            binding.scannablesSecondaryButton.isVisible = true
        } else {
            binding.scannablesPrimaryButton.setText(R.string.home_scan_qr_code_button)
            binding.scannablesSecondaryButton.isVisible = false
        }

        binding.beaconsList.adapter = beaconsAdapter
        binding.beaconsList.layoutManager = LinearLayoutManager(requireContext())

        setupListeners()
        setupObservers()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(viewModel)
        Notificare.scannables().removeListener(this)
    }

    private fun setupListeners() {
        binding.productsListButton.setOnClickListener {
            findNavController().navigate(R.id.home_to_products_list_action)
        }

        binding.scannablesPrimaryButton.setOnClickListener {
            Notificare.scannables().startScannableSession(requireActivity())
        }

        binding.scannablesSecondaryButton.setOnClickListener {
            Notificare.scannables().startQrCodeScannableSession(requireActivity())
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

    // region NotificareScannables.ScannableSessionListener

    override fun onScannableDetected(scannable: NotificareScannable) {
        val notification = scannable.notification ?: return
        Notificare.pushUI().presentNotification(requireActivity(), notification)
    }

    override fun onScannableSessionError(error: Exception) {
        if (error is NotificareUserCancelledScannableSessionException) return

        Snackbar.make(binding.root, R.string.home_scan_session_error_message, Snackbar.LENGTH_SHORT).show()
    }

    // endregion
}
