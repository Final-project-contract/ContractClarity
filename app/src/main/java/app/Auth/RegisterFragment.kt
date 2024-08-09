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
import com.example.final_project.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class RegisterFragment : Fragment() {

    private lateinit var editTextFullName: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var editTextIndustry: EditText
    private lateinit var buttonRegister: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register, container, false)

        editTextFullName = view.findViewById(R.id.editTextFullName)
        editTextEmail = view.findViewById(R.id.editTextEmail)
        editTextPassword = view.findViewById(R.id.editTextPassword)
        editTextIndustry = view.findViewById(R.id.editTextIndustry)
        buttonRegister = view.findViewById(R.id.buttonRegister)

        buttonRegister.setOnClickListener {
            registerUser()
        }

        return view
    }

    private fun registerUser() {
        val fullName = editTextFullName.text.toString().trim()
        val email = editTextEmail.text.toString().trim()
        val password = editTextPassword.text.toString().trim()
        val industry = editTextIndustry.text.toString().trim()

        if (validateInputs(fullName, email, password, industry)) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val client = HttpClient()
                    val response: HttpResponse = client.post("http://10.0.2.2:8080/register") {
                        contentType(ContentType.Application.Json)
                        setBody("""
                            {
                                "fullName":"$fullName",
                                "email":"$email",
                                "password":"$password",
                                "industry":"$industry"
                            }
                        """.trimIndent())
                    }
                    if (response.status.isSuccess()) {
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "Registration successful", Toast.LENGTH_SHORT).show()
                            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                        }
                    } else {
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "Registration failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                    client.close()
                } catch (e: Exception) {
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun validateInputs(
        fullName: String,
        email: String,
        password: String,
        industry: String
    ): Boolean {
        // Validate full name (required)
        if (fullName.isEmpty()) {
            editTextFullName.error = "Full Name is required"
            return false
        }

        // Validate email (required and valid format)
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        if (email.isEmpty() || !email.matches(emailPattern.toRegex())) {
            editTextEmail.error = "Enter a valid email address"
            return false
        }

        // Validate password (at least 6 characters)
        if (password.length < 6) {
            editTextPassword.error = "Password must be at least 6 characters"
            return false
        }

        // Validate industry (required)
        if (industry.isEmpty()) {
            editTextIndustry.error = "Industry is required"
            return false
        }

        return true
    }
}