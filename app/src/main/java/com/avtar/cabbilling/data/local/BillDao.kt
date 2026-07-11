package com.avtar.cabbilling.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.avtar.cabbilling.data.local.entity.BillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {

    @Insert
    suspend fun insert(bill: BillEntity): Long

    @Update
    suspend fun update(bill: BillEntity)

    @Delete
    suspend fun delete(bill: BillEntity)

    @Query("DELETE FROM bills WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Newest trips first; ties broken by insertion order (id). */
    @Query("SELECT * FROM bills ORDER BY dateEpochMillis DESC, id DESC")
    fun observeAll(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<BillEntity?>

    @Query("SELECT * FROM bills WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BillEntity?

    /** Highest invoice number already used in [year], or null if none yet. */
    @Query("SELECT MAX(invoiceNumber) FROM bills WHERE year = :year")
    suspend fun maxInvoiceNumberForYear(year: Int): Int?

    @Query("DELETE FROM bills")
    suspend fun clear()
}
