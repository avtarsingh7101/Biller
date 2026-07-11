package com.avtar.cabbilling.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.avtar.cabbilling.data.local.entity.ConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigDao {

    @Query("SELECT * FROM config WHERE id = 1 LIMIT 1")
    fun observe(): Flow<ConfigEntity?>

    @Query("SELECT * FROM config WHERE id = 1 LIMIT 1")
    suspend fun getOnce(): ConfigEntity?

    @Upsert
    suspend fun upsert(config: ConfigEntity)

    /** Atomic counter bump used inside the create-bill transaction. */
    @Query("UPDATE config SET lastInvoiceNumber = :number, lastInvoiceYear = :year WHERE id = 1")
    suspend fun updateSequence(number: Int, year: Int)

    @Query("UPDATE config SET theme = :theme WHERE id = 1")
    suspend fun updateTheme(theme: String)

    @Query("DELETE FROM config")
    suspend fun clear()
}
