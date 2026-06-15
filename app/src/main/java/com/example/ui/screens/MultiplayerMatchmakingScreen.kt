package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ImmersiveBackground
import com.example.ui.theme.ImmersiveBorder
import com.example.ui.theme.ImmersiveLime
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.viewmodel.GameViewModel
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.Difficulty
import com.example.ui.viewmodel.StadiumTheme
import com.example.data.MultiplayerStatus
import androidx.compose.foundation.BorderStroke

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiplayerMatchmakingScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.matchState.collectAsState()
    val status = state.multiplayerStatus

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(ImmersiveBackground, ImmersiveSurface)
    )

    // Animated radar ripples for matchmaking phase
    val infiniteTransition = rememberInfiniteTransition(label = "MatchmakingRadar")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Pulse"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Alpha"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Multiplayer Pitch", fontWeight = FontWeight.Bold, color = Color.White) },
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
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            when (status) {
                MultiplayerStatus.UNINITIALIZED -> {
                    // Firebase has not been configured by the user yet.
                    // Show helpful manual setup steps.
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ImmersiveSurface),
                        border = BorderStroke(1.dp, ImmersiveBorder),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(Color(0xFFEF4444).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = "Cloud Offline",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Text(
                                text = "Firebase Offline / Uninitialized",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "To enable real-time multiplayer, complete these quick manual steps:",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SetupStepItem("1", "Go to Firebase Console & create a project named 'HandCricket'")
                                SetupStepItem("2", "Register Android app Package: com.sivasaiboggu.handcricket")
                                SetupStepItem("3", "Download google-services.json and paste it into the app/ folder, overwriting the placeholder.")
                                SetupStepItem("4", "Enable Realtime Database and Anonymous Auth inside your console dashboard.")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Play single player fallback
                            Button(
                                onClick = {
                                    viewModel.configureMatch(
                                        overs = 1,
                                        wickets = 1,
                                        difficulty = Difficulty.MEDIUM,
                                        theme = StadiumTheme.CLASSIC_TURF
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ImmersiveLime,
                                    contentColor = ImmersiveBackground
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("PLAY OFFLINE VS AI", fontWeight = FontWeight.Black, color = ImmersiveBackground)
                            }
                        }
                    }
                }

                MultiplayerStatus.MATCHMAKING -> {
                    // Matchmaking in progress. Show the beautiful pulsing radar.
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier.size(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Pulsing radar ripples
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    color = ImmersiveLime.copy(alpha = pulseAlpha),
                                    radius = size.minDimension / 2f * pulseRadius,
                                    style = Stroke(width = 4f)
                                )
                                drawCircle(
                                    color = ImmersiveLime.copy(alpha = pulseAlpha * 0.5f),
                                    radius = size.minDimension / 2f * (pulseRadius * 0.7f).coerceAtLeast(0.1f),
                                    style = Stroke(width = 2f)
                                )
                            }

                            // Glowing inner sphere
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .background(ImmersiveLime.copy(alpha = 0.15f), CircleShape)
                                    .border(2.dp, ImmersiveLime, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = "Connecting",
                                    tint = ImmersiveLime,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(30.dp))

                        Text(
                            text = "SEARCHING FOR OPPONENT",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = "Connecting to the digital stadium grid...",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )

                        Spacer(modifier = Modifier.height(40.dp))

                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF4444).copy(alpha = 0.15f),
                                contentColor = Color(0xFFEF4444)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444)),
                            modifier = Modifier.width(160.dp)
                        ) {
                            Text("CANCEL DUEL", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                MultiplayerStatus.READY -> {
                    // Ready to initiate matchmaking.
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ImmersiveSurface),
                        border = BorderStroke(1.dp, ImmersiveBorder),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(ImmersiveLime.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Server Ready",
                                    tint = ImmersiveLime,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Text(
                                text = "Real-Time Duel Ready",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "Ready to connect to global servers and duel a player in real-time. Matches are 1-over limits.",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )

                            Button(
                                onClick = { viewModel.startMultiplayerMatchmaking() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ImmersiveLime,
                                    contentColor = ImmersiveBackground
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("FIND AN OPPONENT", fontWeight = FontWeight.Black, color = ImmersiveBackground)
                            }
                        }
                    }
                }

                MultiplayerStatus.CONNECTED -> {
                    // Connected to opponent. Loading game.
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = ImmersiveLime, modifier = Modifier.size(60.dp))
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "OPPONENT JOINED!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = ImmersiveLime
                        )
                        Text(
                            text = "Loading pitch stadium, please wait...",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                else -> {
                    // ERROR or OPPONENT_LEFT
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ImmersiveSurface),
                        border = BorderStroke(1.dp, ImmersiveBorder),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(Color(0xFFEF4444).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = "Error",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Text(
                                text = "Matchmaking Error",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "Unable to connect or matchmaking timed out. Check your internet connection and try again.",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )

                            Button(
                                onClick = { viewModel.startMultiplayerMatchmaking() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ImmersiveLime,
                                    contentColor = ImmersiveBackground
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("RETRY MATCHMAKING", fontWeight = FontWeight.Black, color = ImmersiveBackground)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SetupStepItem(number: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(ImmersiveLime.copy(alpha = 0.15f), CircleShape)
                .border(1.dp, ImmersiveLime, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ImmersiveLime
            )
        }

        Text(
            text = description,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
    }
}
