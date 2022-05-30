package re.notifica.go.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.go.R
import re.notifica.go.core.extractConfigurationCode
import re.notifica.go.databinding.ActivityMainBinding
import re.notifica.go.models.AppConfiguration
import re.notifica.go.network.push.PushService
import re.notifica.go.storage.preferences.NotificareSharedPreferences
import re.notifica.models.NotificareNotification
import re.notifica.push.ktx.INTENT_ACTION_ACTION_OPENED
import re.notifica.push.ktx.INTENT_ACTION_NOTIFICATION_OPENED
import re.notifica.push.ktx.push
import re.notifica.push.ui.ktx.pushUI
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var preferences: NotificareSharedPreferences

    @Inject
    lateinit var pushService: PushService

    override fun onCreate(savedInstanceState: Bundle?) {
        // WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent != null) handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent != null) handleIntent(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }


    private fun handleIntent(intent: Intent) {
        if (handleConfigurationIntent(intent)) return
        if (Notificare.push().handleTrampolineIntent(intent)) return
        if (Notificare.handleDynamicLinkIntent(this, intent)) return

        when (intent.action) {
            Notificare.INTENT_ACTION_NOTIFICATION_OPENED -> {
                val notification: NotificareNotification = requireNotNull(
                    intent.getParcelableExtra(Notificare.INTENT_EXTRA_NOTIFICATION)
                )

                Notificare.pushUI().presentNotification(this, notification)
                return
            }
            Notificare.INTENT_ACTION_ACTION_OPENED -> {
                val notification: NotificareNotification = requireNotNull(
                    intent.getParcelableExtra(Notificare.INTENT_EXTRA_NOTIFICATION)
                )

                val action: NotificareNotification.Action = requireNotNull(
                    intent.getParcelableExtra(Notificare.INTENT_EXTRA_ACTION)
                )

                Notificare.pushUI().presentAction(this, notification, action)
                return
            }
        }

        val uri = intent.data ?: return
        Timber.d("Received deep link with uri = $uri")
    }

    private fun handleConfigurationIntent(intent: Intent): Boolean {
        val uri = intent.data ?: return false
        val code = extractConfigurationCode(uri) ?: return false

        if (preferences.appConfiguration != null) {
            AlertDialog.Builder(this)
                .setTitle(R.string.main_configured_dialog_title)
                .setMessage(R.string.main_configured_dialog_message)
                .setPositiveButton(R.string.dialog_ok_button, null)
                .show()
        } else {
            lifecycleScope.launch {
                try {
                    val configuration = pushService.getConfiguration(code)

                    // Persist the configuration.
                    preferences.appConfiguration = AppConfiguration(
                        applicationKey = configuration.demo.applicationKey,
                        applicationSecret = configuration.demo.applicationSecret,
                    )

                    findNavController(R.id.nav_host_fragment).navigate(R.id.splash_fragment)
                } catch (e: Exception) {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle(R.string.main_configuration_error_dialog_title)
                        .setMessage(R.string.main_configuration_error_dialog_message)
                        .setPositiveButton(R.string.dialog_ok_button, null)
                        .show()
                }
            }
        }

        return true
    }
}
