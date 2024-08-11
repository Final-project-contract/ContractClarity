package app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.final_project.R
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import kotlinx.coroutines.launch
import org.jetbrains.annotations.Contract
import java.io.InputStream

class UploadActivity : AppCompatActivity() {
    private lateinit var uploadButton: Button
    private lateinit var selectedFileTextView: TextView
    private lateinit var summaryTextView: TextView
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        uploadButton = findViewById(R.id.uploadButton)
        selectedFileTextView = findViewById(R.id.selectedFileTextView)
        summaryTextView = findViewById(R.id.summaryTextView)
        tokenManager = TokenManager(this)

        uploadButton.setOnClickListener {
            openFilePicker()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/pdf"
        startActivityForResult(Intent.createChooser(intent, "Select a PDF file"), FILE_PICKER_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                selectedFileTextView.text = "File Selected"
                uploadContractToServer(uri)
                summarizePdf(uri)
            }
        }
    }

    private fun uploadContractToServer(fileUri: Uri) {
        lifecycleScope.launch {
            try {
                val token = tokenManager.getToken() ?: throw Exception("No token found")
                val contract = Contract(0, fileUri.lastPathSegment ?: "Unknown", fileUri.toString())
                val response = HerokuRetrofitClient.herokuApi.uploadContract("Bearer $token", contract)
                if (response.isSuccessful) {
                    showToast("Contract uploaded successfully")
                } else {
                    showToast("Failed to upload contract: ${response.message()}")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }

    private fun summarizePdf(pdfUri: Uri) {
        lifecycleScope.launch {
            try {
                val inputStream = contentResolver.openInputStream(pdfUri)
                val pdfText = extractTextFromPdf(inputStream)
                val request = createMessageRequest(pdfText)
                val response = RetrofitClient.create().createMessage(request)
                val summary = response.content.firstOrNull()?.text ?: "No summary available"
                summaryTextView.text = summary
            } catch (e: Exception) {
                Log.e("UploadActivity", "Error summarizing PDF", e)
                showToast("Error summarizing PDF: ${e.message}")
            }
        }
    }

    private fun extractTextFromPdf(inputStream: InputStream?): String {
        return inputStream?.use { stream ->
            val reader = PdfReader(stream)
            val numPages = reader.numberOfPages
            buildString {
                for (i in 1..numPages) {
                    append(PdfTextExtractor.getTextFromPage(reader, i))
                }
            }
        } ?: throw IllegalArgumentException("Cannot read PDF file")
    }

    private fun createMessageRequest(content: String): MessageRequest {
        return MessageRequest(
            model = "claude-3-5-sonnet-20240620",
            max_tokens = 1000,
            temperature = 0.0,
            messages = listOf(
                Message(
                    role = "user",
                    content = listOf(
                        Content(
                            text = "Write a professional concise summary of the next contract to a customer without legal proficiency:$content",
                            type = "text"
                        )
                    )
                )
            )
        )
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val FILE_PICKER_REQUEST_CODE = 1
    }
}