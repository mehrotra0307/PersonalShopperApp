package com.ashish.personalshopperagent.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ashish.personalshopperagent.ui.theme.AppBlack
import com.ashish.personalshopperagent.ui.theme.Emerald
import com.ashish.personalshopperagent.ui.theme.ErrorRed
import com.ashish.personalshopperagent.ui.theme.GoogleBlue
import com.ashish.personalshopperagent.ui.theme.LineBlack
import com.ashish.personalshopperagent.ui.theme.RedditOrange
import com.ashish.personalshopperagent.ui.theme.White
import com.ashish.personalshopperagent.ui.theme.WhiteDim
import com.ashish.personalshopperagent.ui.theme.WhiteMid
import com.ashish.personalshopperagent.ui.theme.YouTubeRed
import com.ashish.personalshopperagent.viewmodel.AgentStatus
import com.ashish.personalshopperagent.viewmodel.ShopperViewModel
import kotlinx.coroutines.delay

@Composable
fun SearchingScreen(
    viewModel: ShopperViewModel,
    onResultReady: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.result) {
        if (state.result != null) { delay(400); onResultReady() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBlack)
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = White) }
            Column {
                Text("Searching", color = White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("${state.elapsedSeconds}s", color = WhiteDim, style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("\"${state.query}\"", color = WhiteMid, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(40.dp))
        if (state.googleStatus != AgentStatus.IDLE) {
            AgentLine("Google Shopping", GoogleBlue, state.googleStatus)
        }
        if (state.redditStatus != AgentStatus.IDLE) {
            Spacer(Modifier.height(28.dp))
            AgentLine("Reddit", RedditOrange, state.redditStatus)
        }
        if (state.youtubeStatus != AgentStatus.IDLE) {
            Spacer(Modifier.height(28.dp))
            AgentLine("YouTube Reviews", YouTubeRed, state.youtubeStatus)
        }
        if (state.synthesizerStatus != AgentStatus.IDLE) {
            Spacer(Modifier.height(36.dp))
            HorizontalDivider(color = LineBlack)
            Spacer(Modifier.height(36.dp))
            AgentLine("Analyzing findings", Emerald, state.synthesizerStatus)
        }
        if (state.error != null) {
            Spacer(Modifier.height(32.dp))
            Text("Error: ${state.error}", color = ErrorRed, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AgentLine(name: String, color: Color, status: AgentStatus) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            name,
            color = if (status == AgentStatus.IDLE) WhiteDim else White,
            fontWeight = if (status != AgentStatus.IDLE) FontWeight.Medium else FontWeight.Normal,
            fontSize = 17.sp
        )
        when (status) {
            AgentStatus.IDLE -> Box(Modifier.size(7.dp).clip(CircleShape).background(WhiteDim))
            AgentStatus.SEARCHING -> PulsingDots(color)
            AgentStatus.DONE -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Check, null, tint = Emerald, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("done", color = Emerald, style = MaterialTheme.typography.labelSmall)
            }
            AgentStatus.ERROR -> Text("failed", color = ErrorRed, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun PulsingDots(color: Color) {
    val t = rememberInfiniteTransition(label = "d")
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val a by t.animateFloat(0.2f, 1f,
                infiniteRepeatable(tween(450, delayMillis = i * 150, easing = LinearEasing), RepeatMode.Reverse), "d$i")
            Box(Modifier.size(7.dp).clip(CircleShape).background(color.copy(alpha = a)))
        }
    }
}
