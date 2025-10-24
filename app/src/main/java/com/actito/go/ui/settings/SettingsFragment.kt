package com.actito.go.ui.settings

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.actito.Actito
import com.actito.go.BuildConfig
import com.actito.go.R
import com.actito.go.databinding.FragmentSettingsBinding
import com.actito.inbox.ktx.inbox
import com.actito.models.ActitoDoNotDisturb
import com.actito.models.ActitoTime
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import timber.log.Timber

class SettingsFragment : Fragment() {
    private val pendingRationales = mutableListOf<PermissionType>()

    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var binding: FragmentSettingsBinding

    private val openSettingsNotificationsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (NotificationManagerCompat.from(requireContext().applicationContext).areNotificationsEnabled()) {
            viewModel.changeRemoteNotifications(enabled = true)
            return@registerForActivityResult
        }

        binding.notificationsCard.notificationsSwitch.isChecked = false
    }

    private val openSettingsLocationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.changeLocationUpdates(enabled = true)
    }

    private val notificationsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.changeRemoteNotifications(enabled = true)
            return@registerForActivityResult
        }

        if (shouldOpenSettings(PermissionType.NOTIFICATIONS)) {
            showSettingsPrompt(PermissionType.NOTIFICATIONS)
            return@registerForActivityResult
        }

        binding.notificationsCard.notificationsSwitch.isChecked = false
    }

    private val foregroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.all { it.value }

        if (granted) {
            return@registerForActivityResult enableLocationUpdates()
        }

        if (shouldOpenSettings(PermissionType.LOCATION)) {
            showSettingsPrompt(PermissionType.LOCATION)
            return@registerForActivityResult
        }

        // Enables location updates with whatever capabilities have been granted so far.
        viewModel.changeLocationUpdates(enabled = true)
    }

    private val backgroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            return@registerForActivityResult enableLocationUpdates()
        }

        // Enables location updates with whatever capabilities have been granted so far.
        viewModel.changeLocationUpdates(enabled = true)
    }

    private val bluetoothScanLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            return@registerForActivityResult enableLocationUpdates()
        }

        // Enables location updates with whatever capabilities have been granted so far.
        viewModel.changeLocationUpdates(enabled = true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(viewModel)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val application = Actito.application

        binding.appNameLabel.text = application?.name
        binding.appIdLabel.text = application?.id
        binding.appVersionLabel.text = getString(R.string.settings_application_version, BuildConfig.VERSION_NAME)

        binding.appInformationSection.isVisible = application != null
        binding.appInformationSection.setOnLongClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Application ID", application?.id)
            clipboard.setPrimaryClip(clip)

            Snackbar.make(binding.root, R.string.settings_application_id_copied_message, Snackbar.LENGTH_SHORT).show()

            true
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        setupListeners()
        setupObservers()
    }

    private fun setupListeners() {
        binding.userCard.root.setOnClickListener {
            findNavController().navigate(R.id.settings_to_profile_action)
        }

        binding.inboxCard.root.setOnClickListener {
            findNavController().navigate(R.id.settings_to_inbox_action)
        }

        binding.notificationsCard.notificationsSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked == viewModel.notificationsEnabled.value) return@setOnCheckedChangeListener

            if (checked) {
                enableRemoteNotifications()
            } else {
                viewModel.changeRemoteNotifications(enabled = false)
            }
        }

        binding.dndCard.dndSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked == viewModel.dndEnabled.value) return@setOnCheckedChangeListener
            viewModel.changeDoNotDisturbEnabled(enabled = checked)
        }

        binding.dndCard.dndStartContainer.setOnClickListener {
            val dnd = viewModel.dnd.value ?: return@setOnClickListener

            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(dnd.start.hours)
                .setMinute(dnd.start.minutes)
                .build()

            picker.addOnPositiveButtonClickListener {
                viewModel.changeDoNotDisturb(
                    ActitoDoNotDisturb(
                        start = ActitoTime(picker.hour, picker.minute),
                        end = dnd.end,
                    )
                )
            }

            picker.show(childFragmentManager, "time-picker")
        }

        binding.dndCard.dndEndContainer.setOnClickListener {
            val dnd = viewModel.dnd.value ?: return@setOnClickListener

            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(dnd.end.hours)
                .setMinute(dnd.end.minutes)
                .build()

            picker.addOnPositiveButtonClickListener {
                viewModel.changeDoNotDisturb(
                    ActitoDoNotDisturb(
                        start = dnd.start,
                        end = ActitoTime(picker.hour, picker.minute),
                    )
                )
            }

            picker.show(childFragmentManager, "time-picker")
        }

        binding.locationCard.locationSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked == viewModel.locationUpdatesEnabled.value) return@setOnCheckedChangeListener

            if (checked) {
                enableLocationUpdates()
            } else {
                viewModel.changeLocationUpdates(enabled = false)
            }
        }

        binding.locationCard.viewPolicyButton.setOnClickListener {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .launchUrl(requireContext(), BuildConfig.LOCATION_DATA_PRIVACY_POLICY_URL.toUri())
        }

        binding.tagsCard.announcementsSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked == viewModel.announcementsTopicEnabled.value) return@setOnCheckedChangeListener
            viewModel.changeTopicSubscription(SettingsViewModel.Topic.ANNOUNCEMENTS, checked)
        }

        binding.tagsCard.marketingSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked == viewModel.marketingTopicEnabled.value) return@setOnCheckedChangeListener
            viewModel.changeTopicSubscription(SettingsViewModel.Topic.MARKETING, checked)
        }

        binding.tagsCard.bestPracticesSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked == viewModel.bestPracticesTopicEnabled.value) return@setOnCheckedChangeListener
            viewModel.changeTopicSubscription(SettingsViewModel.Topic.BEST_PRACTICES, checked)
        }

        binding.tagsCard.productUpdatesSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked == viewModel.productUpdatesTopicEnabled.value) return@setOnCheckedChangeListener
            viewModel.changeTopicSubscription(SettingsViewModel.Topic.PRODUCT_UPDATES, checked)
        }

        binding.tagsCard.engineeringSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked == viewModel.engineeringTopicEnabled.value) return@setOnCheckedChangeListener
            viewModel.changeTopicSubscription(SettingsViewModel.Topic.ENGINEERING, checked)
        }

        binding.tagsCard.staffSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked == viewModel.staffTopicEnabled.value) return@setOnCheckedChangeListener
            viewModel.changeTopicSubscription(SettingsViewModel.Topic.STAFF, checked)
        }
    }

    private fun setupObservers() {
        Actito.inbox().observableBadge.observe(viewLifecycleOwner) { badge ->
            binding.inboxCard.badgeLabel.isVisible = badge > 0
            binding.inboxCard.badgeLabel.text = if (badge <= 99) badge.toString() else "99+"
        }

        viewModel.userInfo.observe(viewLifecycleOwner) { userInfo ->
            binding.userCard.idLabel.text = userInfo.id
            binding.userCard.nameLabel.text = userInfo.name ?: getString(R.string.settings_anonymous_user_name)

            Glide.with(this)
                .load(userInfo.pictureUrl)
                .circleCrop()
                .into(binding.userCard.avatarImage)
        }

        viewModel.notificationsEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.notificationsCard.notificationsSwitch.isChecked = enabled
            binding.dndCard.root.isVisible = enabled
        }

        viewModel.dndEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.dndCard.dndSwitch.isChecked = enabled
            binding.dndCard.dndStartContainer.isVisible = enabled
            binding.dndCard.dndEndContainer.isVisible = enabled
        }

        viewModel.dnd.observe(viewLifecycleOwner) { dnd ->
            binding.dndCard.dndStartLabel.text = dnd.start.format()
            binding.dndCard.dndEndLabel.text = dnd.end.format()
        }

        viewModel.locationUpdatesEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.locationCard.locationSwitch.isChecked = enabled
        }

        viewModel.announcementsTopicEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.tagsCard.announcementsSwitch.isChecked = enabled
        }

        viewModel.marketingTopicEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.tagsCard.marketingSwitch.isChecked = enabled
        }

        viewModel.bestPracticesTopicEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.tagsCard.bestPracticesSwitch.isChecked = enabled
        }

        viewModel.productUpdatesTopicEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.tagsCard.productUpdatesSwitch.isChecked = enabled
        }

        viewModel.engineeringTopicEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.tagsCard.engineeringSwitch.isChecked = enabled
        }

        viewModel.staffTopicEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.tagsCard.staffSwitch.isChecked = enabled
        }
    }

    private fun shouldOpenSettings(permissionType: PermissionType): Boolean {
        val permission = when (permissionType) {
            PermissionType.NOTIFICATIONS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.POST_NOTIFICATIONS
                } else {
                    null
                }
            }
            PermissionType.LOCATION -> Manifest.permission.ACCESS_FINE_LOCATION
        }

        if (permission != null) {
            // Rationale did show and permission was denied
            if (!shouldShowRequestPermissionRationale(permission) && pendingRationales.contains(permissionType)) {
                pendingRationales.remove(permissionType)
                return false
            }
            // Rational did show but request was dismissed
            if (shouldShowRequestPermissionRationale(permission) && pendingRationales.contains(permissionType)) {
                pendingRationales.remove(permissionType)
                return false
            }
            // First permission request denied
            if (shouldShowRequestPermissionRationale(permission) && !pendingRationales.contains(permissionType)) {
                return false
            }
        }
        return true
    }

    private fun showSettingsPrompt(permissionType: PermissionType) {
        AlertDialog.Builder(requireContext()).setTitle(R.string.app_name)
            .setMessage(R.string.permission_open_os_settings_rationale)
            .setCancelable(false)
            .setPositiveButton(R.string.dialog_settings_button) { _, _ ->
                Timber.d("Opening OS Settings")
                val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", requireContext().packageName, null)
                }

                when (permissionType) {
                    PermissionType.NOTIFICATIONS -> openSettingsNotificationsLauncher.launch(settingsIntent)
                    PermissionType.LOCATION -> openSettingsLocationLauncher.launch(settingsIntent)
                }
            }
            .setNegativeButton(R.string.dialog_cancel_button) { _, _ ->
                Timber.d("Redirect to OS Settings cancelled")
                when (permissionType) {
                    PermissionType.NOTIFICATIONS -> binding.notificationsCard.notificationsSwitch.isChecked = false
                    PermissionType.LOCATION -> viewModel.changeLocationUpdates(enabled = true)
                }
            }
            .show()
    }

    private fun enableRemoteNotifications() {
        if (!ensureNotificationsPermission()) return
        if (!NotificationManagerCompat.from(requireContext().applicationContext).areNotificationsEnabled()) {
            // Only runs on Android 12 or lower
            if (shouldOpenSettings(PermissionType.NOTIFICATIONS)) {
                showSettingsPrompt(PermissionType.NOTIFICATIONS)
            }
            return
        }

        viewModel.changeRemoteNotifications(enabled = true)
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
                    pendingRationales.add(PermissionType.NOTIFICATIONS)
                    notificationsPermissionLauncher.launch(permission)
                }
                .setNegativeButton(R.string.dialog_cancel_button) { _, _ ->
                    Timber.d("Notifications permission rationale cancelled.")
                    binding.notificationsCard.notificationsSwitch.isChecked = false
                }
                .show()

            return false
        }

        Timber.d("Requesting notifications permission.")
        notificationsPermissionLauncher.launch(permission)

        return false
    }

    private fun enableLocationUpdates() {
        if (!ensureForegroundLocationPermission()) return
        if (!ensureBackgroundLocationPermission()) return
        if (!ensureBluetoothScanPermission()) return

        viewModel.changeLocationUpdates(enabled = true)
    }

    private fun ensureForegroundLocationPermission(): Boolean {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED

        // We already have been granted the requested permission. Move forward...
        if (granted) return true

        if (shouldShowRequestPermissionRationale(permission)) {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.app_name)
                .setMessage(R.string.permission_foreground_location_rationale)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_ok_button) { _, _ ->
                    Timber.d("Requesting foreground location permission.")
                    pendingRationales.add(PermissionType.LOCATION)
                    foregroundLocationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    )
                }
                .setNegativeButton(R.string.dialog_cancel_button) { _, _ ->
                    Timber.d("Foreground location permission rationale cancelled.")

                    // Enables location updates with whatever capabilities have been granted so far.
                    viewModel.changeLocationUpdates(enabled = true)
                }
                .show()

            return false
        }

        Timber.d("Requesting foreground location permission.")
        foregroundLocationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )

        return false
    }

    private fun ensureBackgroundLocationPermission(): Boolean {
        val permission = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> Manifest.permission.ACCESS_BACKGROUND_LOCATION
            else -> Manifest.permission.ACCESS_FINE_LOCATION
        }

        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED

        // We already have been granted the requested permission. Move forward...
        if (granted) return true

        if (shouldShowRequestPermissionRationale(permission)) {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.app_name)
                .setMessage(R.string.permission_background_location_rationale)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_ok_button) { _, _ ->
                    Timber.d("Requesting background location permission.")
                    backgroundLocationPermissionLauncher.launch(permission)
                }
                .setNegativeButton(R.string.dialog_cancel_button) { _, _ ->
                    Timber.d("Background location permission rationale cancelled.")

                    // Enables location updates with whatever capabilities have been granted so far.
                    viewModel.changeLocationUpdates(enabled = true)
                }
                .show()

            return false
        }

        Timber.d("Requesting background location permission.")
        backgroundLocationPermissionLauncher.launch(permission)

        return false
    }

    private fun ensureBluetoothScanPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true

        val permission = Manifest.permission.BLUETOOTH_SCAN
        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED

        // We already have been granted the requested permission. Move forward...
        if (granted) return true

        if (shouldShowRequestPermissionRationale(permission)) {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.app_name)
                .setMessage(R.string.permission_bluetooth_scan_rationale)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_ok_button) { _, _ ->
                    Timber.d("Requesting bluetooth scan permission.")
                    bluetoothScanLocationPermissionLauncher.launch(permission)
                }
                .setNegativeButton(R.string.dialog_cancel_button) { _, _ ->
                    Timber.d("Bluetooth scan permission rationale cancelled.")

                    // Enables location updates with whatever capabilities have been granted so far.
                    viewModel.changeLocationUpdates(enabled = true)
                }
                .show()

            return false
        }

        Timber.d("Requesting bluetooth scan permission.")
        bluetoothScanLocationPermissionLauncher.launch(permission)

        return false
    }

    enum class PermissionType {
        NOTIFICATIONS,
        LOCATION
    }
}
