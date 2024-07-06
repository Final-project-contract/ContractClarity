package app.Auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.example.final_project.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import android.widget.Toast
class RegisterFragment : Fragment() {

    private lateinit var editTextFullName: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var editTextIndustry: EditText
    private lateinit var buttonRegister: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_register, container, false)

        auth = Firebase.auth

        // Initialize views
        editTextFullName = view.findViewById(R.id.editTextFullName)
        editTextEmail = view.findViewById(R.id.editTextEmail)
        editTextPassword = view.findViewById(R.id.editTextPassword)
        editTextIndustry = view.findViewById(R.id.editTextIndustry)
        buttonRegister = view.findViewById(R.id.buttonRegister)

        // Set click listener for Register button
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
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        // Registration successful
                        val user = auth.currentUser
                        Toast.makeText(context, "Registration successful", Toast.LENGTH_SHORT).show()
                        // TODO: Save additional user information (fullName, industry) to Firestore or Realtime Database
                        // TODO: Navigate to the main screen or login screen
                    } else {
                        // Registration failed
                        Toast.makeText(context, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
//            println("Full Name: $fullName")
//            println("Email: $email")
//            println("Password: $password")
//            println("Industry: $industry")
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
