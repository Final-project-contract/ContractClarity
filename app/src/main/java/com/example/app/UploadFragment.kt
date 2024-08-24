package com.example.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.final_project.R
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.InputStream
class UploadFragment : Fragment() {
    private lateinit var summaryTextView: TextView
    private lateinit var uploadButton: Button
    private lateinit var selectedFileTextView: TextView
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var tokenManager: TokenManager
    private var lastUploadedContractId: Int? = null
    private var selectedFileName: String? = null

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
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar)
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
            data?.data?.let { uri ->
                selectedFileName = getFileNameFromUri(uri)
                showCustomFileNameDialog(uri)
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val displayNameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    return it.getString(displayNameIndex)
                }
            }
        }
        return uri.lastPathSegment ?: "Unknown file"
    }

    private fun showCustomFileNameDialog(fileUri: Uri) {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_custom_file_name, null)

        val fileNameEditText = dialogView.findViewById<EditText>(R.id.fileNameEditText)
        fileNameEditText.setText(selectedFileName)

        builder.setView(dialogView)
            .setPositiveButton("Upload") { _, _ ->
                val fileName = fileNameEditText.text.toString()
                selectedFileName = fileName
                selectedFileTextView.text = "Selected file: $fileName"
                uploadContractToServer(fileUri, fileName)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

        val dialog = builder.create()
        dialog.show()
    }

    private fun uploadContractToServer(fileUri: Uri, fileName: String) {
        lifecycleScope.launch {
            try {
                showLoading(true)

                val token = tokenManager.getToken() ?: throw Exception("No token found")
                Log.d("UploadFragment", "Token retrieved: ${token.take(10)}...")

                val client = HttpClient()

                val inputStream = requireContext().contentResolver.openInputStream(fileUri)
                val fileBytes = inputStream?.readBytes() ?: throw Exception("Cannot read file")
                Log.d("UploadFragment", "File read successfully, size: ${fileBytes.size} bytes")

                val filePath = "/uploads/contracts/$fileName.pdf"

                val response: HttpResponse = client.submitFormWithBinaryData(
                    url = "http://10.0.2.2:8080/contracts",
                    formData = formData {
                        append("file", fileBytes, Headers.build {
                            append(HttpHeaders.ContentType, "application/pdf")
                            append(HttpHeaders.ContentDisposition, "filename=\"$fileName.pdf\"")
                        })
                        append("filePath", filePath)
                    }
                ) {
                    header("Authorization", "Bearer $token")
                }

                Log.d("UploadFragment", "Request sent with file size: ${fileBytes.size}")
                Log.d("UploadFragment", "Upload response status: ${response.status}")
                Log.d("UploadFragment", "Upload response body: ${response.bodyAsText()}")

                if (response.status.isSuccess()) {
                    val responseBody = response.bodyAsText()
                    val contractId = Json.decodeFromString<Map<String, Int>>(responseBody)["contractId"]
                    if (contractId != null) {
                        lastUploadedContractId = contractId
                        showToast("Contract uploaded successfully")
                        selectedFileTextView.text = "Uploaded file: $fileName"
                        summarizePdf(fileUri, requireContext())
                    } else {
                        showToast("Failed to get contract ID")
                        showLoading(false)
                    }
                } else {
                    showToast("Failed to upload contract: ${response.status}")
                    showLoading(false)
                }
                client.close()
            } catch (e: Exception) {
                Log.e("UploadFragment", "Error uploading contract", e)
                showToast("Error: ${e.message}")
                showLoading(false)
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
                    showLoading(false)
                    summaryTextView.visibility = View.VISIBLE
                }

                saveSummaryToServer(summary)
            } catch (e: Exception) {
                e.printStackTrace()
                val errorBody = if (e is retrofit2.HttpException) e.response()?.errorBody()?.string() else null
                activity?.runOnUiThread {
                    summaryTextView.text = "Error: ${e.message}\nError Body: $errorBody"
                    showLoading(false)
                    summaryTextView.visibility = View.VISIBLE
                }
            }
        }
    }

    private suspend fun saveSummaryToServer(summary: String) {
        try {
            val contractId = lastUploadedContractId
            if (contractId == null) {
                showToast("No contract ID available")
                return
            }

            val token = tokenManager.getToken() ?: throw Exception("No token found")
            val client = HttpClient()
            val response: HttpResponse = client.post("http://10.0.2.2:8080/contracts/$contractId/summary") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "summaryText": "$summary"
                    }
                """.trimIndent())
            }
            if (response.status.isSuccess()) {
                showToast("Summary saved successfully")
            } else {
                showToast("Failed to save summary")
            }
            client.close()
        } catch (e: Exception) {
            showToast("Error saving summary: ${e.message}")
        }
    }

    private fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        activity?.runOnUiThread {
            loadingProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            summaryTextView.visibility = if (isLoading) View.GONE else View.VISIBLE
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
                            text = "give me a concise summary of the contract without and introduction and write at the bottom of the output write: IMPORTANT DATES: * list all the important dates in the contract here ONLY if specific dates are specified if not dont write IMPORTANT DATES: at all if there are specified dates write them in this format example: 1-1-1998 payment is due if given a range of dates use the latest date example: if given 1.1.1998-2.2.1999 then the output would be 2-2-1999 end of contract:$content",                            type = "text"
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