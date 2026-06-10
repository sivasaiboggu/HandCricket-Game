package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.Difficulty
import com.example.ui.viewmodel.GameViewModel
import com.example.ui.viewmodel.StadiumTheme
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
fun ConfigureMatchScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val stats by viewModel.playerStats.collectAsState()
    
    var selectedOvers by remember { mutableStateOf(1) }
    var selectedWickets by remember { mutableStateOf(1) }
    var selectedDifficulty by remember { mutableStateOf(Difficulty.MEDIUM) }
    var selectedTheme by remember { mutableStateOf(StadiumTheme.CLASSIC_TURF) }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(ImmersiveBackground, ImmersiveSurface)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Match Settings", fontWeight = FontWeight.Bold, color = Color.White) },
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
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "CUSTOMIZE YOUR DUEL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ImmersiveLime,
                        letterSpacing = 2.sp
                    )

                    // 1. Overs Selector
                    OptionCard(title = "Innings Length (Overs)") {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(1, 3, 5).forEach { over ->
                                val isSelected = selectedOvers == over
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedOvers = over },
                                    label = { Text("$over Over${if (over > 1) "s" else ""}", fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ImmersiveLime,
                                        selectedLabelColor = ImmersiveBackground,
                                        containerColor = ImmersiveBorder.copy(alpha = 0.4f),
                                        labelColor = Color.White.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.weight(1f).testTag("select_overs_$over")
                                )
                            }
                        }
                    }

                    // 2. Wickets Selector
                    OptionCard(title = "Total Wickets (Outs limit)") {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(1, 3, 5).forEach { wicket ->
                                val isSelected = selectedWickets == wicket
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedWickets = wicket },
                                    label = { Text("$wicket Wicket${if (wicket > 1) "s" else ""}", fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ImmersiveLime,
                                        selectedLabelColor = ImmersiveBackground,
                                        containerColor = ImmersiveBorder.copy(alpha = 0.4f),
                                        labelColor = Color.White.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.weight(1f).testTag("select_wickets_$wicket")
                                )
                            }
                        }
                    }

                    // 3. Difficulty Selector
                    OptionCard(title = "AI Defending Tactics") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Difficulty.values().forEach { diff ->
                                val isSelected = selectedDifficulty == diff
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("select_diff_${diff.name}"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) ImmersiveLime.copy(alpha = 0.12f) else ImmersiveSurface
                                    ),
                                    border = BorderStroke(1.dp, if (isSelected) ImmersiveLime else ImmersiveBorder),
                                    onClick = { selectedDifficulty = diff }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = when (diff) {
                                                Difficulty.EASY -> "Rookie AI"
                                                Difficulty.MEDIUM -> "Professional Bowler"
                                                Difficulty.MASTERY -> "GENIUS MASTERY AI (Markov Predictions)"
                                            },
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isSelected) ImmersiveLime else Color.White
                                        )
                                        Text(
                                            text = when (diff) {
                                                Difficulty.EASY -> "Picks numbers general random. Excellent for fresh entries."
                                                Difficulty.MEDIUM -> "Observes overall patterns and restricts easy scoring runs."
                                                Difficulty.MASTERY -> "Traces previous finger moves, predictions sequence, and counters you directly!"
                                            },
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. Stadium choice card
                    OptionCard(title = "Arena Theme Selection") {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StadiumTheme.values().forEach { theme ->
                                val isUnlocked = stats.level >= theme.levelRequired
                                val isSelected = selectedTheme == theme
                                
                                Button(
                                    onClick = { if (isUnlocked) selectedTheme = theme },
                                    enabled = isUnlocked,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) ImmersiveLime else ImmersiveBorder.copy(alpha = 0.4f),
                                        disabledContainerColor = ImmersiveSurface.copy(alpha = 0.3f),
                                        contentColor = if (isSelected) ImmersiveBackground else Color.White
                                    ),
                                    modifier = Modifier.weight(1f).testTag("quick_theme_${theme.id}"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (isUnlocked) theme.title.split(" ").first() else "🔒",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Proceed Button
                Button(
                    onClick = {
                        viewModel.configureMatch(
                            overs = selectedOvers,
                            wickets = selectedWickets,
                            difficulty = selectedDifficulty,
                            theme = selectedTheme
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("confirm_settings_button")
                        .padding(bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ImmersiveLime,
                        contentColor = ImmersiveBackground
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PROCEED TO TOSS", fontWeight = FontWeight.Black, fontSize = 16.sp, color = ImmersiveBackground)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = ImmersiveBackground)
                    }
                }
            }
        }
    }
}

@Composable
fun OptionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = ImmersiveSurface),
            border = BorderStroke(1.dp, ImmersiveBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.padding(12.dp)) {
                content()
            }
        }
    }
}
