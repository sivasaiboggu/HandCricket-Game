package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun HandChoiceButton(
    number: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "ButtonPress"
    )

    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(150)
            isPressed = false
        }
    }

    // Assign specific theme colors based on runs potential
    val buttonColors = when (number) {
        1 -> ButtonColorToken(
            accent = Color(0xFF93C5FD), // Cool Blue
            runTitle = "Single"
        )
        2 -> ButtonColorToken(
            accent = Color(0xFF99F6E4), // Cool Teal
            runTitle = "Double"
        )
        3 -> ButtonColorToken(
            accent = Color(0xFF86EFAC), // Rich Emerald
            runTitle = "Triple"
        )
        4 -> ButtonColorToken(
            accent = Color(0xFFFED7AA), // Fiery Orange
            runTitle = "Four"
        )
        5 -> ButtonColorToken(
            accent = Color(0xFFFECACA), // Crimson Red
            runTitle = "Five"
        )
        else -> ButtonColorToken(
            accent = Color(0xFFE9D5FF), // Cosmic Purple
            runTitle = "Sixer"
        )
    }

    Card(
        modifier = modifier
            .padding(3.dp)
            .scale(scale)
            .testTag("run_choice_button_$number")
            .clip(RoundedCornerShape(14.dp))
            .graphicsLayer { alpha = if (enabled) 1.0f else 0.45f }
            .clickable(enabled = enabled) {
                isPressed = true
                onClick()
            },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            Brush.verticalGradient(
                listOf(
                    if (enabled) buttonColors.accent.copy(alpha = 0.6f) else buttonColors.accent.copy(alpha = 0.2f),
                    if (enabled) buttonColors.accent.copy(alpha = 0.1f) else buttonColors.accent.copy(alpha = 0.02f)
                )
            )
        ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = if (enabled) 3.dp else 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1E293B).copy(alpha = 0.75f), Color(0xFF0F172A).copy(alpha = 0.90f))
                    )
                )
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (enabled) buttonColors.accent.copy(alpha = 0.10f) else Color.Transparent,
                            Color.Transparent
                        ),
                        radius = 100f
                    )
                )
                .padding(vertical = 6.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxHeight()
            ) {
                // 1. Centered hand gesture emoji in a glowing circular backing
                val emoji = when (number) {
                    1 -> "☝️"
                    2 -> "✌️"
                    3 -> "🤟"
                    4 -> "🖖"
                    5 -> "🖐️"
                    else -> "✊"
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    if (enabled) buttonColors.accent.copy(alpha = 0.25f) else buttonColors.accent.copy(alpha = 0.05f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    if (enabled) buttonColors.accent.copy(alpha = 0.7f) else buttonColors.accent.copy(alpha = 0.15f),
                                    if (enabled) buttonColors.accent.copy(alpha = 0.1f) else Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 2. Styled display number and title label at the bottom
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = number.toString(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = buttonColors.accent,
                        lineHeight = 15.sp
                    )
                    Text(
                        text = buttonColors.runTitle.uppercase(),
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White.copy(alpha = if (enabled) 0.7f else 0.3f),
                        letterSpacing = 0.5.sp,
                        lineHeight = 7.sp
                    )
                }
            }
        }
    }
}

private data class ButtonColorToken(
    val accent: Color,
    val runTitle: String
)
