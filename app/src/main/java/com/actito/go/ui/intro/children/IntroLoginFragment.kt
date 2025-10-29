package com.actito.go.ui.intro.children

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.actito.go.BuildConfig
import com.actito.go.R
import com.actito.go.databinding.FragmentIntroLoginBinding
import com.actito.go.ui.intro.IntroViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class IntroLoginFragment : Fragment() {
    private val viewModel: IntroViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private lateinit var binding: FragmentIntroLoginBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentIntroLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.continueButton.setOnClickListener {
            login()
        }
    }


    private fun login(filterAuthorizedAccounts: Boolean = true) {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterAuthorizedAccounts)
            .setServerClientId(BuildConfig.GOOGLE_AUTH_SERVER_ID)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = viewModel.credentialManager.getCredential(requireContext(), request)

                try {
                    viewModel.handleSignInResult(result)
                    findNavController().navigate(R.id.intro_to_main_action)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to handle the login result.")
                    Snackbar.make(binding.root, R.string.intro_login_error_message, Snackbar.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                if (e is GetCredentialCancellationException) {
                    Timber.i("User dismissed the login popup.")
                    return@launch
                }

                if (filterAuthorizedAccounts) {
                    login(filterAuthorizedAccounts = false)
                    return@launch
                }

                Timber.e(e, "Failed to authenticate the user.")
                Snackbar.make(binding.root, R.string.intro_login_error_message, Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}
