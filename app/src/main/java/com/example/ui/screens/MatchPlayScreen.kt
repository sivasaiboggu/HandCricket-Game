package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HandChoiceButton
import com.example.ui.components.StadiumPitchVisualizer
import com.example.ui.viewmodel.GameViewModel
import com.example.ui.viewmodel.MatchPhase
import com.example.ui.viewmodel.PlayerRole
import com.example.ui.viewmodel.MatchState
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
fun MatchPlayScreen(
    viewModel: GameViewModel,
    onBackToMenu: () -> Unit
) {
    val state by viewModel.matchState.collectAsState()
    val stats by viewModel.playerStats.collectAsState()

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(ImmersiveBackground, ImmersiveSurface)
    )

    // Strobe screen-flash modifier for dramatic effects
    val flashColor = when {
        state.isOutEvent -> Color(0xFFEF4444).copy(alpha = 0.2f)
        state.isScoreEvent -> Color(0xFFFFD54F).copy(alpha = 0.12f)
        else -> Color.Transparent
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Active Pitch Duel", fontWeight = FontWeight.Black, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackToMenu, modifier = Modifier.testTag("play_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Menu", tint = Color.White)
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
                .background(flashColor)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Dynamic Scoreboard & Match Target Contexts
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScoreboardWidget(state = state)

                    if (state.currentInnings == 2 && state.target != null) {
                        TargetChaseMeter(state = state)
                    }
                }

                // 2. High-Graphics Stadium perspective Canvas
                StadiumPitchVisualizer(
                    stadiumTheme = state.stadiumTheme,
                    isOut = state.isOutEvent,
                    isScore = state.isScoreEvent,
                    lastScore = state.lastPlayerMove,
                    ballsBowled = state.ballsBowled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                )

                // 3. Choice Duelling Indicator (You vs AI move display)
                InningsDuelPanel(state = state)

                // 4. Live ball-by-ball commentary
                CommentaryBox(commentary = state.commentText)

                Spacer(modifier = Modifier.height(8.dp))

                // 5. Grid of Finger selection Keys (1..6)
                if (state.phase != MatchPhase.COMPLETED) {
                    FingerDecisionDeck(
                        onPlayBall = { choice ->
                            viewModel.playBall(choice)
                        }
                    )
                } else {
                    // Match Finished. Show primary reset button instead of choice keys
                    Button(
                        onClick = onBackToMenu,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("return_menu_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ImmersiveLime,
                            contentColor = ImmersiveBackground
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("RETURN TO HOME", fontWeight = FontWeight.Black, color = ImmersiveBackground)
                    }
                }
            }

            // 6. Game Over Scorecard Modal
            if (state.phase == MatchPhase.COMPLETED) {
                MatchScoreSelectionDialog(
                    state = state,
                    currentLevel = stats.level,
                    onBackToMenu = onBackToMenu
                )
            }
        }
    }
}

