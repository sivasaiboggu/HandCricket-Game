package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ImmersiveLime
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

data class CelebrationParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    val rotationSpeed: Float,
    val rotation: Float = 0f,
    val isConfetti: Boolean = true
)

@Composable
fun CelebrationOverlay(
    isSix: Boolean,
    isWicket: Boolean,
    modifier: Modifier = Modifier
) {
    var particles by remember { mutableStateOf(emptyList<CelebrationParticle>()) }
    var textScale by remember { mutableStateOf(0f) }
    var textAlpha by remember { mutableStateOf(0f) }
    var activeCelebration by remember { mutableStateOf<String?>(null) } // "SIX" or "WICKET" or null

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        // Trigger animations when the states change to true
        LaunchedEffect(isSix, isWicket) {
            if (isSix) {
                try {
                    activeCelebration = "SIX"
                    val newList = mutableListOf<CelebrationParticle>()
                    val colors = listOf(
                        Color(0xFFE91E63), // Hot Pink
                        Color(0xFF2196F3), // Bright Blue
                        Color(0xFF4CAF50), // Grass Green
                        Color(0xFFFFEB3B), // Yellow Confetti
                        Color(0xFFFF9800), // Orange Confetti
                        ImmersiveLime
                    )

                    // Launch confetti from left corner
                    repeat(30) {
                        newList.add(
                            CelebrationParticle(
                                x = 0f,
                                y = height,
                                vx = (12..25).random().toFloat(),
                                vy = -(24..46).random().toFloat(),
                                color = colors.random(),
                                size = (16..32).random().toFloat(),
                                rotationSpeed = (-8..8).random().toFloat(),
                                isConfetti = true
                            )
                        )
                    }

                    // Launch confetti from right corner
                    repeat(30) {
                        newList.add(
                            CelebrationParticle(
                                x = width,
                                y = height,
                                vx = -(12..25).random().toFloat(),
                                vy = -(24..46).random().toFloat(),
                                color = colors.random(),
                                size = (16..32).random().toFloat(),
                                rotationSpeed = (-8..8).random().toFloat(),
                                isConfetti = true
                            )
                        )
                    }
                    particles = newList

                    // Concurrent job to run particle updates at 60fps
                    val physicsJob = launch {
                        val gravity = 0.7f
                        while (isActive && particles.isNotEmpty()) {
                            particles = particles.map { p ->
                                p.copy(
                                    x = p.x + p.vx,
                                    y = p.y + p.vy,
                                    vy = p.vy + gravity,
                                    rotation = p.rotation + p.rotationSpeed
                                )
                            }.filter { p ->
                                p.y < height + 50 && p.x > -100 && p.x < width + 100
                            }
                            delay(16)
                        }
                    }

                    // Text entrance bounce
                    textScale = 0f
                    textAlpha = 1f
                    animate(
                        initialValue = 0f,
                        targetValue = 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) { value, _ ->
                        textScale = value
                    }

                    delay(1100)

                    // Smooth fade out
                    animate(
                        initialValue = 1f,
                        targetValue = 0f,
                        animationSpec = tween(300)
                    ) { value, _ ->
                        textAlpha = value
                    }
                    physicsJob.cancel()
                } finally {
                    activeCelebration = null
                    particles = emptyList()
                }

            } else if (isWicket) {
                try {
                    activeCelebration = "WICKET"
                    val newList = mutableListOf<CelebrationParticle>()
                    val colors = listOf(
                        Color(0xFFEF4444), // Crimson Wicket Red
                        Color(0xFFFF5722), // Fire Orange
                        Color(0xFFFF9800), // Amber
                        Color(0xFFFFC107)  // Yellow Spark
                    )

                    // Wicket explosion: sparks shoot outwards in all directions from the center
                    val centerX = width / 2f
                    val centerY = height * 0.45f
                    repeat(50) {
                        val angle = (0..360).random() * (Math.PI / 180f)
                        val speed = (8..24).random().toFloat()
                        newList.add(
                            CelebrationParticle(
                                x = centerX,
                                y = centerY,
                                vx = (speed * cos(angle)).toFloat(),
                                vy = (speed * sin(angle)).toFloat(),
                                color = colors.random(),
                                size = (10..22).random().toFloat(),
                                rotationSpeed = 0f,
                                isConfetti = false
                            )
                        )
                    }
                    particles = newList

                    // Concurrent job to run particle updates at 60fps
                    val physicsJob = launch {
                        while (isActive && particles.isNotEmpty()) {
                            particles = particles.map { p ->
                                p.copy(
                                    x = p.x + p.vx,
                                    y = p.y + p.vy,
                                    vx = p.vx * 0.95f,
                                    vy = p.vy * 0.95f + 0.15f
                                )
                            }.filter { p ->
                                p.y < height + 50 && p.x > -100 && p.x < width + 100
                            }
                            delay(16)
                        }
                    }

                    // Text pop animation
                    textScale = 0f
                    textAlpha = 1f
                    animate(
                        initialValue = 0f,
                        targetValue = 1.1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioHighBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) { value, _ ->
                        textScale = value
                    }

                    delay(1100)

                    // Fade out text
                    animate(
                        initialValue = 1f,
                        targetValue = 0f,
                        animationSpec = tween(300)
                    ) { value, _ ->
                        textAlpha = value
                    }
                    physicsJob.cancel()
                } finally {
                    activeCelebration = null
                    particles = emptyList()
                }
            }
        }

        // Draw particles
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                if (p.isConfetti) {
                    rotate(p.rotation, pivot = Offset(p.x, p.y)) {
                        drawRect(
                            color = p.color,
                            topLeft = Offset(p.x - p.size / 2f, p.y - p.size / 4f),
                            size = androidx.compose.ui.geometry.Size(p.size, p.size / 2f)
                        )
                    }
                } else {
                    drawCircle(
                        color = p.color,
                        radius = p.size / 2f,
                        center = Offset(p.x, p.y)
                    )
                }
            }
        }

        // Animated overlay text
        if (activeCelebration != null && textAlpha > 0f) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        scaleX = textScale
                        scaleY = textScale
                        alpha = textAlpha
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (activeCelebration == "SIX") {
                    Text(
                        text = "GLORIOUS 6!",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        color = ImmersiveLime,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "MAXIMUM HIT!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 4.sp,
                        textAlign = TextAlign.Center
                    )
                } else if (activeCelebration == "WICKET") {
                    Text(
                        text = "OUT!",
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFEF4444),
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "WICKET FALLEN!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 4.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
