package com.wandr.domain.usecase

import com.wandr.domain.repository.PushTokenRepository

class RegisterPushTokenUseCase(private val repository: PushTokenRepository) {
    suspend operator fun invoke(token: String, platform: String) = repository.register(token, platform)
}
