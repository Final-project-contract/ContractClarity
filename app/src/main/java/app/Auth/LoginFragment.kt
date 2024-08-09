package app.Auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import app.TokenManager
import com.example.final_project.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.json.JSONObject

class LoginFragment : Fragment() {
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var tokenManager: TokenManager

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
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val client = HttpClient()
                    val response: HttpResponse = client.post("http://10.0.2.2:8080/login") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"email":"$email","password":"$password"}""")
                    }
                    if (response.status.isSuccess()) {
                        val jsonBody = JSONObject(response.bodyAsText())
                        val token = jsonBody.getString("token")
                        tokenManager.saveToken(token)
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
                            findNavController().navigate(R.id.action_loginFragment_to_uploadActivity)
                        }
                    } else {
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "Login failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                    client.close()
                } catch (e: Exception) {
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
        }
    }
}