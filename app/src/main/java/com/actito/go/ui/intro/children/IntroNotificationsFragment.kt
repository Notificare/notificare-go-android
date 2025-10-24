package com.actito.go.ui.intro.children

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.actito.go.R
import com.actito.go.databinding.FragmentIntroNotificationsBinding
import com.actito.go.ui.intro.IntroPage
import com.actito.go.ui.intro.IntroViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class IntroNotificationsFragment : Fragment() {
    private val viewModel: IntroViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private lateinit var binding: FragmentIntroNotificationsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentIntroNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.continueButton.setOnClickListener {
            enableRemoteNotifications()
        }
    }

    private val notificationsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            return@registerForActivityResult viewModel.moveTo(IntroPage.LOCATION)
        }

        viewModel.enableRemoteNotifications()
    }

    private fun enableRemoteNotifications() {
        if (!ensureNotificationsPermission()) return
        viewModel.enableRemoteNotifications()
    }

    private fun ensureNotificationsPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true

        val permission = Manifest.permission.POST_NOTIFICATIONS
        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) return true

        if (shouldShowRequestPermissionRationale(permission)) {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.app_name)
                .setMessage(R.string.permission_notifications_rationale)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    Timber.d("Requesting notifications permission.")
                    notificationsPermissionLauncher.launch(permission)
                }
                .setNegativeButton(R.string.dialog_cancel_button) { _, _ ->
                    Timber.d("Notifications permission rationale cancelled.")
                    viewModel.moveTo(IntroPage.LOCATION)
                }
                .show()

            return false
        }

        Timber.d("Requesting notifications permission.")
        notificationsPermissionLauncher.launch(permission)

        return false
    }
}
