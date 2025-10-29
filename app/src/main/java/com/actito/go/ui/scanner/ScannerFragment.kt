package com.actito.go.ui.scanner

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.actito.go.R
import com.actito.go.databinding.FragmentScannerBinding
import com.actito.go.ui.MainViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ScannerFragment : Fragment() {
    private val viewModel: ScannerViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()

    private lateinit var binding: FragmentScannerBinding

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) return@registerForActivityResult openCamera()

        Snackbar.make(binding.root, R.string.scanner_camera_permissions_error, Snackbar.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFragmentResultListener("scan_barcode") { _, bundle ->
            val barcode = bundle.getString("barcode") ?: return@setFragmentResultListener

            lifecycleScope.launch {
                try {
                    val configuration = viewModel.fetchConfiguration(barcode)

                    mainViewModel.configure(configuration)
                    mainViewModel.launch()
                } catch (_: Exception) {
                    Snackbar.make(binding.root, R.string.scanner_invalid_qr_code_error, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.openScannerButton.setOnClickListener {
            checkPermissions()
        }
    }

    private fun checkPermissions() {
        val cameraPermissionGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        when {
            cameraPermissionGranted -> {
                // You can use the API that requires the permission.
                openCamera()
            }
            shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA) -> {
                // In an educational UI, explain to the user why your app requires this
                // permission for a specific feature to behave as expected. In this UI,
                // include a "cancel" or "no thanks" button that allows the user to
                // continue using your app without granting the permission.
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.scanner_camera_permission_message)
                    .setPositiveButton(R.string.dialog_ok_button) { _, _ ->
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                    .setNegativeButton(R.string.dialog_cancel_button) { _, _ ->
                        // binding.permissionsContainer.root.isVisible = true
                    }
                    .setCancelable(false)
                    .show()
            }
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        findNavController().navigate(R.id.scanner_to_scanner_camera_action)
    }
}
