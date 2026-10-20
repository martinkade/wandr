package com.wandr.domain.usecase

import com.wandr.domain.push.PushTokenManager
import com.wandr.domain.repository.AccountRepository

/** Removes all data of the user (server and this device); the account and the profile stay. */
class ResetAccountUseCase(private val repository: AccountRepository) {
    suspend operator fun invoke(userId: String) = repository.resetData(userId)
}

/** Deletes the account with all its data and signs out on this device. */
class DeleteAccountUseCase(
    private val repository: AccountRepository,
    private val pushTokenManager: PushTokenManager
) {
    suspend operator fun invoke(userId: String): Result<Unit> =
        // The device token went with the account: the next user of this device registers it again.
        repository.deleteAccount(userId).onSuccess { pushTokenManager.forgetRegistration() }
}
