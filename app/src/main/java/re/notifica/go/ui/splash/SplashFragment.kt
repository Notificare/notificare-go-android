package re.notifica.go.ui.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import re.notifica.go.R
import re.notifica.go.databinding.FragmentSplashBinding

@AndroidEntryPoint
class SplashFragment : Fragment() {
    private val viewModel: SplashViewModel by viewModels()
    private lateinit var binding: FragmentSplashBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.navigationFlow.collect { option ->
                    when (option) {
                        SplashViewModel.NavigationOption.SCANNER -> findNavController().navigate(R.id.splash_to_scanner_action)
                        SplashViewModel.NavigationOption.INTRO -> findNavController().navigate(R.id.splash_to_intro_action)
                        SplashViewModel.NavigationOption.MAIN -> findNavController().navigate(R.id.splash_to_main_action)
                    }
                }
            }
        }
    }
}
