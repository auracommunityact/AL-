package com.example.ui.auth

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import java.net.ConnectException
import java.net.UnknownHostException

object AuthErrorHelper {
    fun getFriendlyMessage(e: Throwable): String {
        val message = e.message?.lowercase() ?: ""
        
        return when {
            e is ConnectException || e is UnknownHostException -> 
                "Network error. Please check your internet connection."
            e is ConnectTimeoutException || e is SocketTimeoutException || message.contains("timeout") ->
                "Connection timed out. Please try again later."
            message.contains("invalid login credentials") -> 
                "Invalid email or password."
            message.contains("user already registered") || message.contains("email address already in use") -> 
                "An account with this email already exists."
            message.contains("weak password") || message.contains("password should be at least") -> 
                "Password is too weak. Please use at least 8 characters."
            message.contains("email link") || message.contains("rate limit") ->
                "Too many requests. Please wait a moment and try again."
            message.contains("invalid email") ->
                "Please enter a valid email address."
            else -> e.message ?: "An unexpected error occurred. Please try again."
        }
    }
}
