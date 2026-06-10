package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.StadiumTheme
import kotlin.math.sin

@Composable
fun StadiumPitchVisualizer(
    stadiumTheme: StadiumTheme,
    isOut: Boolean,
    isScore: Boolean,
    lastScore: Int,
    ballsBowled: Int,
    modifier: Modifier = Modifier
) {
    // Animation transitions for dynamic events
    val transition = rememberInfiniteTransition(label = "Stadium Lights")
    val ambientPulse by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    // Ball flight animation triggers
    val ballFlightAnim = remember { Animatable(0f) }
    LaunchedEffect(isScore, lastScore) {
        if (isScore && lastScore > 0) {
            ballFlightAnim.snapTo(0f)
            ballFlightAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(800, easing = FastOutSlowInEasing)
            )
        } else {
            ballFlightAnim.snapTo(0f)
        }
    }

    // Stumps rattle animation triggers
    val outRattleAnim = remember { Animatable(0f) }
    LaunchedEffect(isOut) {
        if (isOut) {
            outRattleAnim.snapTo(0f)
            outRattleAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(500, easing = FastOutLinearInEasing)
            )
        } else {
            outRattleAnim.snapTo(0f)
        }
    }

    // Capture Stadium Theme Palette
    val colors = when (stadiumTheme) {
        StadiumTheme.CLASSIC_TURF -> StadiumColors(
            fieldGradStart = Color(0xFF1B5E20),
            fieldGradEnd = Color(0xFF388E3C),
            pitchColor = Color(0xFFD7A15C),
            stumpsColor = Color(0xFFF1F1F1),
            bailsColor = Color(0xFFE53935),
            ambientLight = Color(0x20A5D6A7)
        )
        StadiumTheme.SUNSET_LAGOON -> StadiumColors(
            fieldGradStart = Color(0xFF2E114D),
            fieldGradEnd = Color(0xFFFF5E62),
            pitchColor = Color(0xFFE0A96D),
            stumpsColor = Color(0xFFFFCC80),
            bailsColor = Color(0xFFFF6F00),
            ambientLight = Color(0x30FF9E00)
        )
        StadiumTheme.CYBER_ARENA -> StadiumColors(
            fieldGradStart = Color(0xFF0F051D),
            fieldGradEnd = Color(0xFF2D004F),
            pitchColor = Color(0xFF120024),
            stumpsColor = Color(0xFF00E5FF),
            bailsColor = Color(0xFFFF007F),
            ambientLight = Color(0x4039FF14)
        )
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Draw Stadium Boundary Oval and Field Grass stripes
            drawOutfield(width, height, colors, stadiumTheme, ambientPulse)

            // 2. Draw Pitch Clay / Synthetic strip in perspective
            drawPitch(width, height, colors, stadiumTheme)

            // 3. Draw Crease Lines
            drawCreases(width, height)

            // 4. Draw Stumps & Bails (with Out breaking physics)
            drawStumps(width, height, colors, outRattleAnim.value, isOut)

            // 5. Draw Animated Cricket Ball Flight if runs scored
            if (isScore && lastScore > 0) {
                drawCricketBallFlight(width, height, ballFlightAnim.value, lastScore, colors)
            }
        }
    }
}

private class StadiumColors(
    val fieldGradStart: Color,
    val fieldGradEnd: Color,
    val pitchColor: Color,
    val stumpsColor: Color,
    val bailsColor: Color,
    val ambientLight: Color
)

private fun DrawScope.drawOutfield(
    width: Float,
    height: Float,
    colors: StadiumColors,
    theme: StadiumTheme,
    pulse: Float
) {
    // Radial Gradient to give dynamic studio glow depth
    val glowCenter = Offset(width / 2f, height * 0.4f)
    val outerColor = if (theme == StadiumTheme.CYBER_ARENA) Color(0xFF02010A) else colors.fieldGradStart
    
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(colors.fieldGradEnd, outerColor),
            center = glowCenter,
            radius = width * 0.8f
        ),
        size = Size(width, height)
    )

    // Stadium lighting effects
    if (theme == StadiumTheme.CYBER_ARENA) {
        // Neon boundary laser rings
        drawOval(
            color = colors.stumpsColor.copy(alpha = 0.4f * pulse),
            topLeft = Offset(width * 0.05f, height * 0.1f),
            size = Size(width * 0.9f, height * 0.8f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
        )
        drawOval(
            color = colors.bailsColor.copy(alpha = 0.3f * (1f - pulse)),
            topLeft = Offset(width * 0.02f, height * 0.05f),
            size = Size(width * 0.96f, height * 0.88f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )
    } else {
        // Traditional boundary rope
        drawOval(
            color = Color.White.copy(alpha = 0.5f),
            topLeft = Offset(width * 0.08f, height * 0.15f),
            size = Size(width * 0.84f, height * 0.7f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
        )
    }
}

private fun DrawScope.drawPitch(
    width: Float,
    height: Float,
    colors: StadiumColors,
    theme: StadiumTheme
) {
    // Clay pitch in perspective (tapered towards top)
    val pitchPath = Path().apply {
        moveTo(width * 0.42f, height * 0.2f) // Top Left
        lineTo(width * 0.58f, height * 0.2f) // Top Right
        lineTo(width * 0.65f, height * 0.85f) // Bottom Right
        lineTo(width * 0.35f, height * 0.85f) // Bottom Left
        close()
    }

    if (theme == StadiumTheme.CYBER_ARENA) {
        // Pulsing synth grid pitch
        drawPath(
            path = pitchPath,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF1E0B36), Color(0xFF0F031E))
            )
        )
        // Draw grid lines inside pitch
        // We can draw simple boundaries
        drawPath(
            path = pitchPath,
            color = colors.stumpsColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
        )
    } else {
        // Natural dust/soil pitch block
        drawPath(
            path = pitchPath,
            color = colors.pitchColor
        )
        // Add grass friction marks on the sides
        drawPath(
            path = pitchPath,
            color = Color.Black.copy(alpha = 0.06f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
        )
    }
}

