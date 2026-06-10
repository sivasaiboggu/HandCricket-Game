package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.ui.window.Dialog
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.ui.components.HandSignAnimator
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

                // 2. Hand Sign Animation Visualizer
                HandSignAnimator(
                    playerChoice = state.lastPlayerMove,
                    aiChoice = state.lastAiMove,
                    isShaking = state.isActionShaking,
                    isOut = state.isOutEvent,
                    isScoreEvent = state.isScoreEvent,
                    playerRole = state.playerRole,
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

                // 5. Grid of Finger selection Keys (1..6)
                if (state.phase != MatchPhase.COMPLETED) {
                    TimerProgressBar(secondsRemaining = state.timerValue)
                    Spacer(modifier = Modifier.height(6.dp))
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
    
    // CRR & RRR calculation
    val balls = state.ballsBowled
    val battingRuns = if (state.playerRole == PlayerRole.BATTING) state.playerRuns else state.aiRuns
    val crr = if (balls > 0) (battingRuns.toFloat() / (balls / 6f)) else 0f
    
    val target = state.target
    val isSecondInnings = state.currentInnings == 2 && target != null
    val maxBalls = state.matchOversLimit * 6
    val remainingBalls = (maxBalls - balls).coerceAtLeast(0)
    
    val rrr = if (isSecondInnings && target != null && remainingBalls > 0) {
        val requiredRuns = (target - battingRuns).coerceAtLeast(0)
        requiredRuns.toFloat() / (remainingBalls / 6f)
    } else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ImmersiveSurface),
        border = BorderStroke(1.dp, ImmersiveBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Active Role, CRR, RRR
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
                        text = if (state.playerRole == PlayerRole.BATTING) "YOU BATTING" else "YOU BOWLING",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = roleColor,
                        letterSpacing = 1.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "CRR: ${String.format("%.2f", crr)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (isSecondInnings) {
                        Text(
                            text = "RRR: ${String.format("%.2f", rrr)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ImmersiveLime
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Score Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // YOU score block
                Column {
                    Text(
                        text = "YOU",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = state.playerRuns.toString(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = if (state.playerRole == PlayerRole.BATTING) ImmersiveLime else Color.White
                        )
                        Text(
                            text = " / ${state.playerWickets}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                // AI score block
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "AI DEFENDER",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = state.aiRuns.toString(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = if (state.playerRole == PlayerRole.BOWLING) ImmersiveLime else Color.White
                        )
                        Text(
                            text = " / ${state.aiWickets}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(8.dp))

            // Ball by ball Over Timeline (Crex Style) & Over info
            val currentOverNo = state.ballsBowled / 6
            val currentBallNo = state.ballsBowled % 6
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RecentBallsTimeline(recentBalls = state.recentBalls)

                Text(
                    text = "OVERS: $currentOverNo.$currentBallNo / ${state.matchOversLimit}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun RecentBallsTimeline(recentBalls: List<String>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        recentBalls.forEach { res ->
            BallTimelineChip(result = res)
        }
        if (recentBalls.isEmpty()) {
            Text(
                text = "No balls bowled yet",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.3f),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BallTimelineChip(result: String) {
    val chipColors = when (result) {
        "W" -> Pair(Color(0xFFEF4444), Color.White) // Red Wicket
        "6" -> Pair(ImmersiveLime, ImmersiveBackground) // Green 6
        "4" -> Pair(Color(0xFF818CF8), Color.White) // Purple 4
        "0" -> Pair(ImmersiveBorder.copy(alpha = 0.5f), Color.White.copy(alpha = 0.6f)) // Dot ball
        else -> Pair(ImmersiveBorder, Color.White) // Other runs
    }
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(chipColors.first, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = result,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = chipColors.second
        )
    }
}

@Composable
fun TimerProgressBar(secondsRemaining: Int) {
    val progress = secondsRemaining / 10f
    val color = when {
        secondsRemaining <= 3 -> Color(0xFFEF4444) // Warning Red
        secondsRemaining <= 6 -> Color(0xFFFFA726) // Warning Orange
        else -> ImmersiveLime // Safe Green
    }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CHOOSE FINGERS GESTURE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.5f)
            )
            Text(
                text = "${secondsRemaining}s remaining",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = ImmersiveBorder.copy(alpha = 0.5f)
        )
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
    var selectedTab by remember { mutableStateOf(0) } // 0: Summary, 1: Progression & XP
    val isWin = state.winMessage.contains("Victory", ignoreCase = true)

    Dialog(onDismissRequest = {}) {
        val entranceScale = remember { Animatable(0.8f) }
        LaunchedEffect(Unit) {
            entranceScale.animateTo(
                1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }

        Card(
            modifier = Modifier
                .width(340.dp)
                .height(480.dp)
                .graphicsLayer {
                    scaleX = entranceScale.value
                    scaleY = entranceScale.value
                },
            colors = CardDefaults.cardColors(containerColor = Color(0xEC111827)), // Dark semi-transparent
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(2.dp, Brush.horizontalGradient(
                if (isWin) listOf(ImmersiveLime, ImmersiveBorder) else listOf(Color(0xFFEF4444), ImmersiveBorder)
            ))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Victory/Defeat Banner
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (isWin) ImmersiveLime.copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                                CircleShape
                              ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isWin) Icons.Default.EmojiEvents else Icons.Default.SportsCricket,
                            contentDescription = null,
                            tint = if (isWin) ImmersiveLime else Color(0xFFEF4444),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isWin) "VICTORY!" else "DEFEAT",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isWin) ImmersiveLime else Color(0xFFEF4444),
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (isWin) "You defeated the AI Defender!" else "The AI Defender outsmarted you!",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Tab Navigation Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .padding(2.dp)
                ) {
                    Button(
                        onClick = { selectedTab = 0 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 0) Color.White.copy(alpha = 0.12f) else Color.Transparent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("SUMMARY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { selectedTab = 1 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 1) Color.White.copy(alpha = 0.12f) else Color.Transparent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("XP & CAREER", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Tab Contents
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (selectedTab == 0) {
                        // Tab 0: Summary
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Scores comparison card
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Card(
                                    modifier = Modifier.weight(1f).padding(end = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                                    border = BorderStroke(1.dp, ImmersiveBorder.copy(alpha = 0.2f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("YOU", fontSize = 10.sp, color = ImmersiveLime, fontWeight = FontWeight.Black)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${state.playerRuns} / ${state.playerWickets}",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }
                                }

                                Card(
                                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                                    border = BorderStroke(1.dp, ImmersiveBorder.copy(alpha = 0.2f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("AI DEFENDER", fontSize = 10.sp, color = Color(0xFF81D4FA), fontWeight = FontWeight.Black)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${state.aiRuns} / ${state.aiWickets}",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            Divider(color = Color.White.copy(alpha = 0.08f))

                            // Game metadata list
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Difficulty Mode", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                                    Text(state.difficulty.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Overs Limit", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                                    Text("${state.matchOversLimit} Overs", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Sixes Hit", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                                    Text("${state.sixesHitThisMatch}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ImmersiveLime)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Dot Balls Bowled", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                                    Text("${state.dotBallsBowledThisMatch}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81D4FA))
                                }
                            }
                        }
                    } else {
                        // Tab 1: Progression & XP
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // XP Card
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                                    .border(1.dp, ImmersiveBorder.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text("BATTLE XP EARNED", fontSize = 10.sp, color = ImmersiveLime, fontWeight = FontWeight.Black)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "+${state.xpGained} XP",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Includes: ${if (isWin) "150 (Win)" else "40 (Loss)"} + runs/wickets/dots bonus",
                                        fontSize = 9.sp,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Progression Progress Bar
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("TRAINER RANKING", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                                    Text("LEVEL $currentLevel", fontSize = 12.sp, color = ImmersiveLime, fontWeight = FontWeight.Black)
                                }
                                Spacer(modifier = Modifier.height(6.dp))

                                val nextLevelReq = currentLevel * 250
                                val progressPct = (state.xpGained.toFloat() / nextLevelReq).coerceIn(0f, 1f)

                                LinearProgressIndicator(
                                    progress = { progressPct },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = ImmersiveLime,
                                    trackColor = ImmersiveBorder
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Next Rank needs ${nextLevelReq} XP total",
                                    fontSize = 9.sp,
                                    color = Color.White.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Action Buttons
                Button(
                    onClick = onBackToMenu,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("dialog_dismiss_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isWin) ImmersiveLime else Color(0xFFEF4444),
                        contentColor = ImmersiveBackground
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "PROCEED TO HQ",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = ImmersiveBackground,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
