package com.example.app

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
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
import io.ktor.http.isSuccess
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

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

                Log.d("ProfileFragment", "Profile response status: ${profileResponse.status}")
                Log.d("ProfileFragment", "Profile response body: ${profileResponse.bodyAsText()}")

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

                Log.d("ProfileFragment", "Contracts response status: ${contractsResponse.status}")
                Log.d("ProfileFragment", "Contracts response body: ${contractsResponse.bodyAsText()}")

                if (contractsResponse.status.isSuccess()) {
                    val contractsJson = JSONArray(contractsResponse.bodyAsText())
                    val contracts = mutableListOf<Contract>()
                    for (i in 0 until contractsJson.length()) {
                        val contractObj = contractsJson.getJSONObject(i)
                        contracts.add(Contract(
                            id = contractObj.getInt("id"),
                            name = contractObj.getString("name"),
                            url = contractObj.getString("url")
                        ))
                    }
                    contractsRecyclerView.adapter = ContractAdapter(contracts)
                } else {
                    throw Exception("Failed to fetch contracts: ${contractsResponse.status}")
                }

                client.close()
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Error loading profile", e)
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
data class Contract(val id: Int, val name: String, val url: String)

class ContractAdapter(private val contracts: List<Contract>) :
    RecyclerView.Adapter<ContractAdapter.ContractViewHolder>() {

    class ContractViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val contractNameTextView: TextView = view.findViewById(R.id.contractNameTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContractViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contract, parent, false)
        return ContractViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContractViewHolder, position: Int) {
        val contract = contracts[position]
        holder.contractNameTextView.text = contract.name
        // You can set an OnClickListener here for future functionality
    }

    override fun getItemCount() = contracts.size
}