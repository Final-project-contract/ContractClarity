package com.example.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.final_project.R
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.launch
import java.io.InputStream

class UploadFragment : Fragment() {
    private lateinit var summaryTextView: TextView
    private lateinit var uploadButton: Button
    private lateinit var selectedFileTextView: TextView
    private lateinit var tokenManager: TokenManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_upload, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        uploadButton = view.findViewById(R.id.uploadButton)
        selectedFileTextView = view.findViewById(R.id.selectedFileTextView)
        summaryTextView = view.findViewById(R.id.summaryTextView)
        tokenManager = TokenManager(requireContext())

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
            selectedFileTextView.text = "File Selected"
            data?.data?.let { uri ->
                uploadContractToServer(uri)
                summarizePdf(uri, requireContext())
            }
        }
    }

    private fun uploadContractToServer(fileUri: Uri) {
        lifecycleScope.launch {
            try {
                val token = tokenManager.getToken() ?: throw Exception("No token found")
                val client = HttpClient()
                val response: HttpResponse = client.post("http://10.0.2.2:8080/contracts") {
                    header("Authorization", "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody("""
                        {
                            "name": "${fileUri.lastPathSegment}",
                            "url": "${fileUri}"
                        }
                    """.trimIndent())
                }
                if (response.status.isSuccess()) {
                    showToast("Contract uploaded successfully")
                } else {
                    showToast("Failed to upload contract")
                }
                client.close()
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }

    private fun summarizePdf(pdfUri: Uri, context: Context) {
        lifecycleScope.launch {
            try {
                val inputStream = uriToInputStream(context, pdfUri)
                val pdfText: String = extractData(inputStream)
                Log.d("Extracted Text", pdfText)
                val request = createMessageRequest(pdfText)
                val response = api.createMessage(request)
                val summary = response.content.firstOrNull()?.text ?: "No summary available"

                activity?.runOnUiThread {
                    summaryTextView.text = summary
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val errorBody = if (e is retrofit2.HttpException) e.response()?.errorBody()?.string() else null
                activity?.runOnUiThread {
                    summaryTextView.text = "Error: ${e.message}\nError Body: $errorBody"
                }
            }
        }
    }

    private fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun uriToInputStream(context: Context, uri: Uri): InputStream? {
        return context.contentResolver.openInputStream(uri)
    }

    private fun createMessageRequest(content: String): MessageRequest {
        Log.d("Content", content)
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

    private fun extractData(inputStream: InputStream?): String {
        try {
            var extractedText = ""
            val pdfReader = PdfReader(inputStream)
            val numberOfPages = pdfReader.numberOfPages
            for (i in 1..numberOfPages) {
                extractedText += PdfTextExtractor.getTextFromPage(pdfReader, i).trim() + "\n"
            }
            pdfReader.close()
            return extractedText
        } catch (e: Exception) {
            e.printStackTrace()
            return "Error: ${e.message}"
        }
    }

    companion object {
        private const val FILE_PICKER_REQUEST_CODE = 1
        private val api = RetrofitClient.create()
    }
}