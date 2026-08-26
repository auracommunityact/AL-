package com.example.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.R
import com.example.ui.auth.AuthState
import com.example.ui.auth.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(navController: NavController, authViewModel: AuthViewModel) {
    // Animation states
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.95f) }
    
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        // Run animations in parallel
        val animationJob = launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing)
            )
        }
        val scaleJob = launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }
        
        // Ensure minimum display time
        delay(1000)
        
        when (authState) {
            is AuthState.Success -> {
                navController.navigate("main?tab=home") {
                    popUpTo("splash") { inclusive = true }
                    launchSingleTop = true
                }
            }
            is AuthState.Unauthenticated, is AuthState.Idle -> {
                navController.navigate("login") {
                    popUpTo("splash") { inclusive = true }
                    launchSingleTop = true
                }
            }
            else -> {
                // Keep showing splash while Checking or Loading
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        // App Icon - perfectly centered
        Box(
            modifier = Modifier
                .size(100.dp) // Premium small size (90-110dp range)
                .scale(scale.value)
                .alpha(alpha.value)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(24.dp),
                    clip = false
                )
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White), // White background for the icon container if needed
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.opened_book_logo_1782632258077),
                contentDescription = "Aura Learning Logo",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }
    }
}
