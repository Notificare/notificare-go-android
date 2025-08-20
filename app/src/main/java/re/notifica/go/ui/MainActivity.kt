package re.notifica.go.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.go.R
import re.notifica.go.core.DeepLinksService
import re.notifica.go.core.extractConfigurationCode
import re.notifica.go.databinding.ActivityMainBinding
import re.notifica.go.ktx.observeInLifecycle
import re.notifica.go.ktx.parcelableExtra
import re.notifica.models.NotificareNotification
import re.notifica.push.ktx.INTENT_ACTION_ACTION_OPENED
import re.notifica.push.ktx.INTENT_ACTION_NOTIFICATION_OPENED
import re.notifica.push.ktx.push
import re.notifica.push.ui.NotificarePushUI
import re.notifica.push.ui.ktx.pushUI
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NotificarePushUI.NotificationLifecycleListener {
    private val viewModel: MainViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var deepLinksService: DeepLinksService

    private val navController: NavController
        get() = findNavController(R.id.nav_host_fragment)

    override fun onCreate(savedInstanceState: Bundle?) {
        // WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        Notificare.pushUI().addLifecycleListener(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent != null) handleIntent(intent)

        viewModel.navigationFlow.observeInLifecycle(this) { option ->
            when (option) {
                MainViewModel.NavigationOption.SPLASH -> navController.navigate(R.id.global_to_splash_action)
                MainViewModel.NavigationOption.SCANNER -> navController.navigate(R.id.global_to_scanner_action)
                MainViewModel.NavigationOption.INTRO -> navController.navigate(R.id.global_to_intro_action)
                MainViewModel.NavigationOption.MAIN -> navController.navigate(R.id.global_to_main_action)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        Notificare.pushUI().removeLifecycleListener(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        handleIntent(intent)
    }

    override fun onCustomActionReceived(
        notification: NotificareNotification,
        action: NotificareNotification.Action,
        uri: Uri
    ) {
        try {
            startActivity(
                Intent()
                    .setAction(Intent.ACTION_VIEW)
                    .setData(uri)
            )
        } catch (_: Exception) {
            Timber.w("Cannot open custom action link that's not supported by the application.")
        }
    }

    private fun handleIntent(intent: Intent) {
        if (handleConfigurationIntent(intent)) return
        if (Notificare.push().handleTrampolineIntent(intent)) return
        if (Notificare.handleDynamicLinkIntent(this, intent)) return

        when (intent.action) {
            Notificare.INTENT_ACTION_NOTIFICATION_OPENED -> {
                val notification: NotificareNotification = requireNotNull(
                    intent.parcelableExtra(Notificare.INTENT_EXTRA_NOTIFICATION)
                )

                Notificare.pushUI().presentNotification(this, notification)
                return
            }

            Notificare.INTENT_ACTION_ACTION_OPENED -> {
                val notification: NotificareNotification = requireNotNull(
                    intent.parcelableExtra(Notificare.INTENT_EXTRA_NOTIFICATION)
                )

                val action: NotificareNotification.Action = requireNotNull(
                    intent.parcelableExtra(Notificare.INTENT_EXTRA_ACTION)
                )

                Notificare.pushUI().presentAction(this, notification, action)
                return
            }
        }

        val uri = intent.data ?: return
        Timber.d("Received deep link with uri = $uri")

        // Without the CLEAR_TASK flag, NavController would recreate the task by adding CLEAR_TASK flag to the intent and finish the current instance
        if (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0 && intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TASK == 0) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        deepLinksService.deepLinkIntent.tryEmit(intent)
    }

    private fun handleConfigurationIntent(intent: Intent): Boolean {
        val uri = intent.data ?: return false
        val code = extractConfigurationCode(uri) ?: return false

        lifecycleScope.launch {
            try {
                when (viewModel.configure(code)) {
                    MainViewModel.ConfigurationResult.ALREADY_CONFIGURED -> {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle(R.string.main_configured_dialog_title)
                            .setMessage(R.string.main_configured_dialog_message)
                            .setPositiveButton(R.string.dialog_ok_button, null)
                            .show()
                    }

                    MainViewModel.ConfigurationResult.SUCCESS -> {
                        navController.navigate(R.id.splash_fragment)
                    }
                }
            } catch (_: Exception) {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle(R.string.main_configuration_error_dialog_title)
                    .setMessage(R.string.main_configuration_error_dialog_message)
                    .setPositiveButton(R.string.dialog_ok_button, null)
                    .show()
            }
        }

        return true
    }
}
