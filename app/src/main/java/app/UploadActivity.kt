package app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.final_project.R
import kotlinx.coroutines.launch
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File

class UploadActivity : AppCompatActivity() {
    private lateinit var summaryTextView: TextView
    private lateinit var uploadButton: Button
    private lateinit var selectedFileTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        uploadButton = findViewById(R.id.uploadButton)
        selectedFileTextView = findViewById(R.id.selectedFileTextView)
        summaryTextView = findViewById(R.id.summaryTextView)
        PDFBoxResourceLoader.init(applicationContext)
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
                val pdfFile = File(uri.path) // Create File object from URI
                selectedFileTextView.text = "Selected File: ${pdfFile.name}"
                summarizePdf(pdfFile)
            }
        }
    }

    private fun summarizePdf(pdfFile: File) {
        lifecycleScope.launch {
            try {
               // val pdfText = extractData(pdfFile)
               // Log.d(pdfFile.toString())
                val request = createMessageRequest(pdfFile.toString())
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

//    private suspend fun extractPdfText(uri: Uri): String = withContext(Dispatchers.IO) {
//        val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
//        val document = PDDocument.load(parcelFileDescriptor)
//        val stripper = PDFTextStripper()
//        val pdfText = stripper.getText(document)
//        document.close()
//        parcelFileDescriptor?.close()
//        return@withContext pdfText
//    }

    private fun createMessageRequest(content: String): MessageRequest {
        // Adjust the message request creation based on your API requirements
        return MessageRequest(
            model = "claude-3-5-sonnet-20240620",
            max_tokens = 1000,
            temperature = 0.0,
            messages = listOf(
                Message(
                    role = "user",
                    content = listOf(
                        Content(type = "text", text = content)
                    )
                )
            )
        )
    }

    companion object {
        private const val FILE_PICKER_REQUEST_CODE = 1
        private val api = RetrofitClient.create()
    }


    private fun readPdfContent(uri: Uri): String {
        val inputStream = contentResolver.openInputStream(uri)
        val document = PDDocument.load(inputStream)
        val pdfStripper = PDFTextStripper()
        val text = pdfStripper.getText(document)
        document.close()
        return text
    }
//    private fun extractData(file: File): String {
//        try {
//            var extractedText = ""
//
//            // Create PdfReader instance
//            val pdfReader = PdfReader(file)
//
//            // Get number of pages in the PDF
//            val numberOfPages = pdfReader.numberOfPages
//
//            // Iterate through each page and extract text
//            for (i in 1..numberOfPages) {
//                // Extract text from page i
//                extractedText += PdfTextExtractor.getTextFromPage(pdfReader, i).trim() + "\n"
//            }
//
//            // Close PdfReader
//            pdfReader.close()
//
//            return extractedText
//        } catch (e: Exception) {
//            e.printStackTrace()
//            return "Error: ${e.message}"
//        }
//    }
}
