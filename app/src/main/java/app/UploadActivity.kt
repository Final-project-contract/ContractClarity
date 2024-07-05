package app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import app.firebase.pdfFB
import com.example.final_project.R
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import kotlinx.coroutines.launch
import java.io.InputStream

class UploadActivity : AppCompatActivity() {
    private lateinit var summaryTextView: TextView
    private lateinit var uploadButton: Button
    private lateinit var selectedFileTextView: TextView
    val db = Firebase.firestore

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
            selectedFileTextView.setText("File Selected")
            data?.data?.let { uri ->
                uploadFileToFirebase(uri)
                summarizePdf(uri,this)
            }
        }
    }
    private fun uploadFileToFirebase(fileUri: Uri?) {
        pdfFB.uploadFileToFirebase(fileUri,
            onSuccess = {
                // Handle success
                showToast("File Uploaded Successfully")
            },
            onFailure = { errorMsg ->
                // Handle failure
                showToast(errorMsg)
            }
        )
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    private fun summarizePdf(pdfUri: Uri,context: Context) {
        lifecycleScope.launch {
            try {

                val inputStream = uriToInputStream(context, pdfUri)
                val pdfText:String = extractData(inputStream)
                Log.d("Extracted Text",pdfText)
                val request = createMessageRequest(pdfText)
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

private fun uriToInputStream(context: Context, uri: Uri): InputStream? {
    return context.contentResolver.openInputStream(uri)
}
    private fun createMessageRequest(content: String): MessageRequest {
        // Adjust the message request creation based on your API requirements
        Log.d("Contect",content)
        return MessageRequest(
            model = "claude-3-5-sonnet-20240620",
            max_tokens = 1000,
            temperature = 0.0,
            messages = listOf(
                Message(
                    role = "user",
                    content =listOf(
                        Content(
                            text = "Write a professional concise summary of the next contract to a customer without legal proficiency:$content",
                            type = "text"
                        )
                    )
                )
            )
        )
    }

    companion object {
        private const val FILE_PICKER_REQUEST_CODE = 1
        private val api = RetrofitClient.create()
    }

    private fun extractData(inputStream: InputStream?): String {
        try {
            var extractedText = ""

            // Create PdfReader instance
            val pdfReader = PdfReader(inputStream)

            // Get number of pages in the PDF
            val numberOfPages = pdfReader.numberOfPages

            // Iterate through each page and extract text
            for (i in 1..numberOfPages) {
                // Extract text from page i
                extractedText += PdfTextExtractor.getTextFromPage(pdfReader, i).trim() + "\n"
            }

            // Close PdfReader
            pdfReader.close()

            return extractedText
        } catch (e: Exception) {
            e.printStackTrace()
            return "Error: ${e.message}"
        }
    }
}
