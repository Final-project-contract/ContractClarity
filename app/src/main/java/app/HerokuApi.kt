package app

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface HerokuApi {
    @POST("register")
    suspend fun registerUser(@Body user: User): Response<RegisterResponse>

    @POST("login")
    suspend fun loginUser(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @POST("contracts")
    suspend fun uploadContract(@Header("Authorization") token: String, @Body contract: Contract): Response<ContractResponse>

    @GET("contracts")
    suspend fun getContracts(@Header("Authorization") token: String): Response<List<Contract>>

    @POST("contracts/{id}/summary")
    suspend fun createContractSummary(@Header("Authorization") token: String, @Path("id") contractId: Int, @Body summary: ContractSummary): Response<SummaryResponse>

    @GET("contracts/{id}/summary")
    suspend fun getContractSummary(@Header("Authorization") token: String, @Path("id") contractId: Int): Response<ContractSummary>
}

data class User(val email: String, val password: String, val fullName: String, val industry: String)
data class LoginRequest(val email: String, val password: String)
data class RegisterResponse(val userId: Int)
data class LoginResponse(val token: String)
data class Contract(val id: Int, val name: String, val url: String)
data class ContractResponse(val contractId: Int)
data class ContractSummary(val summaryText: String)
data class SummaryResponse(val summaryId: Int)