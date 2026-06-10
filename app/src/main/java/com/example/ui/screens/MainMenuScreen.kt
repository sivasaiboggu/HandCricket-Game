package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.GameViewModel
import com.example.ui.theme.ImmersiveBackground
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.ImmersiveBorder
import com.example.ui.theme.ImmersiveLime
import com.example.ui.theme.ImmersiveTextPrimary
import com.example.ui.theme.ImmersiveTextSecondary
import androidx.compose.foundation.BorderStroke

@Composable
fun MainMenuScreen(
    viewModel: GameViewModel,
    onStartMatchConfig: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenAchievements: () -> Unit
) {
    val stats by viewModel.playerStats.collectAsState()

    // Smooth ambient background elements
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(ImmersiveBackground, ImmersiveSurface)
    )

    // Animated glow pulse
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val laserPulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        // Dynamic Glowing Stadium Canopy drawn on background Canvas
        Canvas(modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .align(Alignment.TopCenter)
        ) {
            val width = size.width
            val height = size.height

            // Concentric stadium circular domes
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ImmersiveLime.copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(width / 2f, -height * 0.2f),
                    radius = width * 0.9f
                )
            )

            // Neon Canopy arches (Sporty Stadium Lights theme)
            drawArc(
                color = ImmersiveLime.copy(alpha = 0.35f * laserPulse),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(-width * 0.2f, -height * 0.6f),
                size = androidx.compose.ui.geometry.Size(width * 1.4f, height * 1.4f),
                style = Stroke(width = 8f)
            )

            drawArc(
                color = ImmersiveLime.copy(alpha = 0.15f * (1.2f - laserPulse)),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(-width * 0.1f, -height * 0.5f),
                size = androidx.compose.ui.geometry.Size(width * 1.2f, height * 1.2f),
                style = Stroke(width = 4f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. App Title Canopy
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Text(
                    text = "HAND CRICKET",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "PORTRAIT MASTER DUELS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ImmersiveLime,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center
                )
            }

            // 2. Active Level card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = ImmersiveSurface),
                border = BorderStroke(1.dp, ImmersiveBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "OFFLINE CAREER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ImmersiveLime
                            )
                            Text(
                                text = "Rank Star / Lvl ${stats.level}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(ImmersiveBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = ImmersiveLime,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val nextLevelXp = stats.level * 250
                    val progress = if (nextLevelXp > 0) stats.exp.toFloat() / nextLevelXp else 0f
                    
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = ImmersiveLime,
                        trackColor = ImmersiveBorder
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${stats.runsScored} Runs • ${stats.matchesWon} Wins",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            // 3. Central Actions Button Clusters
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Main Match Button (Elevated Big Action)
                Button(
                    onClick = onStartMatchConfig,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("play_match_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ImmersiveLime,
                        contentColor = ImmersiveBackground
                    ),
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(28.dp), tint = ImmersiveBackground)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "START PLAY DUEL",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = ImmersiveBackground
                        )
                    }
                }

                // Grid of side menus
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {


                    // Achievements
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .testTag("achievements_menu")
                            .clip(RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = ImmersiveSurface),
                        border = BorderStroke(1.dp, ImmersiveBorder),
                        onClick = onOpenAchievements
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = ImmersiveLime, modifier = Modifier.size(30.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Badges", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    // Career
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .testTag("history_stats_menu")
                            .clip(RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = ImmersiveSurface),
                        border = BorderStroke(1.dp, ImmersiveBorder),
                        onClick = onOpenStats
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = ImmersiveLime, modifier = Modifier.size(30.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Stats", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // 4. Footnote Sign
            Text(
                text = "Cricket Mastery Edition v1.1",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}
