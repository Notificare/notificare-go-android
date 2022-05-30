package re.notifica.go.ui.intro.children

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import re.notifica.go.BuildConfig
import re.notifica.go.R
import re.notifica.go.databinding.FragmentIntroLoginBinding
import re.notifica.go.ui.intro.IntroViewModel
import timber.log.Timber

@AndroidEntryPoint
class IntroLoginFragment : Fragment() {
    private val viewModel: IntroViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private lateinit var binding: FragmentIntroLoginBinding

    private val loginRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        lifecycleScope.launch {
            try {
                viewModel.handleLoginResult(result)
                findNavController().navigate(R.id.intro_to_main_action)
            } catch (e: Exception) {
                Timber.e(e, "Failed to handle the login result.")
                Snackbar.make(binding.root, R.string.intro_login_error_message, Snackbar.LENGTH_SHORT).show()
            }
        }
    }


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
        val request = BeginSignInRequest.Builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.Builder()
                    .setSupported(true)
                    .setServerClientId(BuildConfig.GOOGLE_AUTH_SERVER_ID)
                    .setFilterByAuthorizedAccounts(filterAuthorizedAccounts)
                    .build()
            )
            .build()

        lifecycleScope.launch {
            try {
                val result = viewModel.loginClient.beginSignIn(request).await()
                loginRequestLauncher.launch(
                    IntentSenderRequest.Builder(result.pendingIntent.intentSender)
                        .build()
                )
            } catch (e: Exception) {
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
