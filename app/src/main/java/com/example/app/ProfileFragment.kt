package com.example.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.final_project.R
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileFragment : Fragment() {
    private lateinit var usernameTextView: TextView
    private lateinit var contractsRecyclerView: RecyclerView
    private lateinit var tokenManager: TokenManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        usernameTextView = view.findViewById(R.id.usernameTextView)
        contractsRecyclerView = view.findViewById(R.id.contractsRecyclerView)
        tokenManager = TokenManager(requireContext())

        contractsRecyclerView.layoutManager = LinearLayoutManager(context)

        loadUserProfile()

        return view
    }

    private fun loadUserProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val token = tokenManager.getToken() ?: throw Exception("No token found")
                Log.d("ProfileFragment", "Token: $token")

                val client = HttpClient()

                // Fetch user profile
                val profileResponse: HttpResponse = client.get("http://10.0.2.2:8080/profile") {
                    header("Authorization", "Bearer $token")
                }

                if (profileResponse.status.isSuccess()) {
                    val profileJson = JSONObject(profileResponse.bodyAsText())
                    val username = profileJson.getString("fullName")
                    usernameTextView.text = username
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
                            name = contractObj.getString("name"),
                            contentType = contractObj.getString("contentType"),
                            fileName = ensureFileExtension(contractObj.getString("name"), ".pdf"),
                            uploadTime = if (contractObj.has("uploadTime")) contractObj.getLong("uploadTime") else null
                        ))
                    }
                    contractsRecyclerView.adapter = ContractAdapter(
                        contracts,
                        onContractClick = { contract -> openContract(contract) },
                        onSummaryClick = { contract -> fetchAndShowSummary(contract.id) }
                    )
                } else {
                    throw Exception("Failed to fetch contracts: ${contractsResponse.status}")
                }

                client.close()
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Error loading profile", e)
                activity?.runOnUiThread {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun ensureFileExtension(fileName: String, extension: String): String {
        return if (fileName.toLowerCase().endsWith(extension.toLowerCase())) {
            fileName
        } else {
            "$fileName$extension"
        }
    }

    private fun openContract(contract: Contract) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val token = tokenManager.getToken() ?: throw Exception("No token found")
                val client = HttpClient()

                // Fetch the contract file
                val fileResponse: HttpResponse = client.get("http://10.0.2.2:8080/contracts/${contract.id}/file") {
                    header("Authorization", "Bearer $token")
                }

                if (fileResponse.status.isSuccess()) {
                    val fileBytes = fileResponse.readBytes()

                    // Save the file locally
                    val file = saveFileLocally(contract.fileName, fileBytes)

                    // Open the file
                    openFile(file, contract.contentType)
                } else {
                    throw Exception("Failed to fetch contract file: ${fileResponse.status}")
                }

                client.close()
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Error opening contract", e)
                activity?.runOnUiThread {
                    Toast.makeText(context, "Error opening contract: ${e.message}", Toast.LENGTH_LONG).show()
                }
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
                val token = tokenManager.getToken() ?: throw Exception("No token found")
                val client = HttpClient()

                val summaryResponse: HttpResponse = client.get("http://10.0.2.2:8080/contracts/$contractId/summary") {
                    header("Authorization", "Bearer $token")
                }

                if (summaryResponse.status.isSuccess()) {
                    val summaryJson = JSONObject(summaryResponse.bodyAsText())
                    val summaryText = summaryJson.getString("summaryText")
                    showContractSummary(summaryText)
                } else {
                    throw Exception("Failed to fetch summary: ${summaryResponse.status}")
                }

                client.close()
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Error fetching summary", e)
                activity?.runOnUiThread {
                    Toast.makeText(context, "Error fetching summary: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showContractSummary(summary: String) {
        val intent = Intent(requireContext(), SummaryActivity::class.java)
        intent.putExtra("SUMMARY", summary)
        startActivity(intent)
    }
}

data class Contract(
    val id: Int,
    val name: String,
    val contentType: String,
    val fileName: String,
    val uploadTime: Long? = null
)

class ContractAdapter(
    private val contracts: List<Contract>,
    private val onContractClick: (Contract) -> Unit,
    private val onSummaryClick: (Contract) -> Unit
) : RecyclerView.Adapter<ContractAdapter.ContractViewHolder>() {

    class ContractViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val contractNameTextView: TextView = view.findViewById(R.id.contractNameTextView)
        val contractDateTextView: TextView = view.findViewById(R.id.contractDateTextView)
        val openContractButton: Button = view.findViewById(R.id.openContractButton)
        val openSummaryButton: Button = view.findViewById(R.id.openSummaryButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContractViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contract, parent, false)
        return ContractViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContractViewHolder, position: Int) {
        val contract = contracts[position]
        holder.contractNameTextView.text = contract.name
        holder.contractDateTextView.text = formatDate(contract.uploadTime)

        holder.openContractButton.setOnClickListener {
            onContractClick(contract)
        }

        holder.openSummaryButton.setOnClickListener {
            onSummaryClick(contract)
        }
    }

    override fun getItemCount() = contracts.size

    private fun formatDate(timestamp: Long?): String {
        return if (timestamp != null) {
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        } else {
            "Upload time not available"
        }
    }
}