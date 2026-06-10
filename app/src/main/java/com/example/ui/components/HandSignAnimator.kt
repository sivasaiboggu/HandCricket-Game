package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ImmersiveBackground
import com.example.ui.theme.ImmersiveBorder
import com.example.ui.theme.ImmersiveLime
import com.example.ui.theme.ImmersiveSurface
import kotlin.math.sin

@Composable
fun HandSignAnimator(
    playerChoice: Int,
    aiChoice: Int,
    isShaking: Boolean,
    isOut: Boolean,
    isScoreEvent: Boolean = false,
    playerRole: com.example.ui.viewmodel.PlayerRole = com.example.ui.viewmodel.PlayerRole.BATTING,
    modifier: Modifier = Modifier
) {
    // 1. Setup continuous shake animation values when isShaking is true
    val infiniteTransition = rememberInfiniteTransition(label = "Shake Loop")
    val shakeOffset by infiniteTransition.animateFloat(
        initialValue = -25f,
        targetValue = 25f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ShakeY"
    )

    val shakeAngle by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ShakeAngle"
    )

    // 2. Setup pop/scale animation on reveal
    val revealScale = remember { Animatable(0.8f) }
    LaunchedEffect(isShaking) {
        if (!isShaking) {
            revealScale.snapTo(0.8f)
            revealScale.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    // 3. Wicket Particle animation
    val particleProgress = remember { Animatable(0f) }
    LaunchedEffect(isOut) {
        if (isOut) {
            particleProgress.snapTo(0f)
            particleProgress.animateTo(1f, tween(1000, easing = FastOutSlowInEasing))
        }
    }

    // 4. Score Neon Halo animation
    val scoreProgress = remember { Animatable(0f) }
    LaunchedEffect(isScoreEvent) {
        if (isScoreEvent) {
            scoreProgress.snapTo(0f)
            scoreProgress.animateTo(1f, tween(1000, easing = LinearOutSlowInEasing))
        }
    }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
    )

    Card(
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(16.dp)
        ) {
            // Background glow ring
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (isOut) Color(0x30EF4444) else ImmersiveLime.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        radius = size.width * 0.6f
                    ),
                    center = Offset(size.width / 2f, size.height / 2f)
                )
            }

            // Particle explosion on wicket out event
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (particleProgress.value > 0f && particleProgress.value < 1f) {
                    val progress = particleProgress.value
                    val pCount = 18
                    for (i in 0 until pCount) {
                        val angle = (i * (2 * Math.PI / pCount)).toFloat()
                        val radius = size.width * 0.4f * progress
                        val px = size.width / 2f + radius * kotlin.math.cos(angle.toDouble()).toFloat()
                        val py = size.height / 2f + radius * kotlin.math.sin(angle.toDouble()).toFloat()
                        val color = when (i % 3) {
                            0 -> Color(0xFFEF4444) // Wicket Red
                            1 -> Color(0xFFFF9800) // Fire Orange
                            else -> Color(0xFFFFD54F) // Sparks Yellow
                        }
                        drawCircle(
                            color = color.copy(alpha = 1f - progress),
                            radius = (12f * (1f - progress)).coerceAtLeast(2f),
                            center = Offset(px, py)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Panel: Player Hand
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "YOU",
                        color = ImmersiveLime,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .height(180.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val activeYOffset = if (isShaking) shakeOffset else 0f
                            val activeRotation = if (isShaking) 15f + shakeAngle else 15f
                            val currentScale = if (isShaking) 1.0f else revealScale.value

                            rotate(degrees = activeRotation, pivot = Offset(size.width * 0.4f, size.height * 0.6f)) {
                                drawHandGesture(
                                    centerX = size.width / 2f,
                                    centerY = (size.height / 2f) + activeYOffset,
                                    scale = currentScale,
                                    fingersCount = if (isShaking) 0 else playerChoice,
                                    handColor = ImmersiveLime,
                                    isLeftHand = true
                                )
                            }

                            // Score halo for Player
                            val progress = scoreProgress.value
                            if (isScoreEvent && playerRole == com.example.ui.viewmodel.PlayerRole.BATTING && progress > 0f && progress < 1f) {
                                drawCircle(
                                    color = ImmersiveLime.copy(alpha = 0.4f * (1f - progress)),
                                    radius = (60f + 120f * progress) * currentScale,
                                    center = Offset(size.width / 2f, size.height / 2f),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f * (1f - progress))
                                )
                            }
                        }

                        // Floating run text overlay
                        if (isScoreEvent && playerRole == com.example.ui.viewmodel.PlayerRole.BATTING && scoreProgress.value > 0f && scoreProgress.value < 1f) {
                            Text(
                                text = if (playerChoice == 6) "GLORIOUS 6!" else if (playerChoice == 4) "BOUNDARY 4!" else "+$playerChoice RUNS",
                                color = ImmersiveLime,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .graphicsLayer {
                                        translationY = -80f * scoreProgress.value
                                        alpha = 1f - scoreProgress.value
                                        scaleX = 1f + 0.5f * scoreProgress.value
                                        scaleY = 1f + 0.5f * scoreProgress.value
                                    },
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = when {
                            isShaking -> "WAITING"
                            playerChoice == 0 -> "TIMEOUT (0)"
                            else -> "PLAYED $playerChoice"
                        },
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Center Battle Divider Line
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.6f)
                        .width(1.dp)
                        .background(Color.White.copy(alpha = 0.1f))
                )

                // Right Panel: AI Hand
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "AI DEFENDER",
                        color = Color(0xFF81D4FA),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .height(180.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val activeYOffset = if (isShaking) -shakeOffset else 0f
                            val activeRotation = if (isShaking) -15f - shakeAngle else -15f
                            val currentScale = if (isShaking) 1.0f else revealScale.value

                            rotate(degrees = activeRotation, pivot = Offset(size.width * 0.6f, size.height * 0.6f)) {
                                drawHandGesture(
                                    centerX = size.width / 2f,
                                    centerY = (size.height / 2f) + activeYOffset,
                                    scale = currentScale,
                                    fingersCount = if (isShaking) 0 else aiChoice,
                                    handColor = Color(0xFF81D4FA),
                                    isLeftHand = false
                                )
                            }

                            // Score halo for AI
                            val progress = scoreProgress.value
                            if (isScoreEvent && playerRole == com.example.ui.viewmodel.PlayerRole.BOWLING && progress > 0f && progress < 1f) {
                                drawCircle(
                                    color = Color(0xFF81D4FA).copy(alpha = 0.4f * (1f - progress)),
                                    radius = (60f + 120f * progress) * currentScale,
                                    center = Offset(size.width / 2f, size.height / 2f),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f * (1f - progress))
                                )
                            }
                        }

                        // Floating run text overlay
                        if (isScoreEvent && playerRole == com.example.ui.viewmodel.PlayerRole.BOWLING && scoreProgress.value > 0f && scoreProgress.value < 1f) {
                            Text(
                                text = if (aiChoice == 6) "CRACKING 6!" else if (aiChoice == 4) "AI BOUNDARY 4!" else "+$aiChoice RUNS",
                                color = Color(0xFF81D4FA),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .graphicsLayer {
                                        translationY = -80f * scoreProgress.value
                                        alpha = 1f - scoreProgress.value
                                        scaleX = 1f + 0.5f * scoreProgress.value
                                        scaleY = 1f + 0.5f * scoreProgress.value
                                    },
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = when {
                            isShaking -> "DECIDING"
                            else -> "PLAYED $aiChoice"
                        },
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Draws a stylized comic-book style hand with dynamic fingers count.
 */
private fun DrawScope.drawHandGesture(
    centerX: Float,
    centerY: Float,
    scale: Float,
    fingersCount: Int,
    handColor: Color,
    isLeftHand: Boolean
) {
    val basePalmWidth = 60f * scale
    val basePalmHeight = 70f * scale
    val fingerThickness = 12f * scale
    val baseFingerLength = 65f * scale

    // 1. Draw Wrist/Forearm base (slanted rectangle)
    val wristPath = androidx.compose.ui.graphics.Path().apply {
        val wStartX = if (isLeftHand) centerX - basePalmWidth * 0.4f else centerX - basePalmWidth * 0.2f
        val wEndX = if (isLeftHand) centerX + basePalmWidth * 0.2f else centerX + basePalmWidth * 0.4f
        moveTo(wStartX, centerY + basePalmHeight * 0.8f)
        lineTo(wEndX, centerY + basePalmHeight * 0.8f)
        lineTo(centerX + (if (isLeftHand) -basePalmWidth * 0.1f else basePalmWidth * 0.1f), centerY + basePalmHeight * 1.5f)
        lineTo(centerX - (if (isLeftHand) basePalmWidth * 0.1f else basePalmWidth * 0.1f), centerY + basePalmHeight * 1.5f)
        close()
    }
    drawPath(
        path = wristPath,
        brush = Brush.verticalGradient(
            colors = listOf(handColor.copy(alpha = 0.6f), Color.Transparent)
        )
    )

    // 2. Draw Palm base (Rounded Rectangle)
    drawRoundRect(
        color = handColor,
        topLeft = Offset(centerX - (basePalmWidth / 2f), centerY - (basePalmHeight / 2f)),
        size = Size(basePalmWidth, basePalmHeight),
        cornerRadius = CornerRadius(20f * scale, 20f * scale)
    )

    // 3. Draw fingers based on selection
    if (fingersCount == 0) {
        // Draw closed fist: Folded finger caps (dots on the palm top edge)
        val numFolded = 4
        val startX = centerX - basePalmWidth * 0.3f
        val stepX = basePalmWidth * 0.6f / (numFolded - 1)
        for (i in 0 until numFolded) {
            val fX = startX + (i * stepX)
            val fY = centerY - basePalmHeight * 0.42f
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = fingerThickness * 0.7f,
                center = Offset(fX, fY)
            )
        }
        // Draw folded thumb
        val thumbX = if (isLeftHand) centerX + basePalmWidth * 0.25f else centerX - basePalmWidth * 0.25f
        val thumbY = centerY + basePalmHeight * 0.1f
        drawCircle(
            color = Color.White.copy(alpha = 0.3f),
            radius = fingerThickness * 0.8f,
            center = Offset(thumbX, thumbY)
        )
    } else {
        // Draw extended fingers
        // Finger indices mapping:
        // index (i=0), middle (i=1), ring (i=2), pinky (i=3)
        // thumb is drawn separately
        
        val fingerSpacing = basePalmWidth * 0.7f / 3
        val fingerStartX = centerX - (basePalmWidth * 0.35f)
        
        // Count how many normal fingers to extend (up to 4)
        val normalFingersToExtend = if (fingersCount >= 5) 4 else (fingersCount - 1).coerceAtLeast(0)
        
        // Always draw the index finger if selection >= 1
        val indexExtended = fingersCount >= 1
        
        // Draw Index Finger (i=0)
        if (indexExtended) {
            drawFinger(
                startX = fingerStartX,
                startY = centerY - basePalmHeight * 0.4f,
                length = baseFingerLength * 1.0f,
                thickness = fingerThickness,
                color = handColor,
                angleDeg = -5f
            )
        } else {
            drawFoldedFingerDot(fingerStartX, centerY - basePalmHeight * 0.4f, fingerThickness)
        }

        // Draw Middle Finger (i=1)
        if (fingersCount >= 2) {
            drawFinger(
                startX = fingerStartX + fingerSpacing,
                startY = centerY - basePalmHeight * 0.45f,
                length = baseFingerLength * 1.1f, // longest
                thickness = fingerThickness,
                color = handColor,
                angleDeg = 0f
            )
        } else {
            drawFoldedFingerDot(fingerStartX + fingerSpacing, centerY - basePalmHeight * 0.45f, fingerThickness)
        }

        // Draw Ring Finger (i=2)
        if (fingersCount >= 3) {
            drawFinger(
                startX = fingerStartX + 2 * fingerSpacing,
                startY = centerY - basePalmHeight * 0.43f,
                length = baseFingerLength * 1.05f,
                thickness = fingerThickness,
                color = handColor,
                angleDeg = 5f
            )
        } else {
            drawFoldedFingerDot(fingerStartX + 2 * fingerSpacing, centerY - basePalmHeight * 0.43f, fingerThickness)
        }

        // Draw Pinky Finger (i=3)
        if (fingersCount >= 4) {
            drawFinger(
                startX = fingerStartX + 3 * fingerSpacing,
                startY = centerY - basePalmHeight * 0.38f,
                length = baseFingerLength * 0.85f, // shortest
                thickness = fingerThickness,
                color = handColor,
                angleDeg = 12f
            )
        } else {
            drawFoldedFingerDot(fingerStartX + 3 * fingerSpacing, centerY - basePalmHeight * 0.38f, fingerThickness)
        }

        // Draw Thumb (based on hand orientation)
        val isThumbExtended = fingersCount >= 5
        val thumbStartX = if (isLeftHand) centerX + basePalmWidth * 0.42f else centerX - basePalmWidth * 0.42f
        val thumbStartY = centerY + basePalmHeight * 0.05f
        
        if (isThumbExtended) {
            // Extended thumb draws outwards sideways
            val thumbAngle = if (isLeftHand) 50f else -50f
            drawFinger(
                startX = thumbStartX,
                startY = thumbStartY,
                length = baseFingerLength * 0.75f,
                thickness = fingerThickness * 1.1f,
                color = handColor,
                angleDeg = thumbAngle
            )
        } else {
            // Folded thumb dot
            drawFoldedFingerDot(thumbStartX, thumbStartY, fingerThickness * 1.1f)
        }

        // Handle case 6: Draw double thumb/extra visual glow on top of 5 fingers
        if (fingersCount == 6) {
            // Draw extra glowing ring around the open hand
            drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                radius = baseFingerLength * 1.3f,
                center = Offset(centerX, centerY - basePalmHeight * 0.1f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 4f * scale,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(15f, 15f), 0f
                    )
                )
            )
        }
    }
}

private fun DrawScope.drawFinger(
    startX: Float,
    startY: Float,
    length: Float,
    thickness: Float,
    color: Color,
    angleDeg: Float
) {
    rotate(angleDeg, pivot = Offset(startX, startY)) {
        drawLine(
            color = color,
            start = Offset(startX, startY),
            end = Offset(startX, startY - length),
            strokeWidth = thickness,
            cap = StrokeCap.Round
        )
        // Finger tip white highlight shine
        drawLine(
            color = Color.White.copy(alpha = 0.4f),
            start = Offset(startX, startY - length * 0.4f),
            end = Offset(startX, startY - length * 0.9f),
            strokeWidth = thickness * 0.3f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawFoldedFingerDot(
    x: Float,
    y: Float,
    thickness: Float
) {
    drawCircle(
        color = Color.Black.copy(alpha = 0.2f),
        radius = thickness * 0.5f,
        center = Offset(x, y + 5f)
    )
}
