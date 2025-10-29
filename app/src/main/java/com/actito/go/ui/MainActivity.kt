package com.actito.go.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.actito.Actito
import com.actito.go.R
import com.actito.go.core.DeepLinksService
import com.actito.go.core.extractConfigurationCode
import com.actito.go.databinding.ActivityMainBinding
import com.actito.go.ktx.observeInLifecycle
import com.actito.go.ktx.parcelableExtra
import com.actito.models.ActitoNotification
import com.actito.push.ktx.INTENT_ACTION_ACTION_OPENED
import com.actito.push.ktx.INTENT_ACTION_NOTIFICATION_OPENED
import com.actito.push.ktx.push
import com.actito.push.ui.ActitoPushUI
import com.actito.push.ui.ktx.pushUI
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), ActitoPushUI.NotificationLifecycleListener {
    private val viewModel: MainViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var deepLinksService: DeepLinksService

    private val navController: NavController
        get() = findNavController(R.id.nav_host_fragment)

    override fun onCreate(savedInstanceState: Bundle?) {
        // WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        Actito.pushUI().addLifecycleListener(this)

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

        Actito.pushUI().removeLifecycleListener(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        handleIntent(intent)
    }

    override fun onCustomActionReceived(
        notification: ActitoNotification,
        action: ActitoNotification.Action,
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
        if (Actito.push().handleTrampolineIntent(intent)) return
        if (Actito.handleDynamicLinkIntent(this, intent)) return

        // Handle notification opened
        Actito.push().parseNotificationOpenedIntent(intent)?.also { result ->
            Actito.pushUI().presentNotification(this, result.notification)
            return
        }

        // Handle notification action opened
        Actito.push().parseNotificationActionOpenedIntent(intent)?.also { result ->
            Actito.pushUI().presentAction(this, result.notification, result.action)
            return
        }

        val uri = intent.data ?: return
        Timber.d("Received deep link with uri = $uri")

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
