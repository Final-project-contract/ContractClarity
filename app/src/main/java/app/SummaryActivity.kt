package app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.example.final_project.R

class SummaryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        val summaryTextView: TextView = findViewById(R.id.summaryTextView)
        summaryTextView.text = intent.getStringExtra("SUMMARY")
    }
}