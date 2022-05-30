package re.notifica.go.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.elevation.SurfaceColors
import dagger.hilt.android.AndroidEntryPoint
import re.notifica.go.R
import re.notifica.go.databinding.FragmentMainBinding
import re.notifica.go.ktx.hideKeyboardOnFocusChange
import re.notifica.go.ktx.setOnKeyboardVisibilityChangeListener

@AndroidEntryPoint
class MainFragment : Fragment() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: FragmentMainBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().window.navigationBarColor = SurfaceColors.SURFACE_2.getColor(requireActivity())

        // Access the nested NavController.
        // Using findNavController will yield a reference to the parent's NavController.
        val fragmentContainer = view.findViewById<View>(R.id.main_nav_host_fragment)
        val navController = Navigation.findNavController(fragmentContainer)

        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.home_fragment, R.id.cart_fragment, R.id.settings_fragment)
        )

        binding.toolbar.setupWithNavController(navController, appBarConfiguration)
        binding.bottomNavigation.setupWithNavController(navController)

        requireActivity().hideKeyboardOnFocusChange { it !is EditText }
        requireActivity().setOnKeyboardVisibilityChangeListener { keyboardShown ->
            binding.bottomNavigation.isGone = keyboardShown
        }

        viewModel.storeEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.bottomNavigation.menu.findItem(R.id.cart_fragment).isVisible = enabled
        }
    }
}
