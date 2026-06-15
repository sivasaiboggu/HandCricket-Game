package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ImmersiveBackground
import com.example.ui.theme.ImmersiveBorder
import com.example.ui.theme.ImmersiveLime
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.viewmodel.GameViewModel
import com.example.ui.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    viewModel: GameViewModel,
    onNavigateToSignUp: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Google Sign-In helper state
    var showGoogleDialog by remember { mutableStateOf(false) }
    var googleNickname by remember { mutableStateOf("") }
    var googleEmail by remember { mutableStateOf("") }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(ImmersiveBackground, ImmersiveSurface)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // App Logo / Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = "ATHLETE PORTAL",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "SIGN IN TO PLAY ONLINE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ImmersiveLime,
                    letterSpacing = 3.sp
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = ImmersiveSurface),
                border = BorderStroke(1.dp, ImmersiveBorder),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address", color = Color.White.copy(alpha = 0.6f)) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = ImmersiveLime) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ImmersiveLime,
                            unfocusedBorderColor = ImmersiveBorder
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Secret Password", color = Color.White.copy(alpha = 0.6f)) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = ImmersiveLime) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ImmersiveLime,
                            unfocusedBorderColor = ImmersiveBorder
                        ),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Sign In Button
                    Button(
                        onClick = {
                            if (email.isEmpty() || password.isEmpty()) {
                                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isLoading = true
                            viewModel.signInWithEmail(
                                email = email.trim(),
                                password = password.trim(),
                                onSuccess = {
                                    isLoading = false
                                    Toast.makeText(context, "Welcome back!", Toast.LENGTH_SHORT).show()
                                },
                                onError = { error ->
                                    isLoading = false
                                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ImmersiveLime,
                            contentColor = ImmersiveBackground
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = ImmersiveBackground, modifier = Modifier.size(24.dp))
                        } else {
                            Text("SIGN IN", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            // Google Authentication Section
            Text(
                text = "— OR CONNECT WITH —",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Google Button (Interactive dialog launcher)
            Button(
                onClick = { showGoogleDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                border = BorderStroke(1.dp, Color.LightGray),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "SIGN IN WITH GOOGLE",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Play Offline Button (Bypasses online registration)
            Button(
                onClick = {
                    viewModel.playOfflineMode()
                    Toast.makeText(context, "Offline AI Mode Activated", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color.White
                ),
                border = BorderStroke(1.dp, ImmersiveBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ImmersiveLime)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PLAY OFFLINE (AI MODE ONLY)", fontWeight = FontWeight.Bold)
                }
            }

            // Register Link
            TextButton(onClick = onNavigateToSignUp) {
                Text(
                    text = "New Athlete? Register New Account Here",
                    color = ImmersiveLime,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }

    // Google Sign-In simulated dialog setup
    if (showGoogleDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleDialog = false },
            title = { Text("Google Account Setup", fontWeight = FontWeight.Bold, color = Color.White) },
            containerColor = ImmersiveSurface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Simulating Google Play Services Auth connection. Please provide nickname & email to sync profile with database:",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    OutlinedTextField(
                        value = googleNickname,
                        onValueChange = { googleNickname = it },
                        label = { Text("Athlete Nickname") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = googleEmail,
                        onValueChange = { googleEmail = it },
                        label = { Text("Google Email") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (googleNickname.trim().length < 3 || googleEmail.isEmpty()) {
                            Toast.makeText(context, "Invalid nickname or email", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        showGoogleDialog = false
                        isLoading = true
                        viewModel.signInWithGoogleSimulated(
                            username = googleNickname.trim(),
                            email = googleEmail.trim(),
                            onSuccess = {
                                isLoading = false
                                Toast.makeText(context, "Google Auth Connected!", Toast.LENGTH_SHORT).show()
                            },
                            onError = { error ->
                                isLoading = false
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ImmersiveLime)
                ) {
                    Text("CONNECT ACCOUNT", color = ImmersiveBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleDialog = false }) {
                    Text("CANCEL", color = Color.White)
                }
            }
        )
    }
}
