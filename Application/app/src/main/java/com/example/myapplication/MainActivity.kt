package com.example.myapplication
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Layout
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AlignmentSpan
import android.util.Log
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ExperimentalGetImage
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream

class MainActivity : AppCompatActivity() {
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private val client = OkHttpClient()
    private val serverUrl = "https://TrustQR.i-Keeper.synology.me/predict"

    @OptIn(ExperimentalGetImage::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val qrScanButton = findViewById<LinearLayout>(R.id.qrScanButton)
        val selectImageButton = findViewById<Button>(R.id.btn_select_qr_image)
        val sendUrlButton = findViewById<Button>(R.id.btnSendUrl)
        val urlInput = findViewById<EditText>(R.id.urlInput)
        val qshingSolutionText = findViewById<TextView>(R.id.qshingSolutionText)

        qshingSolutionText.setOnClickListener {
            val intent = Intent(this, QshingIntroActivity::class.java)
            intent.putExtra("fromMain", true)
            startActivity(intent)
        }

        qrScanButton.setOnClickListener {
            startActivity(Intent(this, QRScannerActivity::class.java))
        }

        selectImageButton.setOnClickListener { pickQRCodeImage() }

        sendUrlButton.setOnClickListener {
            val url = urlInput.text.toString().trim()
            if (url.isNotEmpty()) {
                sendUrlToServer(url)
            } else {
                showAlertDialog("알림", "URL을 입력하세요.")
            }
        }

        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                result.data!!.data?.let { uri ->
                    Log.d("ImageURI", "선택한 이미지 URI: $uri")

                    val inputStream: InputStream? = contentResolver.openInputStream(uri)
                    val bitmap: Bitmap? = BitmapFactory.decodeStream(inputStream)

                    if (bitmap != null) {
                        decodeQRCode(bitmap) { qrText ->
                            if (!qrText.isNullOrEmpty()) {
                                sendUrlToServer(qrText)
                            } else {
                                showAlertDialog("QR 코드 인식 실패", "QR 코드를 인식하지 못했습니다.")
                            }
                        }
                    } else {
                        showAlertDialog("오류", "이미지를 불러오지 못했습니다.")
                    }
                }
            }
        }
    }

    private fun pickQRCodeImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }

    private fun decodeQRCode(bitmap: Bitmap, onResult: (String?) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val scanner: BarcodeScanner = BarcodeScanning.getClient()

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val qrText = barcode.rawValue
                    if (!qrText.isNullOrEmpty()) {
                        onResult(qrText)
                        return@addOnSuccessListener
                    }
                }
                onResult(null)
            }
            .addOnFailureListener {
                onResult(null)
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
                    if (!response.isSuccessful) {
                        runOnUiThread {
                            showAlertDialog("오류", "서버 응답 오류: ${response.code}")
                        }
                        return
                    }

                    val responseData = response.body?.string()
                    runOnUiThread {
                        parseAndShowResult(responseData ?: "{}")
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
        runOnUiThread {
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
                .setPositiveButton("확인") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }
}
