package app.Auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.final_project.R

class MainAppActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // If AppFragment is the start destination, you can navigate here
        // findNavController(R.id.fragmentContainer).navigate(R.id.appFragment)
    }
}
