package com.ashish.personalshopperagent.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashish.personalshopperagent.data.api.RetrofitClient
import com.ashish.personalshopperagent.data.model.QueryRequest
import com.google.gson.JsonElement
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AgentStatus { IDLE, SEARCHING, DONE, ERROR }

data class ShopperUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val result: String? = null,
    val error: String? = null,
    val googleStatus: AgentStatus = AgentStatus.IDLE,
    val redditStatus: AgentStatus = AgentStatus.IDLE,
    val youtubeStatus: AgentStatus = AgentStatus.IDLE,
    val synthesizerStatus: AgentStatus = AgentStatus.IDLE
)

class ShopperViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ShopperUiState())
    val uiState: StateFlow<ShopperUiState> = _uiState.asStateFlow()

    fun search(query: String) {
        viewModelScope.launch {
            _uiState.value = ShopperUiState(query = query, isLoading = true)

            launch { runAnimationTimeline() }

            try {
                val response = RetrofitClient.api.query(QueryRequest(query))
                val text = extractText(response.result)

                // Always show synthesizer as active for at least 1 second before showing result
                if (_uiState.value.synthesizerStatus == AgentStatus.IDLE) {
                    delay(7000)
                }
                delay(1000)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        result = text,
                        googleStatus = AgentStatus.DONE,
                        redditStatus = AgentStatus.DONE,
                        youtubeStatus = AgentStatus.DONE,
                        synthesizerStatus = AgentStatus.DONE
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Connection failed. Check your network and try again."
                    )
                }
            }
        }
    }

    private suspend fun runAnimationTimeline() {
        // Google appears first
        delay(400)
        if (_uiState.value.isLoading) _uiState.update { it.copy(googleStatus = AgentStatus.SEARCHING) }

        // Google finishes → Reddit appears
        delay(3800)
        if (_uiState.value.isLoading) _uiState.update {
            it.copy(googleStatus = AgentStatus.DONE, redditStatus = AgentStatus.SEARCHING)
        }

        // Reddit finishes → YouTube appears
        delay(2000)
        if (_uiState.value.isLoading) _uiState.update {
            it.copy(redditStatus = AgentStatus.DONE, youtubeStatus = AgentStatus.SEARCHING)
        }

        // YouTube finishes → Synthesizer appears
        delay(1800)
        if (_uiState.value.isLoading) _uiState.update {
            it.copy(youtubeStatus = AgentStatus.DONE, synthesizerStatus = AgentStatus.SEARCHING)
        }
    }

    private fun extractText(element: JsonElement?): String {
        if (element == null) return "No response received from agent."
        return when {
            element.isJsonPrimitive -> element.asString
            element.isJsonObject -> {
                val obj = element.asJsonObject
                obj.get("response")?.asString
                    ?: obj.get("text")?.asString
                    ?: obj.get("message")?.asString
                    ?: element.toString()
            }
            else -> element.toString()
        }
    }

    fun reset() {
        _uiState.value = ShopperUiState()
    }
}
