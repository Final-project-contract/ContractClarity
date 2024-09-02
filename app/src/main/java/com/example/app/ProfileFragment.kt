    package com.example.app
    
    import android.app.AlertDialog
    import android.content.Intent
    import android.os.Bundle
    import android.util.Log
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.Button
    import android.widget.EditText
    import android.widget.ImageView
    import android.widget.ProgressBar
    import android.widget.TextView
    import android.widget.Toast
    import androidx.core.content.FileProvider
    import androidx.fragment.app.Fragment
    import androidx.lifecycle.lifecycleScope
    import androidx.recyclerview.widget.LinearLayoutManager
    import androidx.recyclerview.widget.RecyclerView
    import com.example.final_project.R
    import io.ktor.client.HttpClient
    import io.ktor.client.engine.android.Android
    import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
    import io.ktor.client.request.get
    import io.ktor.client.request.header
    import io.ktor.client.statement.HttpResponse
    import io.ktor.client.statement.bodyAsText
    import io.ktor.client.statement.readBytes
    import io.ktor.http.isSuccess
    import io.ktor.serialization.kotlinx.json.json
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.launch
    import kotlinx.coroutines.withContext
    import kotlinx.serialization.json.Json
    import org.json.JSONArray
    import org.json.JSONObject
    import java.io.File
    import java.io.FileOutputStream
    import java.text.SimpleDateFormat
    import java.util.Date
    import java.util.Locale
    import io.ktor.client.*
    import io.ktor.client.request.*
    import io.ktor.client.statement.*
    import io.ktor.http.*

    class ProfileFragment : Fragment() {
        private var usernameTextView: TextView? = null
        private var contractsRecyclerView: RecyclerView? = null
        private lateinit var tokenManager: TokenManager
        private var loadingProgressBar: ProgressBar? = null
        private var logoutButton: Button? = null
    
        private val client = HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }
    
        private val api = RetrofitClient.create()
    
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.fragment_profile, container, false)
        }
    
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
    
            usernameTextView = view.findViewById(R.id.usernameTextView)
            contractsRecyclerView = view.findViewById(R.id.contractsRecyclerView)
            loadingProgressBar = view.findViewById(R.id.loadingProgressBar)
    
            tokenManager = TokenManager(requireContext())
    
            contractsRecyclerView?.layoutManager = LinearLayoutManager(context)
    
            loadUserProfile()
    
        }

        private fun loadUserProfile() {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    showLoading(true)
                    val token = tokenManager.getToken() ?: throw Exception("No token found")
                    Log.d("ProfileFragment", "Token: $token")

                    // Fetch user profile
                    val profileResponse: HttpResponse = client.get("http://10.0.2.2:8080/profile") {
                        header("Authorization", "Bearer $token")
                    }

                    if (profileResponse.status.isSuccess()) {
                        val profileJson = JSONObject(profileResponse.bodyAsText())
                        val username = profileJson.getString("fullName")
                        usernameTextView?.text = username
                    } else {
                        throw Exception("Failed to fetch profile: ${profileResponse.status}")
                    }

                    // Fetch user's contracts
                    val contractsResponse: HttpResponse = client.get("http://10.0.2.2:8080/contracts") {
                        header("Authorization", "Bearer $token")
                    }

                    if (contractsResponse.status.isSuccess()) {
                        val responseBody = contractsResponse.bodyAsText()
                        Log.d("ProfileFragment", "Contracts response: $responseBody")
                        val contractsJson = JSONArray(responseBody)
                        val contracts = mutableListOf<Contract>()
                        for (i in 0 until contractsJson.length()) {
                            val contractObj = contractsJson.getJSONObject(i)
                            contracts.add(Contract(
                                id = contractObj.getInt("id"),
                                userId = contractObj.getInt("userId"),
                                name = contractObj.getString("name"),
                                filePath = contractObj.getString("filePath"),
                                fileSize = contractObj.getLong("fileSize"),
                                contentType = contractObj.getString("contentType"),
                                uploadTime = contractObj.getLong("uploadTime")
                            ))
                        }
                        contractsRecyclerView?.adapter = ContractAdapter(
                            contracts,
                            onContractClick = { contract -> openContract(contract) },
                            onSummaryClick = { contract -> fetchAndShowSummary(contract.id) },
                            onAskQuestionClick = { contract -> showAskQuestionDialog(contract) },
                            onDeleteClick = { contract -> deleteContract(contract) } // Add this
                        )
                    } else {
                        throw Exception("Failed to fetch contracts: ${contractsResponse.status}")
                    }
                } catch (e: Exception) {
                    Log.e("ProfileFragment", "Error loading profile", e)
                    showToast("Error: ${e.message}")
                } finally {
                    showLoading(false)
                }
            }
        }
        private fun deleteContract(contract: Contract) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    showLoading(true)
                    val token = tokenManager.getToken() ?: throw Exception("No token found")

                    Log.d("ProfileFragment", "Deleting contract ID: ${contract.id}")

                    val deleteResponse: HttpResponse = client.request("http://10.0.2.2:8080/contracts/${contract.id}") {
                        method = HttpMethod.Delete
                        header("Authorization", "Bearer $token")
                    }

                    if (deleteResponse.status.isSuccess()) {
                        showToast("Contract deleted successfully")
                        val adapter = contractsRecyclerView?.adapter as? ContractAdapter
                        val position = adapter?.getContractPosition(contract)
                        if (position != null) {
                            adapter.removeContractAt(position)
                        }
                    } else {
                        throw Exception("Failed to delete contract: ${deleteResponse.status}")
                    }
                } catch (e: Exception) {
                    Log.e("ProfileFragment", "Error deleting contract", e)
                  //  showToast("Error deleting contract: ${e.message}")
                } finally {
                    showLoading(false)
                }
            }
        }



        private fun openContract(contract: Contract) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    showLoading(true)
                    val token = tokenManager.getToken() ?: throw Exception("No token found")
    
                    // Fetch the contract file
                    val fileResponse: HttpResponse = client.get("http://10.0.2.2:8080/contracts/${contract.id}/file") {
                        header("Authorization", "Bearer $token")
                    }

                    if (fileResponse.status.isSuccess()) {
                        val fileBytes = fileResponse.readBytes()
                        val file = saveFileLocally(contract.name, fileBytes)
                        openFile(file, contract.contentType)
                    } else {
                        throw Exception("Failed to fetch contract file: ${fileResponse.status}")
                    }
                } catch (e: Exception) {
                    Log.e("ProfileFragment", "Error opening contract", e)
                    showToast("Error opening contract: ${e.message}")
                } finally {
                    showLoading(false)
                }
            }
        }
    
        private suspend fun saveFileLocally(fileName: String, fileBytes: ByteArray): File = withContext(Dispatchers.IO) {
            val file = File(requireContext().filesDir, fileName)
            FileOutputStream(file).use { it.write(fileBytes) }
            file
        }
    
        private fun openFile(file: File, mimeType: String) {
            val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Open Contract"))
        }

        private fun fetchAndShowSummary(contractId: Int) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    showLoading(true)
                    val token = tokenManager.getToken() ?: throw Exception("No token found")

                    val summaryResponse: HttpResponse = client.get("http://10.0.2.2:8080/contracts/$contractId/summary") {
                        header("Authorization", "Bearer $token")
                    }

                    if (summaryResponse.status.isSuccess()) {
                        val summaryJson = JSONObject(summaryResponse.bodyAsText())
                        Log.d("ProfileFragment", "Summary Response: $summaryJson") // Add this line
                        val summary = summaryJson.optString("summaryText", "No summary available")
                        showContractSummary(summary)
                    } else {
                        throw Exception("Failed to fetch summary: ${summaryResponse.status}")
                    }
                } catch (e: Exception) {
                    Log.e("ProfileFragment", "Error fetching summary", e)
                    showToast("Error fetching summary: ${e.message}")
                } finally {
                    showLoading(false)
                }
            }
        }


        private fun showContractSummary(summary: String) {
            val intent = Intent(requireContext(), SummaryActivity::class.java)
            intent.putExtra("SUMMARY", summary)
            startActivity(intent)
        }


        private fun showAskQuestionDialog(contract: Contract) {
            // Inflate the dialog layout
            val dialogView = layoutInflater.inflate(R.layout.dialog_ask_question, null)
    
            // Initialize the EditText, Buttons, and RecyclerView
            val questionEditText = dialogView.findViewById<EditText>(R.id.questionEditText)
            val sendButton = dialogView.findViewById<Button>(R.id.sendQuestionButton)
            val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
            val recyclerView = dialogView.findViewById<RecyclerView>(R.id.conversationRecyclerView)
    
            // Set up RecyclerView (You need an adapter and layout manager)
            val adapter = ConversationAdapter() // Assume you have a ConversationAdapter
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            recyclerView.adapter = adapter
    
            // Create the dialog
            val dialog = AlertDialog.Builder(requireContext())
                .setTitle("Ask a question about ${contract.name}")
                .setView(dialogView)
                .create()

            // Handle Send button click
            sendButton.setOnClickListener {
                val question = questionEditText.text.toString()
                if (question.isNotBlank()) {
                    // Add the question to RecyclerView
                    adapter.addMessage(question, isUserMessage = true)
                    askQuestionToAnthropicAPI(question, contract.id) // API call
                    questionEditText.text.clear() // Clear input field
                } else {
                    showToast("Please enter a question")
                }
            }
    
            // Handle Cancel button click
            cancelButton.setOnClickListener {
                dialog.dismiss()
            }
    
            dialog.show()
        }
    
    
    
        private fun askQuestionToAnthropicAPI(question: String, contractId: Int) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    showLoading(true)
    
                    val token = tokenManager.getToken() ?: throw Exception("No token found")
                    val summaryResponse: HttpResponse = client.get("http://10.0.2.2:8080/contracts/$contractId/summary") {
                        header("Authorization", "Bearer $token")
                    }
    
                    if (!summaryResponse.status.isSuccess()) {
                        throw Exception("Failed to fetch summary: ${summaryResponse.status}")
                    }
    
                    val summaryJson = JSONObject(summaryResponse.bodyAsText())
                    val summary = summaryJson.getString("summaryText")
    
                    val profileRequest = createProfileMessageRequest(summary, question)
                    val request = adaptProfileRequestToMessageRequest(profileRequest)
                    val response = api.createMessage(request)
                    val answer = response.content.firstOrNull()?.text ?: "No answer available"
    
                    Log.d("ProfileFragment", "Answer: $answer")
                    showAnswerDialog(answer)
                } catch (e: Exception) {
                    Log.e("ProfileFragment", "Error asking question", e)
                    showErrorDialog("Error asking question: ${e.message}")
                } finally {
                    showLoading(false)
                }
            }
        }
    
        private fun adaptProfileRequestToMessageRequest(profileRequest: ProfileMessageRequest): MessageRequest {
            return MessageRequest(
                model = profileRequest.model,
                max_tokens = profileRequest.max_tokens,
                temperature = profileRequest.temperature,
                messages = profileRequest.messages.map { profileMessage ->
                    Message(
                        role = profileMessage.role,
                        content = listOf(Content(
                            type = profileMessage.content.firstOrNull()?.type ?: "",
                            text = profileMessage.content.firstOrNull()?.text ?: ""
                        ))
                    )
                }
            )
        }
    
        private fun createProfileMessageRequest(summary: String, question: String): ProfileMessageRequest {
            return ProfileMessageRequest(
                model = "claude-3-5-sonnet-20240620",
                max_tokens = 1000,
                temperature = 0.0,
                messages = listOf(
                    ProfileMessage(
                        role = "user",
                        content = listOf(
                            ProfileContent(
                                text = "Here's a summary of a contract: $summary\n\nNow, please answer this question about the contract: $question",
                                type = "text"
                            )
                        )
                    )
                )
            )
        }
    
        private fun showLoading(isLoading: Boolean) {
            activity?.runOnUiThread {
                loadingProgressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
    
        private fun showAnswerDialog(answer: String) {
            AlertDialog.Builder(requireContext())
                .setTitle("Answer")
                .setMessage(answer)
                .setPositiveButton("OK", null)
                .show()
        }
    
        private fun showErrorDialog(message: String) {
            AlertDialog.Builder(requireContext())
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
        }
    
        private fun showToast(message: String) {
            activity?.runOnUiThread {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    
        override fun onDestroy() {
            super.onDestroy()
            client.close()
        }
    }
    
    class ContractAdapter(
        private val contracts: MutableList<Contract>,
        private val onContractClick: (Contract) -> Unit,
        private val onSummaryClick: (Contract) -> Unit,
        private val onAskQuestionClick: (Contract) -> Unit,
        private val onDeleteClick: (Contract) -> Unit
    ) : RecyclerView.Adapter<ContractAdapter.ContractViewHolder>() {
    
        class ContractViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val contractNameTextView: TextView = view.findViewById(R.id.contractNameTextView)
            val contractDateTextView: TextView = view.findViewById(R.id.contractDateTextView)
            val openContractButton: Button = view.findViewById(R.id.openContractButton)
            val openSummaryButton: Button = view.findViewById(R.id.openSummaryButton)
            val askQuestionButton: ImageView = view.findViewById(R.id.askQuestionButton)
            val deleteItemButton: ImageView = view.findViewById(R.id.deleteItem)
        }
    
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContractViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_contract, parent, false)
            return ContractViewHolder(view)
        }
        fun getContractPosition(contract: Contract): Int? {
            return contracts.indexOf(contract).takeIf { it >= 0 }
        }
        fun removeContractAt(position: Int) {
            if (position in 0 until contracts.size) {
                contracts.removeAt(position)
                notifyItemRemoved(position)
            }
        }
        override fun onBindViewHolder(holder: ContractViewHolder, position: Int) {
            val contract = contracts[position]
            holder.contractNameTextView.text = contract.name
            holder.contractDateTextView.text = formatDate(contract.uploadTime)
    
            holder.openContractButton.setOnClickListener { onContractClick(contract) }
            holder.openSummaryButton.setOnClickListener { onSummaryClick(contract) }
            holder.askQuestionButton.setOnClickListener { onAskQuestionClick(contract) }
            holder.deleteItemButton.setOnClickListener {
                onDeleteClick(contract)
                contracts.removeAt(position)
                notifyItemRemoved(position)
            }
        }
    
        override fun getItemCount() = contracts.size
    
        private fun formatDate(timestamp: Long): String {
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }
    
    data class Contract(
        val id: Int,
        val userId: Int,
        val name: String,
        val filePath: String,
        val fileSize: Long,
        val contentType: String,
        val uploadTime: Long
    )
    
    data class ProfileMessageRequest(
        val model: String,
        val max_tokens: Int,
        val temperature: Double,
        val messages: List<ProfileMessage>
    )
    
    data class ProfileMessage(
        val role: String,
        val content: List<ProfileContent>
    )
    
    data class ProfileContent(
        val text: String,
        val type: String
    )

//package com.example.app
//
//import android.app.AlertDialog
//import android.content.Intent
//import android.os.Bundle
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Button
//import android.widget.EditText
//import android.widget.ImageView
//import android.widget.ProgressBar
//import android.widget.TextView
//import android.widget.Toast
//import androidx.core.content.FileProvider
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.lifecycleScope
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.example.final_project.R
//import io.ktor.client.HttpClient
//import io.ktor.client.engine.android.Android
//import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
//import io.ktor.client.request.get
//import io.ktor.client.request.header
//import io.ktor.client.statement.HttpResponse
//import io.ktor.client.statement.bodyAsText
//import io.ktor.client.statement.readBytes
//import io.ktor.http.isSuccess
//import io.ktor.serialization.kotlinx.json.json
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import kotlinx.serialization.json.Json
//import org.json.JSONArray
//import org.json.JSONObject
//import java.io.File
//import java.io.FileOutputStream
//import java.text.SimpleDateFormat
//import java.util.Date
//import java.util.Locale
//
//class ProfileFragment : Fragment() {
//    private var usernameTextView: TextView? = null
//    private var contractsRecyclerView: RecyclerView? = null
//    private lateinit var tokenManager: TokenManager
//    private var loadingProgressBar: ProgressBar? = null
//    private var logoutButton: Button? = null
//
//    private val client = HttpClient(Android) {
//        install(ContentNegotiation) {
//            json(Json {
//                prettyPrint = true
//                isLenient = true
//                ignoreUnknownKeys = true
//            })
//        }
//    }
//
//    private val api = RetrofitClient.create()
//
//    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
//        return inflater.inflate(R.layout.fragment_profile, container, false)
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        usernameTextView = view.findViewById(R.id.usernameTextView)
//        contractsRecyclerView = view.findViewById(R.id.contractsRecyclerView)
//        loadingProgressBar = view.findViewById(R.id.loadingProgressBar)
//
//        //  logoutButton = view.findViewById(R.id.logoutButton)
//
//        tokenManager = TokenManager(requireContext())
//
//        contractsRecyclerView?.layoutManager = LinearLayoutManager(context)
//
//        loadUserProfile()
//
//
//    }
//
////    private fun navigateToQuestionAnswerActivity() {
////        val intent = Intent(requireContext(), QuestionAnswerActivity::class.java)
////        startActivity(intent)
////    }
//
//    private fun loadUserProfile() {
//        viewLifecycleOwner.lifecycleScope.launch {
//            try {
//                showLoading(true)
//                val token = tokenManager.getToken() ?: throw Exception("No token found")
//                Log.d("ProfileFragment", "Token: $token")
//
//                // Fetch user profile
//                val profileResponse: HttpResponse = client.get("http://10.0.2.2:8080/profile") {
//                    header("Authorization", "Bearer $token")
//                }
//
//                if (profileResponse.status.isSuccess()) {
//                    val profileJson = JSONObject(profileResponse.bodyAsText())
//                    val username = profileJson.getString("fullName")
//                    usernameTextView?.text = username
//                } else {
//                    throw Exception("Failed to fetch profile: ${profileResponse.status}")
//                }
//
//                // Fetch user's contracts
//                val contractsResponse: HttpResponse = client.get("http://10.0.2.2:8080/contracts") {
//                    header("Authorization", "Bearer $token")
//                }
//
//                if (contractsResponse.status.isSuccess()) {
//                    val responseBody = contractsResponse.bodyAsText()
//                    Log.d("ProfileFragment", "Contracts response: $responseBody")
//                    val contractsJson = JSONArray(responseBody)
//                    val contracts = mutableListOf<Contract>()
//                    for (i in 0 until contractsJson.length()) {
//                        val contractObj = contractsJson.getJSONObject(i)
//                        contracts.add(Contract(
//                            id = contractObj.getInt("id"),
//                            userId = contractObj.getInt("userId"),
//                            name = contractObj.getString("name"),
//                            filePath = contractObj.getString("filePath"),
//                            fileSize = contractObj.getLong("fileSize"),
//                            contentType = contractObj.getString("contentType"),
//                            uploadTime = contractObj.getLong("uploadTime")
//                        ))
//                    }
//                    contractsRecyclerView?.adapter = ContractAdapter(
//                        contracts,
//                        onContractClick = { contract -> openContract(contract) },
//                        onSummaryClick = { contract -> fetchAndShowSummary(contract.id) },
//                        onAskQuestionClick = { contract -> navigateToQuestionAnswerActivity(contract) } // Pass the contract here
//                    )
//                } else {
//                    throw Exception("Failed to fetch contracts: ${contractsResponse.status}")
//                }
//            } catch (e: Exception) {
//                Log.e("ProfileFragment", "Error loading profile", e)
//                showToast("Error: ${e.message}")
//            } finally {
//                showLoading(false)
//            }
//        }
//    }
//
//    private fun openContract(contract: Contract) {
//        viewLifecycleOwner.lifecycleScope.launch {
//            try {
//                showLoading(true)
//                val token = tokenManager.getToken() ?: throw Exception("No token found")
//
//                // Fetch the contract file
//                val fileResponse: HttpResponse = client.get("http://10.0.2.2:8080/contracts/${contract.id}/file") {
//                    header("Authorization", "Bearer $token")
//                }
//
//                if (fileResponse.status.isSuccess()) {
//                    val fileBytes = fileResponse.readBytes()
//                    val file = saveFileLocally(contract.name, fileBytes)
//                    openFile(file, contract.contentType)
//                } else {
//                    throw Exception("Failed to fetch contract file: ${fileResponse.status}")
//                }
//            } catch (e: Exception) {
//                Log.e("ProfileFragment", "Error opening contract", e)
//                showToast("Error opening contract: ${e.message}")
//            } finally {
//                showLoading(false)
//            }
//        }
//    }
//
//    private suspend fun saveFileLocally(fileName: String, fileBytes: ByteArray): File = withContext(Dispatchers.IO) {
//        val file = File(requireContext().filesDir, fileName)
//        FileOutputStream(file).use { it.write(fileBytes) }
//        file
//    }
//
//    private fun openFile(file: File, mimeType: String) {
//        val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
//        val intent = Intent(Intent.ACTION_VIEW).apply {
//            setDataAndType(uri, mimeType)
//            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//        }
//        startActivity(Intent.createChooser(intent, "Open Contract"))
//    }
//
//    private fun fetchAndShowSummary(contractId: Int) {
//        viewLifecycleOwner.lifecycleScope.launch {
//            try {
//                showLoading(true)
//                val token = tokenManager.getToken() ?: throw Exception("No token found")
//
//                val summaryResponse: HttpResponse = client.get("http://10.0.2.2:8080/contracts/$contractId/summary") {
//                    header("Authorization", "Bearer $token")
//                }
//
//                if (summaryResponse.status.isSuccess()) {
//                    val summaryJson = JSONObject(summaryResponse.bodyAsText())
//                    val summary = summaryJson.getString("summaryText")
//                    showContractSummary(summary)
//                } else {
//                    throw Exception("Failed to fetch summary: ${summaryResponse.status}")
//                }
//            } catch (e: Exception) {
//                Log.e("ProfileFragment", "Error fetching summary", e)
//                showToast("Error fetching summary: ${e.message}")
//            } finally {
//                showLoading(false)
//            }
//        }
//    }
//
//    private fun showContractSummary(summary: String) {
//        val intent = Intent(requireContext(), SummaryActivity::class.java)
//        intent.putExtra("SUMMARY", summary)
//        startActivity(intent)
//    }
//
//    private fun showAskQuestionDialog(contract: Contract) {
//        val dialogView = layoutInflater.inflate(R.layout.dialog_ask_question, null)
//        val questionEditText = dialogView.findViewById<EditText>(R.id.questionEditText)
//        val sendButton = dialogView.findViewById<Button>(R.id.sendQuestionButton)
//
//        val dialog = AlertDialog.Builder(requireContext())
//            .setTitle("Ask a question about ${contract.name}")
//            .setView(dialogView)
//            .create()
//
//        sendButton.setOnClickListener {
//            val question = questionEditText.text.toString()
//            if (question.isNotBlank()) {
//                askQuestionToAnthropicAPI(question, contract.id)
//                dialog.dismiss()
//            } else {
//                showToast("Please enter a question")
//            }
//        }
//
//        dialog.show()
//    }
//
//    private fun askQuestionToAnthropicAPI(question: String, contractId: Int) {
//        viewLifecycleOwner.lifecycleScope.launch {
//            try {
//                showLoading(true)
//
//                val token = tokenManager.getToken() ?: throw Exception("No token found")
//                val summaryResponse: HttpResponse = client.get("http://10.0.2.2:8080/contracts/$contractId/summary") {
//                    header("Authorization", "Bearer $token")
//                }
//
//                if (!summaryResponse.status.isSuccess()) {
//                    throw Exception("Failed to fetch summary: ${summaryResponse.status}")
//                }
//
//                val summaryJson = JSONObject(summaryResponse.bodyAsText())
//                val summary = summaryJson.getString("summaryText")
//
//                val profileRequest = createProfileMessageRequest(summary, question)
//                val request = adaptProfileRequestToMessageRequest(profileRequest)
//                val response = api.createMessage(request)
//                val answer = response.content.firstOrNull()?.text ?: "No answer available"
//
//                Log.d("ProfileFragment", "Answer: $answer")
//                showAnswerDialog(answer)
//            } catch (e: Exception) {
//                Log.e("ProfileFragment", "Error asking question", e)
//                showErrorDialog("Error asking question: ${e.message}")
//            } finally {
//                showLoading(false)
//            }
//        }
//    }
//
//    private fun adaptProfileRequestToMessageRequest(profileRequest: ProfileMessageRequest): MessageRequest {
//        return MessageRequest(
//            model = profileRequest.model,
//            max_tokens = profileRequest.max_tokens,
//            temperature = profileRequest.temperature,
//            messages = profileRequest.messages.map { profileMessage ->
//                Message(
//                    role = profileMessage.role,
//                    content = listOf(Content(
//                        type = profileMessage.content.firstOrNull()?.type ?: "",
//                        text = profileMessage.content.firstOrNull()?.text ?: ""
//                    ))
//                )
//            }
//        )
//    }
//
//    private fun createProfileMessageRequest(summary: String, question: String): ProfileMessageRequest {
//        return ProfileMessageRequest(
//            model = "claude-3-5-sonnet-20240620",
//            max_tokens = 1000,
//            temperature = 0.0,
//            messages = listOf(
//                ProfileMessage(
//                    role = "user",
//                    content = listOf(
//                        ProfileContent(
//                            text = "Here's a summary of a contract: $summary\n\nNow, please answer this question about the contract: $question",
//                            type = "text"
//                        )
//                    )
//                )
//            )
//        )
//    }
//
//    private fun showLoading(isLoading: Boolean) {
//        activity?.runOnUiThread {
//            loadingProgressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
//        }
//    }
//
//    private fun showAnswerDialog(answer: String) {
//        AlertDialog.Builder(requireContext())
//            .setTitle("Answer")
//            .setMessage(answer)
//            .setPositiveButton("OK", null)
//            .show()
//    }
//
//    private fun showErrorDialog(message: String) {
//        AlertDialog.Builder(requireContext())
//            .setTitle("Error")
//            .setMessage(message)
//            .setPositiveButton("OK", null)
//            .show()
//    }
//
//    private fun showToast(message: String) {
//        activity?.runOnUiThread {
//            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        client.close()
//    }
//    private fun navigateToQuestionAnswerActivity(contract: Contract) {
//        val intent = Intent(requireContext(), QuestionAnswerActivity::class.java).apply {
//            putExtra("CONTRACT_ID", contract.id) // Pass the contract ID
//        }
//        startActivity(intent)
//    }
//
//
//
//
//}
//
//class ContractAdapter(
//    private val contracts: List<Contract>,
//    private val onContractClick: (Contract) -> Unit,
//    private val onSummaryClick: (Contract) -> Unit,
//    private val onAskQuestionClick: (Contract) -> Unit
//) : RecyclerView.Adapter<ContractAdapter.ContractViewHolder>() {
//
//    class ContractViewHolder(view: View) : RecyclerView.ViewHolder(view) {
//        val contractNameTextView: TextView = view.findViewById(R.id.contractNameTextView)
//        val contractDateTextView: TextView = view.findViewById(R.id.contractDateTextView)
//        val openContractButton: Button = view.findViewById(R.id.openContractButton)
//        val openSummaryButton: Button = view.findViewById(R.id.openSummaryButton)
//        val askQuestionButton: ImageView = view.findViewById(R.id.askQuestionButton)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContractViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.item_contract, parent, false)
//        return ContractViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: ContractViewHolder, position: Int) {
//        val contract = contracts[position]
//        holder.contractNameTextView.text = contract.name
//        holder.contractDateTextView.text = formatDate(contract.uploadTime)
//
//        holder.openContractButton.setOnClickListener { onContractClick(contract) }
//        holder.openSummaryButton.setOnClickListener { onSummaryClick(contract) }
//        holder.askQuestionButton.setOnClickListener { onAskQuestionClick(contract) }
//    }
//
//    override fun getItemCount() = contracts.size
//
//    private fun formatDate(timestamp: Long): String {
//        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
//        return sdf.format(Date(timestamp))
//    }
//}
//
//
//data class Contract(
//    val id: Int,
//    val userId: Int,
//    val name: String,
//    val filePath: String,
//    val fileSize: Long,
//    val contentType: String,
//    val uploadTime: Long
//)
//
//data class ProfileMessageRequest(
//    val model: String,
//    val max_tokens: Int,
//    val temperature: Double,
//    val messages: List<ProfileMessage>
//)
//
//data class ProfileMessage(
//    val role: String,
//    val content: List<ProfileContent>
//)
//
//data class ProfileContent(
//    val text: String,
//    val type: String
//)