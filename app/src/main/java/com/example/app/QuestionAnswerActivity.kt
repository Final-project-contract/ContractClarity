package com.example.app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.final_project.R
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject


class QuestionAnswerActivity : AppCompatActivity() {

    private lateinit var questionEditText: EditText
    private lateinit var sendQuestionButton: Button
    private lateinit var answerTextView: TextView
    private lateinit var tokenManager: TokenManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_answer)

        questionEditText = findViewById(R.id.questionEditText)
        sendQuestionButton = findViewById(R.id.sendQuestionButton)
        answerTextView = findViewById(R.id.answerTextView)
        tokenManager = TokenManager(this)  // Initialize TokenManager

        val contractId = intent.getIntExtra("CONTRACT_ID", -1)

        sendQuestionButton.setOnClickListener {
            val question = questionEditText.text.toString()
            if (question.isNotBlank()) {
                askQuestion(question)
            } else {
                Toast.makeText(this, "Please enter a question", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { prettyPrint = true; isLenient = true; ignoreUnknownKeys = true })
        }
    }

    private fun askQuestion(question: String) {
        val contractId = intent.getIntExtra("CONTRACT_ID", -1)

        if (contractId == -1) {
            Toast.makeText(this, "Contract ID not found", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val token = tokenManager.getToken() ?: throw Exception("No token found")
                val summaryResponse: HttpResponse = client.get("http://10.0.2.2:8080/contracts/$contractId/summary") {
                    header("Authorization", "Bearer $token")
                }

                if (!summaryResponse.status.isSuccess()) {
                    throw Exception("Failed to fetch summary: ${summaryResponse.status}")
                }

                val summaryJson = JSONObject(summaryResponse.bodyAsText())
                val summary = summaryJson.getString("summaryText")

                // Prepare request for the bot API
                val request = ProfileMessageRequest(
                    model = "claude-3-5-sonnet-20240620",
                    max_tokens = 1000,
                    temperature = 0.0,
                    messages = listOf(
                        ProfileMessage(
                            role = "user",
                            content = listOf(
                                ProfileContent(
                                    text = "Here's a summary of a contract: $summary\n\nNow, please answer this question about the contract: $question",
                                    type = "text"
                                )
                            )
                        )
                    )
                )

                // Send question to the bot API
                val response: HttpResponse = client.post("http://10.0.2.2:8080/askQuestion") {
                    header("Authorization", "Bearer $token")
                    contentType(io.ktor.http.ContentType.Application.Json)
                    setBody(request)
                }

                val answer = if (response.status.isSuccess()) {
                    val responseBody = response.bodyAsText()
                    // Assuming the answer is directly in the response body
                    responseBody
                } else {
                    "No answer available"
                }

                answerTextView.text = answer
            } catch (e: Exception) {
                Log.e("QuestionAnswerActivity", "Error asking question", e)
                Toast.makeText(this@QuestionAnswerActivity, "Error asking question: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
