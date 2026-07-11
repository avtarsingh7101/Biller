package com.avtar.cabbilling.data.repository

import androidx.room.withTransaction
import com.avtar.cabbilling.data.local.AppDatabase
import com.avtar.cabbilling.data.local.entity.BillEntity
import com.avtar.cabbilling.util.Formatters
import com.avtar.cabbilling.util.InvoiceSequence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Validated payload the UI hands over to create a bill. */
data class NewBillInput(
    val carName: String,
    val carPlate: String = "",
    val fromLocation: String,
    val toLocation: String,
    val passengerName: String,
    val amount: Double,
    val notes: String,
    val date: LocalDate
)

/**
 * Owns all invoice reads/writes. The create path runs inside a single Room
 * transaction so the sequence bump and the row insert can never diverge, even
 * if two saves race. Every public mutator returns [Result].
 */
class BillRepository(private val database: AppDatabase) {

    private val billDao = database.billDao()
    private val configDao = database.configDao()

    fun observeAll(): Flow<List<BillEntity>> = billDao.observeAll()

    fun observeById(id: Long): Flow<BillEntity?> = billDao.observeById(id)

    suspend fun getById(id: Long): BillEntity? =
        withContext(Dispatchers.IO) { billDao.getById(id) }

    /**
     * Allocates the next invoice number (applying the yearly-reset rule against
     * the trip's year) and inserts the bill atomically.
     */
    suspend fun createBill(input: NewBillInput): Result<BillEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                database.withTransaction {
                    val config = configDao.getOnce()
                        ?: error("Setup is incomplete. Please finish setup first.")

                    val targetYear = input.date.year
                    val nextNumber = InvoiceSequence.nextNumber(
                        lastNumber = config.lastInvoiceNumber,
                        lastYear = config.lastInvoiceYear,
                        targetYear = targetYear
                    )
                    if (!InvoiceSequence.isWithinLimit(nextNumber)) {
                        error("Invoice limit of ${InvoiceSequence.MAX} reached for $targetYear.")
                    }

                    val entity = BillEntity(
                        invoiceNumber = nextNumber,
                        invoiceCode = InvoiceSequence.code(nextNumber),
                        companyName = config.companyName,
                        carName = input.carName,
                        carPlate = input.carPlate.ifBlank { null },
                        fromLocation = input.fromLocation,
                        toLocation = input.toLocation,
                        passengerName = input.passengerName.trim(),
                        amount = input.amount,
                        notes = input.notes.trim(),
                        dateEpochMillis = Formatters.startOfDayMillis(input.date),
                        year = targetYear,
                        createdAtMillis = System.currentTimeMillis()
                    )

                    val newId = billDao.insert(entity)
                    configDao.updateSequence(nextNumber, targetYear)
                    entity.copy(id = newId)
                }
            }
        }

    suspend fun deleteBill(bill: BillEntity): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { billDao.delete(bill) } }

    /** Wipes every bill and the configuration row in one transaction (factory reset). */
    suspend fun resetApp(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                database.withTransaction {
                    billDao.clear()
                    configDao.clear()
                }
            }
        }
}
