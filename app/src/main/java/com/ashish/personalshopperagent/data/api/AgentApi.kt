package com.ashish.personalshopperagent.data.api

import com.ashish.personalshopperagent.data.model.AgentResponse
import com.ashish.personalshopperagent.data.model.QueryRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AgentApi {
    @POST("query")
    suspend fun query(@Body request: QueryRequest): AgentResponse
}
