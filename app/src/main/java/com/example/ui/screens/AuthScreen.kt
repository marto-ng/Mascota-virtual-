package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuthScreen(
    onLogin: (String, String, (Boolean) -> Unit) -> Unit, // email, password, onResult
    onRegister: (String, String, String, (Boolean, String?) -> Unit) -> Unit, // username, email, password, onResult
    modifier: Modifier = Modifier
) {
    var isSignUpTab by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var infoMsg by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val containerBgBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F172A), // Slate 900
            Color(0xFF1E1B4B)  // Indigo 950
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(containerBgBrush)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.9f)),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(2.dp, Color(0xFFB082FF)),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Game Station style hatch logo
                Text(
                    text = "👾 MOCHI PETS 👾",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFD54F),
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Hatchery Inteligente Terrestre",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Custom Tab Switch
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Button(
                            onClick = {
                                isSignUpTab = false
                                errorMsg = null
                                infoMsg = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isSignUpTab) Color(0xFFB082FF) else Color.Transparent,
                                contentColor = if (!isSignUpTab) Color.White else Color(0xFF94A3B8)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(2.dp)
                        ) {
                            Text("Iniciar Sesión", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                isSignUpTab = true
                                errorMsg = null
                                infoMsg = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSignUpTab) Color(0xFFB082FF) else Color.Transparent,
                                contentColor = if (isSignUpTab) Color.White else Color(0xFF94A3B8)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(2.dp)
                        ) {
                            Text("Crear Cuenta", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Input fields
                if (isSignUpTab) {
                    // Registration contains Username, Email, Password, Confirm Password
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            errorMsg = null
                        },
                        label = { Text("Nombre de usuario", color = Color(0xFF94A3B8)) },
                        placeholder = { Text("Ej: martin12", color = Color.Gray) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFFB082FF)
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0x1A000000),
                            unfocusedContainerColor = Color(0x1A000000),
                            focusedIndicatorColor = Color(0xFFB082FF)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("auth_username_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Email field is shown in both login and signup
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMsg = null
                    },
                    label = { Text("Email", color = Color(0xFF94A3B8)) },
                    placeholder = { Text("Ej: usuario@correo.com", color = Color.Gray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = Color(0xFFB082FF)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0x1A000000),
                        unfocusedContainerColor = Color(0x1A000000),
                        focusedIndicatorColor = Color(0xFFB082FF)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("auth_email_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Password field is shown in both login and signup
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMsg = null
                    },
                    label = { Text("Contraseña", color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFB082FF)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0x1A000000),
                        unfocusedContainerColor = Color(0x1A000000),
                        focusedIndicatorColor = Color(0xFFB082FF)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("auth_password_input")
                )

                if (isSignUpTab) {
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            errorMsg = null
                        },
                        label = { Text("Confirmar Contraseña", color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFB082FF)
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0x1A000000),
                            unfocusedContainerColor = Color(0x1A000000),
                            focusedIndicatorColor = Color(0xFFB082FF)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("auth_confirm_password_input")
                    )
                }

                // Messages UI info/error
                AnimatedVisibility(
                    visible = errorMsg != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    errorMsg?.let { msg ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .background(Color(0x33FF5252), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Error",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(msg, color = Color(0xFFFF7171), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                AnimatedVisibility(
                    visible = infoMsg != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    infoMsg?.let { msg ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .background(Color(0x3381C784), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = Color(0xFF81C784),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(msg, color = Color(0xFFA5D6A7), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Submit Button
                Button(
                    onClick = {
                        val cleanEmail = email.trim()
                        val cleanPassword = password

                        if (isSignUpTab) {
                            val cleanUser = username.trim()
                            if (cleanUser.isEmpty() || cleanEmail.isEmpty() || cleanPassword.isEmpty() || confirmPassword.isEmpty()) {
                                errorMsg = "Por favor, completa todos los campos vacíos."
                                return@Button
                            }
                            if (cleanPassword != confirmPassword) {
                                errorMsg = "Las contraseñas no coinciden."
                                return@Button
                            }
                            isLoading = true
                            errorMsg = null
                            infoMsg = null

                            onRegister(cleanUser, cleanEmail, cleanPassword) { success, error ->
                                isLoading = false
                                if (success) {
                                    infoMsg = "¡Registro exitoso! Iniciando sesión..."
                                } else {
                                    errorMsg = error ?: "Ocurrió un error en el registro."
                                }
                            }
                        } else {
                            if (cleanEmail.isEmpty() || cleanPassword.isEmpty()) {
                                errorMsg = "Por favor, ingresa tu correo electrónico y contraseña."
                                return@Button
                            }
                            isLoading = true
                            errorMsg = null
                            infoMsg = null

                            onLogin(cleanEmail, cleanPassword) { success ->
                                isLoading = false
                                if (!success) {
                                    errorMsg = "Correo electrónico o contraseña incorrectos."
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB082FF)),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("auth_submit_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = if (isSignUpTab) "CREAR CUENTA 🧪" else "ACCEDER 🚀",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