@Composable
fun ScoreboardWidget(state: MatchState) {
    val roleColor = if (state.playerRole == PlayerRole.BATTING) ImmersiveLime else Color(0xFF4FC3F7)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ImmersiveSurface),
        border = BorderStroke(1.dp, ImmersiveBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Role and overs limits
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(roleColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.playerRole == PlayerRole.BATTING) "YOU ARE BATTING" else "YOU ARE BOWLING",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = roleColor
                    )
                }

                Text(
                    text = "OVERS LIMIT: ${state.matchOversLimit}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Score details comparison
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Player Score
                Column {
                    Text(text = "YOUR RUNS", fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = state.playerRuns.toString(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = " / ${state.playerWickets}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                // Center vertical line
                Box(
                    modifier = Modifier
                        .height(35.dp)
                        .width(1.dp)
                        .background(Color.White.copy(alpha = 0.1f))
                )

                // Right: AI score
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "AI RUNS", fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = state.aiRuns.toString(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = " / ${state.aiWickets}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = Color.White.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(8.dp))

            // Current balls/overs progress bar
            val currentOverNo = state.ballsBowled / 6
            val currentBallNo = state.ballsBowled % 6
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Innings #${state.currentInnings}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ImmersiveLime
                )

                Text(
                    text = "Overs Progressive: $currentOverNo.$currentBallNo / ${state.matchOversLimit}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun TargetChaseMeter(state: MatchState) {
    val target = state.target ?: 1
    val baseScore = if (state.playerRole == PlayerRole.BATTING) state.playerRuns else state.aiRuns
    val requiredRuns = (target - baseScore).coerceAtLeast(0)

    val maxInningsBalls = state.matchOversLimit * 6
    val remainingBalls = (maxInningsBalls - state.ballsBowled).coerceAtLeast(0)

    val chaseRatio = if (target > 0) baseScore.toFloat() / target else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ImmersiveSurface),
        border = BorderStroke(1.dp, ImmersiveBorder)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TARGET: $target",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ImmersiveLime
                )

                Text(
                    text = "Need $requiredRuns runs off $remainingBalls balls",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            // Target progress bar
            LinearProgressIndicator(
                progress = { chaseRatio.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = ImmersiveLime,
                trackColor = ImmersiveBorder
            )
        }
    }
}

@Composable
fun InnsDuelCircle(
    title: String,
    choice: Int,
    color: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ImmersiveSurface),
        border = BorderStroke(1.dp, ImmersiveBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.width(100.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(10.dp)
        ) {
            Text(text = title, fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape)
                    .background(Brush.radialGradient(listOf(color.copy(alpha = 0.1f), Color.Transparent)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (choice > 0) choice.toString() else "-",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = color
                )
            }
        }
    }
}

@Composable
fun InningsDuelPanel(state: MatchState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // You choice
        InnsDuelCircle(
            title = "YOUR GESTURE",
            choice = state.lastPlayerMove,
            color = if (state.isOutEvent) Color(0xFFEF4444) else ImmersiveLime
        )

        // Center Battle Verses indicator
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (state.lastPlayerMove > 0 && state.lastAiMove > 0) {
                if (state.isOutEvent) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFEF4444), RoundedCornerShape(4.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("OUT!", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                } else {
                    val addVal = if (state.playerRole == PlayerRole.BATTING) state.lastPlayerMove else state.lastAiMove
                    Box(
                        modifier = Modifier
                            .background(ImmersiveLime, RoundedCornerShape(4.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("+$addVal RUNS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = ImmersiveBackground)
                    }
                }
            } else {
                Text(
                    text = "VS",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White.copy(alpha = 0.2f)
                )
            }
        }

        // AI Choice
        InnsDuelCircle(
            title = "AI GESTURE",
            choice = state.lastAiMove,
            color = if (state.isOutEvent) Color(0xFFEF4444) else Color(0xFF81D4FA)
        )
    }
}

@Composable
fun CommentaryBox(commentary: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ImmersiveSurface),
        border = BorderStroke(1.dp, ImmersiveBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SportsCricket,
                    contentDescription = null,
                    tint = ImmersiveLime,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = commentary,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun FingerDecisionDeck(
    onPlayBall: (Int) -> Unit
) {
    Column {
        Text(
            text = "TAP TO SHOW RUN FINGERS",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().height(180.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.weight(1f)) {
                    HandChoiceButton(1, onClick = { onPlayBall(1) }, modifier = Modifier.weight(1f))
                    HandChoiceButton(2, onClick = { onPlayBall(2) }, modifier = Modifier.weight(1f))
                    HandChoiceButton(3, onClick = { onPlayBall(3) }, modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.weight(1f)) {
                    HandChoiceButton(4, onClick = { onPlayBall(4) }, modifier = Modifier.weight(1f))
                    HandChoiceButton(5, onClick = { onPlayBall(5) }, modifier = Modifier.weight(1f))
                    HandChoiceButton(6, onClick = { onPlayBall(6) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun MatchScoreSelectionDialog(
    state: MatchState,
    currentLevel: Int,
    onBackToMenu: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {}, // Force scorecard acknowledgment
        confirmButton = {
            Button(
                onClick = onBackToMenu,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ImmersiveLime,
                    contentColor = ImmersiveBackground
                ),
                modifier = Modifier.testTag("dialog_dismiss_button")
            ) {
                Text("PROCEED HOME", fontWeight = FontWeight.Bold, color = ImmersiveBackground)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = ImmersiveLime)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "BATTLE SCORECARD",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = state.winMessage,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Divider(color = Color.White.copy(alpha = 0.08f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("PLAYER FINAL", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                        Text("${state.playerRuns} / ${state.playerWickets}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("AI FINAL", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                        Text("${state.aiRuns} / ${state.aiWickets}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.08f))

                // XP rewards overview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ImmersiveBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "OFFLINE PROGRESSION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ImmersiveLime
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Earned +${state.xpGained} XP in this battle!",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "Current Trainer ranking: Level $currentLevel",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        },
        containerColor = ImmersiveSurface,
        shape = RoundedCornerShape(16.dp)
    )
}
