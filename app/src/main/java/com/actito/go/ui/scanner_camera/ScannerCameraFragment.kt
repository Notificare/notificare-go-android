package com.actito.go.ui.scanner_camera

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.actito.go.databinding.FragmentScannerCameraBinding
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

class ScannerCameraFragment : Fragment() {
    private lateinit var binding: FragmentScannerCameraBinding
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalyzer: ImageAnalysis? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentScannerCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val cameraPermissionGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!cameraPermissionGranted) {
            findNavController().popBackStack()
            return
        }
        lifecycleScope.launch {
            startCamera()
        }
    }

    private suspend fun startCamera() {
        val cameraProvider = ProcessCameraProvider
            .getInstance(requireContext())
            .await()
            .also { this.cameraProvider = it }

        // Preview
        val preview = Preview.Builder()
            .build()
            .also { it.surfaceProvider = binding.cameraPreview.surfaceProvider }

        // Image analyzer
        imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .apply {
                setAnalyzer(
                    ContextCompat.getMainExecutor(requireContext()),
                    BarcodeAnalyzer(),
                )
            }

        // Select back camera as a default
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll()

            // Bind use cases to camera
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
        } catch (exc: Exception) {
            exc.printStackTrace()
        }
    }

    private fun stopCamera() {
        cameraProvider?.unbindAll()
    }

    private fun handleBarcode(barcode: String) {
        setFragmentResult("scan_barcode", bundleOf("barcode" to barcode))
        findNavController().popBackStack()
    }

    private inner class BarcodeAnalyzer : ImageAnalysis.Analyzer {
        private val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_PDF417)
                .build()
        )

        @SuppressLint("UnsafeOptInUsageError")
        override fun analyze(proxy: ImageProxy) {
            val image = proxy.image ?: return
            barcodeScanner.process(InputImage.fromMediaImage(image, proxy.imageInfo.rotationDegrees))
                .addOnSuccessListener { barcodes ->
                    val barcode = barcodes.firstOrNull()?.rawValue ?: return@addOnSuccessListener
                    handleBarcode(barcode)

                    barcodeScanner.close()
                    stopCamera()
                }
                .addOnFailureListener { }
                // When the image is from CameraX analysis use case, must call image.close() on received
                // images when finished using them. Otherwise, new images may not be received or the camera
                // may stall.
                .addOnCompleteListener { proxy.close() }
        }
    }
}
