package com.ashish.personalshopperagent.data.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class QueryRequest(
    val message: String
)

data class AgentResponse(
    @SerializedName("result") val result: JsonElement? = null
)
