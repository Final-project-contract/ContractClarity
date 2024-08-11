package app.Auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import app.TokenManager
import com.example.final_project.R
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class LoginFragment : Fragment() {
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var tokenManager: TokenManager


    companion object {
        private const val BASE_URL = "https://contractclarity-e30d2227fa32.herokuapp.com/"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        editTextEmail = view.findViewById(R.id.editTextEmail)
        editTextPassword = view.findViewById(R.id.editTextPassword)
        buttonLogin = view.findViewById(R.id.buttonLogin)
        tokenManager = TokenManager(requireContext())

        buttonLogin.setOnClickListener { loginUser() }

        return view
    }

    private fun loginUser() {
        val email = editTextEmail.text.toString().trim()
        val password = editTextPassword.text.toString().trim()

        if (email.isNotEmpty() && password.isNotEmpty()) {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val client = HttpClient()
                    val response: HttpResponse = client.post("$BASE_URL/login") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"email":"$email","password":"$password"}""")
                    }
                    if (response.status.isSuccess()) {
                        val jsonBody = JSONObject(response.bodyAsText())
                        val token = jsonBody.getString("token")
                        tokenManager.saveToken(token)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
                            findNavController().navigate(R.id.action_loginFragment_to_uploadActivity)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Login failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                    client.close()
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
        }
    }
}