package com.example.app

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.final_project.R

class SummaryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        val summaryTextView: TextView = findViewById(R.id.summaryTextView)
        summaryTextView.text = intent.getStringExtra("SUMMARY")

        val backButton: ImageButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }
    }
}



//package com.example.app
//
//import android.os.Bundle
//import android.widget.ImageButton
//import android.widget.TextView
//import androidx.appcompat.app.AppCompatActivity
//import com.example.final_project.R
//
//class SummaryActivity : AppCompatActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_summary)
//
//        val contractNameTextView: TextView = findViewById(R.id.contractName)
//        val summaryTextView: TextView = findViewById(R.id.summaryTextView)
//        val backButton: ImageButton = findViewById(R.id.backButton)
//
//        val contractName = intent.getStringExtra("CONTRACT_NAME") ?: "No Contract Name"
//        val summary = intent.getStringExtra("SUMMARY") ?: "No summary available"
//
//        contractNameTextView.text = contractName
//        summaryTextView.text = summary
//
//        backButton.setOnClickListener {
//            finish()
//        }
//    }
//}
//
