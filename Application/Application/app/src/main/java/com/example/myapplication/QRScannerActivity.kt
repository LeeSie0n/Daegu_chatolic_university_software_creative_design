package com.example.myapplication

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.*
import android.text.Layout
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AlignmentSpan
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@androidx.camera.core.ExperimentalGetImage
class QRScannerActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewView: PreviewView
    private var isQrScanned = false

    private val client = OkHttpClient()
    private val serverUrl = "https://TrustQR.i-Keeper.synology.me/predict"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qrscanner)

        previewView = findViewById(R.id.viewFinder)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                showAlertDialog("권한 거부", "카메라 권한이 필요합니다.")
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .setTargetResolution(android.util.Size(1280, 720))
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(android.util.Size(1280, 720))
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        // Add condition to stop analysis
                        if (isQrScanned) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        processImageProxy(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (e: Exception) {
                Log.e(TAG, "카메라 바인딩 실패", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return imageProxy.close()

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val scanner: BarcodeScanner = BarcodeScanning.getClient()

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val qrText = barcode.rawValue?.trim()
                    if (!qrText.isNullOrEmpty()) {
                        isQrScanned = true
                        Log.d(TAG, "QR 코드 인식 성공: $qrText")
                        sendUrlToServer(qrText)
                        break
                    }
                }
            }
            .addOnFailureListener {
                showAlertDialog("QR 코드 오류", "QR 코드를 인식하지 못했습니다.")
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun sendUrlToServer(url: String) {
        val jsonObject = JSONObject().put("url", url)
        val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(serverUrl)
            .post(requestBody)
            .header("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    showAlertDialog("서버 요청 실패", "서버 요청에 실패했습니다.\n오류: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseText = response.body?.string()
                    runOnUiThread {
                        parseAndShowResult(responseText ?: "{}")
                    }
                }
            }
        })
    }

    private fun parseAndShowResult(responseText: String) {
        try {
            val jsonObject = JSONObject(responseText)
            val result = jsonObject.optString("Result", "결과 없음")

            val isMalicious = result.equals("Malicious", ignoreCase = true)
            val message = if (isMalicious) "악성 URL입니다" else "정상 URL입니다"

            showAlertDialog("스캔 결과", message)
        } catch (e: Exception) {
            showAlertDialog("오류", "서버 응답을 해석할 수 없습니다.\n오류: ${e.message}")
        }
    }

    private fun showAlertDialog(title: String, message: String) {
        val centeredTitle = SpannableString(title).apply {
            setSpan(
                AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                0, length,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
        }

        val centeredMessage = SpannableString(message).apply {
            setSpan(
                AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                0, length,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
        }

        AlertDialog.Builder(this)
            .setTitle(centeredTitle)
            .setMessage(centeredMessage)
            .setPositiveButton("확인") { dialog, _ ->
                dialog.dismiss()
                restartCamera()
            }
            .setCancelable(false)
            .show()
    }

    private fun restartCamera() {
        previewView.postDelayed({
            isQrScanned = false
        }, 300)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "QRScannerActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
