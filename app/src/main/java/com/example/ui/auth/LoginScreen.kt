package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController, viewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    
    val authState by viewModel.authState.collectAsState()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    
    // For password reset state
    var showForgotPassword by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            viewModel.resetState()
            navController.navigate("main?tab=home") {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        } else if (authState is AuthState.Error) {
            val message = (authState as AuthState.Error).message
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.resetState()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // Logo
            Image(
                painter = painterResource(id = R.drawable.opened_book_logo_1782632258077),
                contentDescription = "Aura Learning Logo",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Welcome Text
            Text(
                text = "Welcome to Aura Learning",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Learn smarter. Grow better.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            if (showForgotPassword) {
                // Forgot Password Flow
                Text(
                    text = "Reset Password",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = email,
                    onValueChange = { 
                        email = it
                        emailError = null
                    },
                    label = { Text("Email Address") },
                    placeholder = { Text("Enter your email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    isError = emailError != null,
                    supportingText = {
                        if (emailError != null) {
                            Text(text = emailError!!, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        if (email.isBlank()) {
                            emailError = "Please enter your email address."
                            return@Button
                        }
                        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            emailError = "Please enter a valid email."
                            return@Button
                        }
                        focusManager.clearFocus()
                        viewModel.sendPasswordReset(email)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = authState !is AuthState.Loading
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Send Reset Link", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(onClick = { showForgotPassword = false }) {
                    Text("Back to Login", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            } else {
                // Login Flow
                OutlinedTextField(
                    value = email,
                    onValueChange = { 
                        email = it
                        emailError = null
                    },
                    label = { Text("Email Address") },
                    placeholder = { Text("Enter your email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    isError = emailError != null,
                    supportingText = {
                        if (emailError != null) {
                            Text(text = emailError!!, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        passwordError = null
                    },
                    label = { Text("Password") },
                    placeholder = { Text("Enter your password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        val description = if (passwordVisible) "Hide password" else "Show password"
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = description, tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { 
                            focusManager.clearFocus()
                            // trigger login
                        }
                    ),
                    isError = passwordError != null,
                    supportingText = {
                        if (passwordError != null) {
                            Text(text = passwordError!!, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
                
                // Forgot Password Link
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    TextButton(onClick = { 
                        showForgotPassword = true 
                        viewModel.resetState()
                    }) {
                        Text("Forgot Password?", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        var isValid = true
                        if (email.isBlank()) {
                            emailError = "Please enter your email address."
                            isValid = false
                        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            emailError = "Please enter a valid email."
                            isValid = false
                        }
                        
                        if (password.isBlank()) {
                            passwordError = "Please enter your password."
                            isValid = false
                        }
                        
                        if (isValid) {
                            focusManager.clearFocus()
                            viewModel.login(email, password)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = authState !is AuthState.Loading
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Login", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Don't have an account?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Sign Up",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            viewModel.resetState()
                            navController.navigate("register") 
                        }
                    )
                }
            }
            
            // Error message display removed in favor of Snackbar
            
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(32.dp))
            
            // Bottom sections
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "By continuing, you agree to our",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row {
                    Text(
                        text = "Terms of Use",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            // TODO: Navigate to Terms
                        }
                    )
                    Text(
                        text = " and ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            // TODO: Navigate to Privacy
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Powered by Aura Community Act",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Owner & Founder — Shaan Mohammad",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
}
