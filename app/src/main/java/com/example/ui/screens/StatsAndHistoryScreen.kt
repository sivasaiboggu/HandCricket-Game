package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Leaderboard
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.GameViewModel
import com.example.data.model.MatchHistory
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.ui.theme.ImmersiveBackground
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.ImmersiveBorder
import com.example.ui.theme.ImmersiveLime
import com.example.ui.theme.ImmersiveTextPrimary
import com.example.ui.theme.ImmersiveTextSecondary
import androidx.compose.foundation.BorderStroke
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsAndHistoryScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val stats by viewModel.playerStats.collectAsState()
    val history by viewModel.matchHistory.collectAsState()

    // Professional background gradient
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(ImmersiveBackground, ImmersiveSurface)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Career & Statistics", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("stats_back_button")) {
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
                .padding(16.dp)
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Level progress card
                item {
                    val nextLevelXp = stats.level * 250
                    val xpProgress = if (nextLevelXp > 0) stats.exp.toFloat() / nextLevelXp else 0f

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ImmersiveSurface),
                        border = BorderStroke(1.dp, ImmersiveBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text(
                                        text = "PLAYER LEVEL",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ImmersiveLime
                                    )
                                    Text(
                                        text = "Level ${stats.level}",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .background(ImmersiveLime, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = ImmersiveBackground,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // XP Progress bar
                            LinearProgressIndicator(
                                progress = { xpProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = ImmersiveLime,
                                trackColor = ImmersiveBorder
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${stats.exp} XP",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "$nextLevelXp XP to Lvl ${stats.level + 1}",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }

                // 2. Statistics Grid
                item {
                    Text(
                        text = "CAREER PERFORMANCE",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ImmersiveLime,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCard(
                                title = "Matches",
                                value = stats.matchesPlayed.toString(),
                                icon = Icons.Default.Leaderboard,
                                accentColor = Color(0xFF388E3C),
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Win Rate",
                                value = String.format(Locale.getDefault(), "%.1f%%", stats.winRate),
                                icon = Icons.Default.EmojiEvents,
                                accentColor = Color(0xFFFFB300),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCard(
                                title = "Total Runs",
                                value = stats.runsScored.toString(),
                                icon = Icons.Default.Star,
                                accentColor = Color(0xFFFF7043),
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Best Score",
                                value = stats.highestScore.toString(),
                                icon = Icons.Default.EmojiEvents,
                                accentColor = Color(0xFF00ACC1),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCard(
                                title = "Wickets Taken",
                                value = stats.wicketsTaken.toString(),
                                icon = Icons.Default.Star,
                                accentColor = Color(0xFFAB47BC),
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Dot Balls",
                                value = stats.dotBallsBowled.toString(),
                                icon = Icons.Default.Leaderboard,
                                accentColor = Color(0xFF26A69A),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 3. Match History Section
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = ImmersiveLime)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "BATTLE HISTORY",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ImmersiveLime
                        )
                    }
                }

                if (history.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = ImmersiveSurface),
                            border = BorderStroke(1.dp, ImmersiveBorder)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No matches played yet. Step into the pitch!",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                } else {
                    items(history) { match ->
                        MatchHistoryRow(match = match)
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ImmersiveSurface),
        border = BorderStroke(1.dp, ImmersiveBorder)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(accentColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = value,
                    fontSize = 18.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun MatchHistoryRow(match: MatchHistory) {
    val formatter = SimpleDateFormat("MMM dd, yyyy - h:mm a", Locale.getDefault())
    val dateStr = formatter.format(Date(match.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ImmersiveSurface
        ),
        border = BorderStroke(1.dp, ImmersiveBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (match.isWin) ImmersiveLime else Color(0xFFEF4444))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (match.isWin) "WIN" else "LOSS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = if (match.isWin) ImmersiveBackground else Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "vs AI (${match.difficulty.lowercase().replaceFirstChar { it.uppercase() }})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    text = dateStr,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Divider(color = Color.White.copy(alpha = 0.08f))

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("PLAYER SCORE", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                    Text(
                        text = "${match.playerRuns} / ${match.playerWickets}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = if (match.isWin) ImmersiveLime else Color.White
                    )
                }
                Text(
                    text = "vs",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text("AI SCORE", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                    Text(
                        text = "${match.aiRuns} / ${match.aiWickets}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = if (!match.isWin) ImmersiveLime else Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${match.oversLimit} Overs Match",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}
