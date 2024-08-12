package app.Auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.final_project.R

class AppFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_app, container, false)

        // Find buttons by their IDs
        val loginButton = view.findViewById<Button>(R.id.loginButton)
        val registerButton = view.findViewById<Button>(R.id.registerButton)

        // Set click listener for Login button
        loginButton.setOnClickListener {
            // Navigate to Login page (assuming you have a navigation action defined)
            findNavController().navigate(R.id.action_appFragment_to_loginFragment)
        }

        // Set click listener for Register button
        registerButton.setOnClickListener {
            // Navigate to Register page (assuming you have a navigation action defined)
            findNavController().navigate(R.id.action_appFragment_to_registerFragment)
        }

        return view
    }
}
