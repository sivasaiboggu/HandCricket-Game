package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.TossCoinFlipper
import com.example.ui.viewmodel.GameViewModel
import com.example.ui.viewmodel.PlayerRole
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.ui.theme.ImmersiveBackground
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.ImmersiveBorder
import com.example.ui.theme.ImmersiveLime
import com.example.ui.theme.ImmersiveTextPrimary
import com.example.ui.theme.ImmersiveTextSecondary
import androidx.compose.foundation.BorderStroke

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinTossScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val isSpinning by viewModel.isCoinSpinning.collectAsState()
    val result by viewModel.tossResult.collectAsState()
    val winner by viewModel.tossWinner.collectAsState()
    val matchState by viewModel.matchState.collectAsState()

    var playerChoice by remember { mutableStateOf<String?>(null) } // "HEADS" or "TAILS"

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(ImmersiveBackground, ImmersiveSurface)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("The Coin Toss", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ImmersiveBackground)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Subtitle
                Text(
                    text = "WIN THE TOSS TO DESIGNATE ROLES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ImmersiveLime,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )

                // Coin visualizer container
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TossCoinFlipper(isSpinning = isSpinning, result = result ?: "")
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (playerChoice == null) {
                        // Let player make their call first before spinning
                        Text(
                            text = "Call heads or tails to flip the coin:",
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    playerChoice = "HEADS"
                                    viewModel.playToss("HEADS")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .testTag("toss_heads_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = ImmersiveSurface, contentColor = Color.White),
                                border = BorderStroke(1.dp, ImmersiveBorder),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("HEADS", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    playerChoice = "TAILS"
                                    viewModel.playToss("TAILS")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .testTag("toss_tails_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = ImmersiveSurface, contentColor = Color.White),
                                border = BorderStroke(1.dp, ImmersiveBorder),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("TAILS", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
                            }
                        }
                    } else if (isSpinning) {
                        Text(
                            text = "Spinning metallic token... Call is $playerChoice",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    } else if (winner != null) {
                        // Coin finished. Result is ready
                        val outcomeStr = if (result.isNotEmpty()) "$result Lands!" else ""
                        Text(
                            text = outcomeStr,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = ImmersiveLime,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        if (winner == "PLAYER") {
                            // Player Won! Choice
                            Text(
                                text = "Congratulations! You won the toss.\nSelect your opening match role:",
                                fontSize = 15.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 20.dp)
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = { viewModel.makeTossDecision(PlayerRole.BATTING) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("choose_batting_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ImmersiveLime,
                                        contentColor = ImmersiveBackground
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("BAT FIRST", fontWeight = FontWeight.Black, fontSize = 14.sp, color = ImmersiveBackground)
                                }

                                Button(
                                    onClick = { viewModel.makeTossDecision(PlayerRole.BOWLING) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("choose_bowling_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4FC3F7),
                                        contentColor = ImmersiveBackground
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("BOWL FIRST", fontWeight = FontWeight.Black, fontSize = 14.sp, color = ImmersiveBackground)
                                }
                            }
                        } else {
                            // AI Won
                            Text(
                                text = if (matchState.commentText.contains("elected")) matchState.commentText else "AI won the toss and gets to decide...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Empty padding space to balances portrait ratios
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
