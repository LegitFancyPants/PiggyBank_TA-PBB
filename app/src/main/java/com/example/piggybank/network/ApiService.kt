package com.example.piggybank.network

import com.example.piggybank.model.GoalRequest
import com.example.piggybank.model.SavingsGoal
import com.example.piggybank.model.Transaction
import com.example.piggybank.model.TransactionRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // Transactions Endpoints
    @GET("transactions")
    suspend fun getTransactions(
        @Query("type") type: String? = null
    ): List<Transaction>

    @GET("transactions/{id}")
    suspend fun getTransactionById(
        @Path("id") id: Long
    ): Transaction

    @POST("transactions")
    suspend fun createTransaction(
        @Body request: TransactionRequest
    ): Transaction

    @PUT("transactions/{id}")
    suspend fun updateTransaction(
        @Path("id") id: Long,
        @Body request: TransactionRequest
    ): Transaction

    @DELETE("transactions/{id}")
    suspend fun deleteTransaction(
        @Path("id") id: Long
    )

    // Savings Goals Endpoints
    @GET("goals")
    suspend fun getGoals(): List<SavingsGoal>

    @GET("goals/{id}")
    suspend fun getGoalById(
        @Path("id") id: Long
    ): SavingsGoal

    @POST("goals")
    suspend fun createGoal(
        @Body request: GoalRequest
    ): SavingsGoal

    @PUT("goals/{id}")
    suspend fun updateGoal(
        @Path("id") id: Long,
        @Body request: GoalRequest
    ): SavingsGoal

    @DELETE("goals/{id}")
    suspend fun deleteGoal(
        @Path("id") id: Long
    )
}
