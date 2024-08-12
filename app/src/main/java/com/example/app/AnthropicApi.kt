package com.example.app

import retrofit2.http.Body
import retrofit2.http.POST

interface AnthropicApi {
    @POST("v1/messages")
    suspend fun createMessage(@Body request: MessageRequest): MessageResponse

}