package com.example.final_project

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import app.Content
import app.Message
import app.MessageRequest
import app.RetrofitClient
import app.SummaryActivity
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.io.BufferedReader
import java.io.InputStreamReader

class UploadActivity : AppCompatActivity() {
    private lateinit var summaryTextView: TextView

    private lateinit var uploadButton: Button
    private lateinit var selectedFileTextView: TextView
    private val api = RetrofitClient.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        uploadButton = findViewById(R.id.uploadButton)
        selectedFileTextView = findViewById(R.id.selectedFileTextView)
        summaryTextView = findViewById(R.id.summaryTextView)

        uploadButton.setOnClickListener {
            openFilePicker()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/pdf"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(Intent.createChooser(intent, "Select a PDF file"), FILE_PICKER_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                selectedFileTextView.text = "Selected File: ${uri.path}"
                summarizePdf(uri)
            }
        }
    }
    private fun summarizePdf(uri: Uri) {
        lifecycleScope.launch {
            try {
                val pdfText = readPdfContent(uri)
                val maxLength = 4000 // Adjust as needed
                val truncatedText = if (pdfText.length > maxLength) pdfText.substring(0, maxLength) else pdfText

                val request = MessageRequest(
                    model = "claude-3-5-sonnet-20240620",
                    max_tokens = 1000,
                    temperature = 0.0,
                    messages = listOf(
                        Message(
                            role = "user",
                            content = listOf(Content(type = "text", text = "Summarize this PDF content: $truncatedText"))
                        )
                    )
                )

                println("Request: $request")

                val response = api.createMessage(request)
                val summary = response.content.firstOrNull()?.text ?: "No summary available"

                runOnUiThread {
                    summaryTextView.text = summary
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val errorBody = if (e is retrofit2.HttpException) e.response()?.errorBody()?.string() else null
                runOnUiThread {
                    summaryTextView.text = "Error: ${e.message}\nError Body: $errorBody"
                }
            }
        }

    }

    private fun readPdfContent(uri: Uri): String {
        val inputStream = contentResolver.openInputStream(uri)
        val reader = BufferedReader(InputStreamReader(inputStream))
        return reader.use { it.readText() }
    }

    companion object {
        private const val FILE_PICKER_REQUEST_CODE = 1
    }
}
