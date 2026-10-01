package com.wandr.domain.model

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val user: User
)
