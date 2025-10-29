package com.actito.go.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.actito.go.R
import com.actito.go.core.DeepLinksService
import com.actito.go.databinding.FragmentMainBinding
import com.actito.go.ktx.hideKeyboardOnFocusChange
import com.actito.go.ktx.setOnKeyboardVisibilityChangeListener
import com.actito.go.storage.preferences.ActitoSharedPreferences
import com.google.android.material.elevation.SurfaceColors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainFragment : Fragment() {
    private lateinit var binding: FragmentMainBinding

    @Inject
    lateinit var sharedPreferences: ActitoSharedPreferences

    @Inject
    lateinit var deepLinksService: DeepLinksService

    private val navController: NavController
        get() {
            // Access the nested NavController.
            // Using findNavController will yield a reference to the parent's NavController.
            val fragmentContainer = binding.root.findViewById<View>(R.id.main_nav_host_fragment)
            return fragmentContainer.findNavController()
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().window.navigationBarColor = SurfaceColors.SURFACE_2.getColor(requireActivity())

        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigation) { view, insets ->
            view.setPadding(0, 0, 0, 0)
            insets
        }
        
        // Only shows the cart tab when enabled in the remote config.
        binding.bottomNavigation.menu.findItem(R.id.cart_fragment).isVisible = sharedPreferences.hasStoreEnabled

        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.home_fragment, R.id.cart_fragment, R.id.settings_fragment)
        )

        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        binding.toolbar.setupWithNavController(navController, appBarConfiguration)
        binding.bottomNavigation.setupWithNavController(navController)

        requireActivity().hideKeyboardOnFocusChange { it !is EditText }
        requireActivity().setOnKeyboardVisibilityChangeListener { keyboardShown ->
            binding.bottomNavigation.isGone = keyboardShown
        }

        lifecycleScope.launch {
            deepLinksService.deepLinkIntent.collect { intent ->
                if (intent == null) return@collect

                // Since the navigation component doesn't support placeholders in the scheme, we need to work around that.
                // The graph has static deep links with the production application id and the manifest handles the
                // intent filter explicitly.
                intent.data = intent.data
                    ?.buildUpon()
                    ?.scheme("re.notifica.go")
                    ?.authority("notifica.re")
                    ?.build()

                // Without the CLEAR_TASK flag, NavController would recreate the task by adding CLEAR_TASK flag to the intent and finish the current instance
                if (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0 && intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TASK == 0) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }

                navController.handleDeepLink(intent)
                deepLinksService.deepLinkIntent.emit(null)
            }
        }
    }
}
