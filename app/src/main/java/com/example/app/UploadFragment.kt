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
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.final_project.R
import com.example.server.CalendarEventDao
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
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
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
<<<<<<< HEAD
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan

=======
>>>>>>> e35c2ddb821733de65430bad6afbd1de6a39cafd

class UploadFragment : Fragment() {
    private lateinit var summaryTextView: TextView
    private lateinit var uploadButton: ImageView
    private lateinit var selectedFileTextView: TextView
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var tokenManager: TokenManager
    private var lastUploadedContractId: Int? = null
    private var selectedFileName: String? = null
    private val calendarEventDao = CalendarEventDao()

    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_upload, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        uploadButton = view.findViewById(R.id.uploadIcon)
        selectedFileTextView = view.findViewById(R.id.selectedFileTextView)
        summaryTextView = view.findViewById(R.id.summaryTextView)
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar)
        tokenManager = TokenManager(requireContext())

        uploadButton.setOnClickListener {
            openFilePicker()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/pdf"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
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
                val fileName = fileNameEditText.text.toString().trim()
                if (fileName.isNotEmpty()) {
                    selectedFileName = fileName
                    selectedFileTextView.text = "Selected file: $fileName"
                    uploadContractToServer(fileUri, fileName)
                } else {
                    Toast.makeText(requireContext(), "File name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        builder.create().show()
    }
//    private fun showCustomFileNameDialog(fileUri: Uri) {
//        val builder = AlertDialog.Builder(requireContext())
//        val inflater = requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
//        val dialogView = inflater.inflate(R.layout.dialog_custom_file_name, null)
//
//        val fileNameEditText = dialogView.findViewById<EditText>(R.id.fileNameEditText)
//        fileNameEditText.setText(selectedFileName)
//
//        builder.setView(dialogView)
//            .setPositiveButton("Upload") { _, _ ->
//                val fileName = fileNameEditText.text.toString()
//                selectedFileName = fileName
//                selectedFileTextView.text = "Selected file: $fileName"
//                uploadContractToServer(fileUri, fileName)
//            }
//            .setNegativeButton("Cancel") { dialog, _ ->
//                dialog.dismiss()
//            }
//
//        val dialog = builder.create()
//        dialog.show()
//    }

    private fun uploadContractToServer(fileUri: Uri, fileName: String) {
        lifecycleScope.launch {
            try {
                showLoading(true)

                val token = tokenManager.getToken() ?: throw Exception("No token found")
                Log.d("UploadFragment", "Token retrieved: ${token.take(10)}...")

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
                    val contractId = json.decodeFromString<Map<String, Int>>(responseBody)["contractId"]
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
            } catch (e: Exception) {
                Log.e("UploadFragment", "Error uploading contract", e)
                showToast("Error: ${e.message}")
                showLoading(false)
            }
        }
    }

    private fun formatSummaryText(summary: String): SpannableString {
        val spannableString = SpannableString(summary)

        // Define the color for IMPORTANT DATES
        val importantDatesColor = ContextCompat.getColor(requireContext(), R.color.navy_blue)

        // Find the start and end indices of IMPORTANT DATES
        val importantDatesStart = summary.indexOf("IMPORTANT DATES:")
        if (importantDatesStart != -1) {
            val span = ForegroundColorSpan(importantDatesColor)
            spannableString.setSpan(span, importantDatesStart, summary.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        return spannableString
    }

    private fun summarizePdf(pdfUri: Uri, context: Context) {
        lifecycleScope.launch {
            try {
                val inputStream = uriToInputStream(context, pdfUri)
                val pdfText: String = extractData(inputStream)
                val request = createMessageRequest(pdfText)
                val response = api.createMessage(request)
                val summary = response.content.firstOrNull()?.text ?: "No summary available"

                // Format the summary to highlight IMPORTANT DATES
                val formattedSummary = formatSummaryText(summary)

                activity?.runOnUiThread {
                    summaryTextView.text = formattedSummary
                    showLoading(false)
                    summaryTextView.visibility = View.VISIBLE
                }

                saveSummaryToServer(summary)
                createCalendarEventsFromSummary(summary, lastUploadedContractId)
            } catch (e: Exception) {
                Log.e("UploadFragment", "Error in summarizePdf", e)
                showToast("Error summarizing PDF: ${e.message}")
                showLoading(false)
            }
        }
    }


    private suspend fun createCalendarEventsFromSummary(summary: String, contractId: Int?) {
        Log.d("UploadFragment", "Creating calendar events from summary")
        contractId?.let { id ->
            val importantDates = extractImportantDates(summary)
            Log.d("UploadFragment", "Extracted important dates: $importantDates")

            val userId = getCurrentUserId()
            if (userId == null) {
                Log.e("UploadFragment", "Failed to get current user ID")
                showToast("Failed to create calendar events: User ID not found")
                return
            }

            var successCount = 0
            var failureCount = 0

            for ((date, eventTitle) in importantDates) {
                Log.d("UploadFragment", "Attempting to create calendar event: $eventTitle on ${Date(date)}")
                try {
                    val token = tokenManager.getToken() ?: throw Exception("No token found")
                    val jsonBody = JSONObject().apply {
                        put("contractId", id)
                        put("title", eventTitle)
                        put("date", date)
                    }
                    val response: HttpResponse = client.post("http://10.0.2.2:8080/calendar-events") {
                        header("Authorization", "Bearer $token")
                        contentType(ContentType.Application.Json)
                        setBody(jsonBody.toString())
                    }
                    if (response.status.isSuccess()) {
                        Log.d("UploadFragment", "Created calendar event: $eventTitle on ${Date(date)}")
                        successCount++
                    } else {
                        Log.e("UploadFragment", "Failed to create calendar event: ${response.status}")
                        failureCount++
                    }
                } catch (e: Exception) {
                    Log.e("UploadFragment", "Error creating calendar event: ${e.message}", e)
                    failureCount++
                }
            }

            val message = if (failureCount == 0) {
                "Successfully created $successCount calendar events."
            } else {
                "Created $successCount calendar events. Failed to create $failureCount events."
            }
            showToast(message)
        }
    }

    private fun extractImportantDates(summary: String): List<Pair<Long, String>> {
        val importantDates = mutableListOf<Pair<Long, String>>()
        val importantDatesIndex = summary.indexOf("IMPORTANT DATES:")
        if (importantDatesIndex != -1) {
            val importantDatesText = summary.substring(importantDatesIndex + "IMPORTANT DATES:".length).trim()
            val dateLines = importantDatesText.split("\n")

            for (line in dateLines) {
                val parts = line.trim().split(" ", limit = 2)
                if (parts.size == 2) {
                    val date = parseDate(parts[0])
                    if (date != null) {
                        importantDates.add(Pair(date, parts[1]))
                    } else {
                        Log.e("UploadFragment", "Failed to parse date: ${parts[0]}")
                    }
                } else {
                    Log.e("UploadFragment", "Invalid date line format: $line")
                }
            }
        } else {
            Log.w("UploadFragment", "No IMPORTANT DATES section found in summary")
        }
        return importantDates
    }

    private fun parseDate(dateString: String): Long? {
        val formatters = listOf(
            SimpleDateFormat("M-d-yyyy", Locale.US),
            SimpleDateFormat("MM-dd-yyyy", Locale.US),
            SimpleDateFormat("yyyy-MM-dd", Locale.US)
        )

        for (formatter in formatters) {
            try {
                formatter.timeZone = TimeZone.getTimeZone("UTC")  // Ensure UTC timezone
                val date = formatter.parse(dateString)
                if (date != null) {
                    Log.d("UploadFragment", "Successfully parsed date: $dateString to ${date.time}")
                    return date.time
                }
            } catch (e: Exception) {
                // Continue to next formatter
            }
        }

        Log.e("UploadFragment", "Failed to parse date: $dateString")
        return null
    }

    private suspend fun saveSummaryToServer(summary: String) {
        try {
            val contractId = lastUploadedContractId
            if (contractId == null) {
                showToast("No contract ID available")
                return
            }

            val token = tokenManager.getToken() ?: throw Exception("No token found")
            val response: HttpResponse = client.post("http://10.0.2.2:8080/contracts/$contractId/summary") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(mapOf("summaryText" to summary))
            }
            if (response.status.isSuccess()) {
                showToast("Summary saved successfully")
            } else {
                showToast("Failed to save summary")
            }
        } catch (e: Exception) {
            showToast("Error saving summary: ${e.message}")
        }
    }

    private fun getCurrentUserId(): Int? {
        val token = tokenManager.getToken() ?: return null
        return try {
            val parts = token.split(".")
            if (parts.size == 3) {
                val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE))
                val jsonObject = org.json.JSONObject(payload)
                jsonObject.getInt("userId")
            } else {
                Log.e("UploadFragment", "Invalid token format")
                null
            }
        } catch (e: Exception) {
            Log.e("UploadFragment", "Error extracting user ID from token: ${e.message}")
            null
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
        Log.d("UploadFragment", "Creating message request with content length: ${content.length}")
        return MessageRequest(
            model = "claude-3-5-sonnet-20240620",
            max_tokens = 1000,
            temperature = 0.0,
            messages = listOf(
                Message(
                    role = "user",
                    content = listOf(
                        Content(
                            text = "give me a concise summary of the contract without and introduction and write at the bottom of the output write: IMPORTANT DATES: * list all the important dates in the contract here ONLY if specific dates are specified if not dont write IMPORTANT DATES: at all if there are specified dates write them in this format example: 1-1-1998 payment is due if given a range of dates use the latest date example: if given 1.1.1998-2.2.1999 then the output would be 2-2-1999 end of contract:$content",
                            type = "text"
                        )
                    )
                )
            )
        )
    }
    private fun extractData(inputStream: InputStream?): String {
        if (inputStream == null) return "Error: Input stream is null"
        return try {
            val pdfReader = PdfReader(inputStream)
            val extractedText = StringBuilder()
            val numberOfPages = pdfReader.numberOfPages
            for (i in 1..numberOfPages) {
                extractedText.append(PdfTextExtractor.getTextFromPage(pdfReader, i).trim()).append("\n")
            }
            pdfReader.close()
            extractedText.toString()
        } catch (e: Exception) {
            Log.e("UploadFragment", "Error extracting PDF text", e)
            "Error: ${e.message}"
        }
    }
<<<<<<< HEAD
//    private fun extractData(inputStream: InputStream?): String {
//        try {
//            var extractedText = ""
//            val pdfReader = PdfReader(inputStream)
//            val numberOfPages = pdfReader.numberOfPages
//            for (i in 1..numberOfPages) {
//                extractedText += PdfTextExtractor.getTextFromPage(pdfReader, i).trim() + "\n"
//            }
//            pdfReader.close()
//            return extractedText
//        } catch (e: Exception) {
//            Log.e("UploadFragment", "Error extracting PDF text", e)
//            return "Error: ${e.message}"
//        }
//    }
=======
>>>>>>> e35c2ddb821733de65430bad6afbd1de6a39cafd

    companion object {
        private const val FILE_PICKER_REQUEST_CODE = 1
        private val api = RetrofitClient.create()
    }
}