private fun DrawScope.drawCreases(width: Float, height: Float) {
    // Batsman crease (Bottom)
    drawLine(
        color = Color.White.copy(alpha = 0.8f),
        start = Offset(width * 0.37f, height * 0.72f),
        end = Offset(width * 0.63f, height * 0.72f),
        strokeWidth = 3f
    )

    // Bowling crease (Top)
    drawLine(
        color = Color.White.copy(alpha = 0.8f),
        start = Offset(width * 0.43f, height * 0.28f),
        end = Offset(width * 0.57f, height * 0.28f),
        strokeWidth = 2f
    )
}

private fun DrawScope.drawStumps(
    width: Float,
    height: Float,
    colors: StadiumColors,
    rattleProgress: Float,
    isOut: Boolean
) {
    // Draw Stumps at the bowling crease (Top section of the pitch)
    val baseCenterX = width * 0.5f
    val baseCenterY = height * 0.26f
    
    val stumpHeight = height * 0.07f
    val stumpWidth = width * 0.008f
    val stumpSpacing = width * 0.015f

    // We draw 3 stumps: Left, Middle, Right
    if (isOut && rattleProgress > 0f) {
        // Out rattling animations: stumps fly or rotate!
        // Stumps fly apart
        val leftFlyX = -rattleProgress * 25f
        val leftRot = -rattleProgress * 30f

        val rightFlyX = rattleProgress * 25f
        val rightRot = rattleProgress * 30f

        val midFlyY = -rattleProgress * 15f
        val midRot = rattleProgress * 10f

        // Draw Left Stump (flying left)
        rotate(leftRot, pivot = Offset(baseCenterX - stumpSpacing, baseCenterY)) {
            drawRoundRect(
                color = colors.stumpsColor,
                topLeft = Offset(baseCenterX - stumpSpacing - (stumpWidth / 2f) + leftFlyX, baseCenterY - stumpHeight),
                size = Size(stumpWidth, stumpHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }

        // Draw Middle Stump (moving up/shaking)
        rotate(midRot, pivot = Offset(baseCenterX, baseCenterY)) {
            drawRoundRect(
                color = colors.stumpsColor,
                topLeft = Offset(baseCenterX - (stumpWidth / 2f), baseCenterY - stumpHeight + midFlyY),
                size = Size(stumpWidth, stumpHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }

        // Draw Right Stump (flying right)
        rotate(rightRot, pivot = Offset(baseCenterX + stumpSpacing, baseCenterY)) {
            drawRoundRect(
                color = colors.stumpsColor,
                topLeft = Offset(baseCenterX + stumpSpacing - (stumpWidth / 2f) + rightFlyX, baseCenterY - stumpHeight),
                size = Size(stumpWidth, stumpHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }

        // Bails flying in vectors
        val bailHeight = height * 0.005f
        val bailWidth = stumpSpacing * 1.1f
        val leftBailFlyX = -rattleProgress * 35f
        val leftBailFlyY = -rattleProgress * 45f
        val rightBailFlyX = rattleProgress * 35f
        val rightBailFlyY = -rattleProgress * 55f

        // Draw Left Bail (shaking off to left-sky)
        rotate(-rattleProgress * 120f, pivot = Offset(baseCenterX - (stumpSpacing / 2f), baseCenterY - stumpHeight)) {
            drawRoundRect(
                color = colors.bailsColor,
                topLeft = Offset(baseCenterX - stumpSpacing + leftBailFlyX, baseCenterY - stumpHeight - (stumpHeight * 0.1f) + leftBailFlyY),
                size = Size(bailWidth, bailHeight),
                cornerRadius = CornerRadius(2f, 2f)
            )
        }

        // Draw Right Bail (shaking off to right-sky)
        rotate(rattleProgress * 140f, pivot = Offset(baseCenterX + (stumpSpacing / 2f), baseCenterY - stumpHeight)) {
            drawRoundRect(
                color = colors.bailsColor,
                topLeft = Offset(baseCenterX + (stumpSpacing * 0.1f) + rightBailFlyX, baseCenterY - stumpHeight - (stumpHeight * 0.15f) + rightBailFlyY),
                size = Size(bailWidth, bailHeight),
                cornerRadius = CornerRadius(2f, 2f)
            )
        }

    } else {
        // normal undisturbed stumps state
        // Left
        drawRoundRect(
            color = colors.stumpsColor,
            topLeft = Offset(baseCenterX - stumpSpacing - (stumpWidth / 2f), baseCenterY - stumpHeight),
            size = Size(stumpWidth, stumpHeight),
            cornerRadius = CornerRadius(4f, 4f)
        )
        // Mid
        drawRoundRect(
            color = colors.stumpsColor,
            topLeft = Offset(baseCenterX - (stumpWidth / 2f), baseCenterY - stumpHeight),
            size = Size(stumpWidth, stumpHeight),
            cornerRadius = CornerRadius(4f, 4f)
        )
        // Right
        drawRoundRect(
            color = colors.stumpsColor,
            topLeft = Offset(baseCenterX + stumpSpacing - (stumpWidth / 2f), baseCenterY - stumpHeight),
            size = Size(stumpWidth, stumpHeight),
            cornerRadius = CornerRadius(4f, 4f)
        )

        // Bails
        val bailHeight = height * 0.005f
        val bailWidth = stumpSpacing * 1.1f
        // Let's connect Left-Mid and Mid-Right Stumps
        drawRoundRect(
            color = colors.bailsColor,
            topLeft = Offset(baseCenterX - stumpSpacing, baseCenterY - stumpHeight - bailHeight),
            size = Size(bailWidth, bailHeight),
            cornerRadius = CornerRadius(2f, 2f)
        )
        drawRoundRect(
            color = colors.bailsColor,
            topLeft = Offset(baseCenterX + (stumpSpacing * 0.05f), baseCenterY - stumpHeight - bailHeight),
            size = Size(bailWidth, bailHeight),
            cornerRadius = CornerRadius(2f, 2f)
        )
    }
}

private fun DrawScope.drawCricketBallFlight(
    width: Float,
    height: Float,
    progress: Float,
    runs: Int,
    colors: StadiumColors
) {
    // Batsman swings at bottom, ball flies upwards based on 'runs' scored!
    val startX = width * 0.5f
    val startY = height * 0.72f // Batting crease

    // Flight configuration: higher runs = higher and longer launch!
    val maxTravelX = when (runs) {
        6 -> width * 0.35f * sin(progress * 3.14f)
        4 -> -width * 0.38f * progress
        3 -> width * 0.15f * progress
        2 -> -width * 0.1f * progress
        1 -> width * 0.05f * progress
        else -> 0f
    }

    val finalTargetY = when (runs) {
        6 -> height * 0.08f // Into the deep arena crowd!
        4 -> height * 0.12f // Over boundary lines
        3 -> height * 0.35f // Deep gaps
        2 -> height * 0.45f
        1 -> height * 0.55f
        else -> startY
    }

    // Parabolic motion calculation
    val currentX = startX + (maxTravelX * progress)
    val linearY = startY + (finalTargetY - startY) * progress
    val arcHeight = height * when (runs) {
        6 -> 0.3f
        4 -> 0.15f
        3 -> 0.12f
        2 -> 0.08f
        1 -> 0.04f
        else -> 0f
    }
    // Parabola offset using sine factor: sin(pi * x)
    val currentY = linearY - (arcHeight * sin(progress * 3.14159f))

    // Draw trajectory trail
    for (i in 1..10) {
        val trailP = progress * (i / 10f)
        val tX = startX + (maxTravelX * trailP)
        val tLinearY = startY + (finalTargetY - startY) * trailP
        val tY = tLinearY - (arcHeight * sin(trailP * 3.14159f))
        
        drawCircle(
            color = colors.bailsColor.copy(alpha = 0.3f * trailP),
            radius = 6f * trailP,
            center = Offset(tX, tY)
        )
    }

    // Draw main glowing cricket ball (Red or Neon Pink depending on theme)
    val ballColor = if (colors.bailsColor == Color.White) Color.Yellow else colors.bailsColor
    
    // Ball Shadow (Projected on pitch below)
    drawCircle(
        color = Color.Black.copy(alpha = 0.25f),
        radius = (12f * (1f - (sin(progress * 3.14f) * 0.5f))),
        center = Offset(currentX, linearY)
    )

    // Ball itself
    drawCircle(
        color = ballColor,
        radius = 14f,
        center = Offset(currentX, currentY)
    )
    // Ball core shine
    drawCircle(
        color = Color.White.copy(alpha = 0.7f),
        radius = 5f,
        center = Offset(currentX - 4f, currentY - 4f)
    )
}
