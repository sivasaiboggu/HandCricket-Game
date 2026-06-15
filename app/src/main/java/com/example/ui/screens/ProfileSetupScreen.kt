package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ImmersiveBackground
import com.example.ui.theme.ImmersiveBorder
import com.example.ui.theme.ImmersiveLime
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(ImmersiveBackground, ImmersiveSurface)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup Athlete Profile", fontWeight = FontWeight.Bold, color = Color.White) },
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
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ImmersiveSurface),
                border = BorderStroke(1.dp, ImmersiveBorder),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(ImmersiveLime.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsCricket,
                            contentDescription = "Athlete Profile",
                            tint = ImmersiveLime,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Text(
                        text = "CHOOSE YOUR ATHLETE NAME",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = ImmersiveLime,
                        letterSpacing = 1.5.sp,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Enter a nickname to identify yourself on the scoreboard in real-time online matches.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { newValue ->
                            // Clean up immediately: limit to 12 characters and filter space
                            if (newValue.length <= 12) {
                                username = newValue.filter { it.isLetterOrDigit() }
                                errorText = null
                            }
                        },
                        label = { Text("Athlete Username", color = Color.White.copy(alpha = 0.4f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ImmersiveLime,
                            unfocusedBorderColor = ImmersiveBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input_field"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (errorText != null) {
                        Text(
                            text = errorText ?: "",
                            color = Color(0xFFEF4444),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val trimmedName = username.trim()
                            if (trimmedName.length < 3) {
                                errorText = "Username must be at least 3 characters."
                            } else {
                                viewModel.saveUsernameAndMatch(trimmedName)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_username_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ImmersiveLime,
                            contentColor = ImmersiveBackground
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("CONFIRM & START SEARCH", fontWeight = FontWeight.Black, color = ImmersiveBackground)
                    }
                }
            }
        }
    }
}
