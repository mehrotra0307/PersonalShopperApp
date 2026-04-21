package com.ashish.personalshopperagent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ashish.personalshopperagent.ui.components.MarkdownText
import com.ashish.personalshopperagent.ui.theme.AppBlack
import com.ashish.personalshopperagent.ui.theme.Emerald
import com.ashish.personalshopperagent.ui.theme.LineBlack
import com.ashish.personalshopperagent.ui.theme.White
import com.ashish.personalshopperagent.ui.theme.WhiteDim
import com.ashish.personalshopperagent.ui.theme.WhiteMid
import com.ashish.personalshopperagent.viewmodel.ShopperViewModel
import kotlinx.coroutines.delay

@Composable
fun ResultScreen(viewModel: ShopperViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val fullText = state.result ?: ""

    var displayed by remember { mutableStateOf("") }

    LaunchedEffect(fullText) {
        displayed = ""
        if (fullText.isNotEmpty()) {
            // Type out the result — one char per 6ms
            for (i in fullText.indices) {
                delay(6)
                displayed = fullText.substring(0, i + 1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBlack)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Your Recommendation",
                    color = White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    state.query,
                    color = WhiteDim,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            // Source dots
            Row {
                listOf(Emerald) .forEach { color ->
                    // show a live typing indicator while text is streaming
                    if (displayed.length < fullText.length) {
                        Text("●", color = Emerald, fontSize = 10.sp)
                        Spacer(Modifier.width(4.dp))
                        Text("writing...", color = WhiteMid, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        HorizontalDivider(color = LineBlack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp)
        ) {
            item {
                MarkdownText(markdown = displayed)
                Spacer(Modifier.height(48.dp))
                Text(
                    "Google Shopping · Reddit · YouTube  |  Gemini 2.0 Flash",
                    color = WhiteDim,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
