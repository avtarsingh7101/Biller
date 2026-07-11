package com.avtar.cabbilling.data.repository

import com.avtar.cabbilling.data.local.ConfigDao
import com.avtar.cabbilling.data.local.entity.ConfigEntity
import com.avtar.cabbilling.data.model.AppTheme
import com.avtar.cabbilling.util.PinHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Owns the single [ConfigEntity] row: one-time setup, PIN credential, theme.
 * All mutating calls return [Result] so the UI layer can surface a safe message
 * instead of crashing on an unexpected storage failure.
 */
class ConfigRepository(private val configDao: ConfigDao) {

    val config: Flow<ConfigEntity?> = configDao.observe()

    suspend fun getConfig(): ConfigEntity? = withContext(Dispatchers.IO) { configDao.getOnce() }

    suspend fun completeSetup(
        companyName: String,
        companyPhone: String,
        carNames: List<String>,
        currencySymbol: String,
        pin: String,
        theme: AppTheme
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val salt = PinHasher.newSalt()
            val existing = configDao.getOnce() ?: ConfigEntity()
            configDao.upsert(
                existing.copy(
                    companyName = companyName.trim(),
                    companyPhone = companyPhone.trim(),
                    carNames = carNames.map { it.trim() }.filter { it.isNotEmpty() },
                    currencySymbol = currencySymbol.trim().ifEmpty { "" },
                    pinHash = PinHasher.hash(pin, salt),
                    pinSalt = salt,
                    theme = theme.id,
                    isSetupComplete = true
                )
            )
        }
    }

    suspend fun verifyPin(pin: String): Boolean = withContext(Dispatchers.IO) {
        val current = configDao.getOnce() ?: return@withContext false
        PinHasher.verify(pin, current.pinSalt, current.pinHash)
    }

    suspend fun updateProfile(
        companyName: String,
        companyPhone: String,
        carNames: List<String>,
        currencySymbol: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val current = configDao.getOnce() ?: error("No configuration to update.")
            configDao.upsert(
                current.copy(
                    companyName = companyName.trim(),
                    companyPhone = companyPhone.trim(),
                    carNames = carNames.map { it.trim() }.filter { it.isNotEmpty() },
                    currencySymbol = currencySymbol.trim().ifEmpty { "" }
                )
            )
        }
    }

    suspend fun changePin(newPin: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val current = configDao.getOnce() ?: error("No configuration to update.")
            val salt = PinHasher.newSalt()
            configDao.upsert(
                current.copy(pinHash = PinHasher.hash(newPin, salt), pinSalt = salt)
            )
        }
    }

    suspend fun updateTheme(theme: AppTheme) = withContext(Dispatchers.IO) {
        configDao.updateTheme(theme.id)
    }
}
