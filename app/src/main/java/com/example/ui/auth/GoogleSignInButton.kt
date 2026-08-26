package com.example.ui.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GoogleSignInButton(viewModel: AuthViewModel) {
    Button(
        onClick = { viewModel.signInWithQuickAccess("student_review_${System.currentTimeMillis()}@gmail.com") },
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Text("Sign in with Google")
    }
}
