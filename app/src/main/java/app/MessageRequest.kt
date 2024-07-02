package app

data class MessageRequest(
    val model: String,
    val max_tokens: Int,
    val temperature: Double,
    val messages: List<Message>
)

data class Message(
    val role: String,
    val content: List<Content>
)

data class Content(
    val type: String,
    val text: String
)

data class MessageResponse(
    val content: List<Content>
)
