package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HandChoiceButton(
    number: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "ButtonPress"
    )

    // Assign specific theme colors based on runs potential
    val buttonColors = when (number) {
        1 -> ButtonColorToken(
            mainGrad = listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6)), // Cool Blue
            accent = Color(0xFF93C5FD),
            runTitle = "Single"
        )
        2 -> ButtonColorToken(
            mainGrad = listOf(Color(0xFF115E59), Color(0xFF14B8A6)), // Cool Teal
            accent = Color(0xFF99F6E4),
            runTitle = "Couple"
        )
        3 -> ButtonColorToken(
            mainGrad = listOf(Color(0xFF166534), Color(0xFF22C55E)), // Rich Emerald
            accent = Color(0xFF86EFAC),
            runTitle = "Triple"
        )
        4 -> ButtonColorToken(
            mainGrad = listOf(Color(0xFF9A3412), Color(0xFFF97316)), // Fiery Orange
            accent = Color(0xFFFED7AA),
            runTitle = "Boundary"
        )
        5 -> ButtonColorToken(
            mainGrad = listOf(Color(0xFF991B1B), Color(0xFFEF4444)), // Crimson Red
            accent = Color(0xFFFECACA),
            runTitle = "Sneaky Five"
        )
        else -> ButtonColorToken(
            mainGrad = listOf(Color(0xFF6B21A8), Color(0xFFA855F7)), // Cosmic Purple
            accent = Color(0xFFE9D5FF),
            runTitle = "GLORIOUS SIX"
        )
    }

    Card(
        modifier = modifier
            .padding(4.dp)
            .scale(scale)
            .testTag("run_choice_button_$number")
            .clip(CardDefaults.shape)
            .clickable {
                isPressed = true
                onClick()
                // Auto-reset pulse
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    isPressed = false
                }, 150)
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(buttonColors.mainGrad)
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Large styled Display Number
                Text(
                    text = number.toString(),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Mini Ball rating indicators
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    repeat(number) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = buttonColors.accent,
                            modifier = Modifier.size(9.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Run type title label
                Text(
                    text = buttonColors.runTitle,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

private data class ButtonColorToken(
    val mainGrad: List<Color>,
    val accent: Color,
    val runTitle: String
)
