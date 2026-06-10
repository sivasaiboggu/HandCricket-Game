package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import kotlin.math.cos

@Composable
fun TossCoinFlipper(
    isSpinning: Boolean,
    result: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Coin Glow")
    val neonPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "NeonPulse"
    )

    // Spin angle animation
    val spinTransition = rememberInfiniteTransition(label = "Spin")
    val spinAngle by spinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpinAngle"
    )

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val centerY = height / 2f
            val radius = minOf(width, height) * 0.4f

            val currentScaleY = if (isSpinning) {
                // Generate a squeeze scale that mimics real 3D coin flipping!
                val radians = Math.toRadians(spinAngle.toDouble())
                cos(radians).toFloat()
            } else {
                1.0f
            }

            // Draw coin shadow
            drawOval(
                color = Color.Black.copy(alpha = 0.25f),
                topLeft = Offset(centerX - radius, centerY + (radius * 0.8f)),
                size = Size(radius * 2f, radius * 0.3f)
            )

            // Draw Coin Body (With simulated 3D thickness)
            val thickness = 10f * currentScaleY
            
            // Core coin surface with gold gradient
            val coinGoldBrush = Brush.radialGradient(
                colors = if (isSpinning) {
                    listOf(Color(0xFFFFF176), Color(0xFFFBC02D))
                } else {
                    if (result == "TAILS") {
                        listOf(Color(0xFF00E5FF), Color(0xFF0D47A1)) // Tails is futuristic Cyan/Blue
                    } else if (result == "HEADS") {
                        listOf(Color(0xFFFFD54F), Color(0xFFE65100)) // Heads is Sunset Gold/Orange
                    } else {
                        listOf(Color(0xFFFFEE58), Color(0xFFFBC02D)) // Normal gold
                    }
                },
                center = Offset(centerX, centerY),
                radius = radius
            )

            // Draw Coin face (using native 3D scaling)
            val heightOffset = radius * currentScaleY
            drawOval(
                brush = coinGoldBrush,
                topLeft = Offset(centerX - radius, centerY - heightOffset),
                size = Size(radius * 2f, heightOffset * 2f)
            )

            // Outer metallic rim
            val rimColor = if (isSpinning) Color(0xFFFFD54F) else {
                if (result == "TAILS") Color(0xFF80DEEA) else Color(0xFFFFE082)
            }
            drawOval(
                color = rimColor.copy(alpha = 0.8f + (0.2f * neonPulse)),
                topLeft = Offset(centerX - radius, centerY - heightOffset),
                size = Size(radius * 2f, heightOffset * 2f),
                style = Stroke(width = 6f)
            )

            // Inner design text representations on Canvas
            val letter = if (isSpinning) {
                if ((spinAngle / 90).toInt() % 2 == 0) "H" else "T"
            } else {
                if (result == "HEADS") "H" else if (result == "TAILS") "T" else ""
            }

            if (letter.isNotEmpty()) {
                val textScaleY = if (isSpinning) currentScaleY.coerceAtLeast(0.01f) else 1.0f
                scale(scaleX = 1f, scaleY = textScaleY, pivot = Offset(centerX, centerY)) {
                    drawContext.canvas.nativeCanvas.drawText(
                        letter,
                        centerX,
                        centerY + (radius * 0.3f),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = radius * 0.9f
                            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }

            if (!isSpinning && result.isNotEmpty()) {
                val textColor = Color.White
                // Draw inner circle decoration
                drawOval(
                    color = textColor.copy(alpha = 0.3f),
                    topLeft = Offset(centerX - (radius * 0.8f), centerY - (heightOffset * 0.8f)),
                    size = Size(radius * 1.6f, heightOffset * 1.6f),
                    style = Stroke(width = 2f)
                )
            }
        }
    }
}
