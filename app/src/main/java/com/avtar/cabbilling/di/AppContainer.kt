package com.avtar.cabbilling.di

import android.content.Context
import com.avtar.cabbilling.data.local.AppDatabase
import com.avtar.cabbilling.data.repository.BillRepository
import com.avtar.cabbilling.data.repository.ConfigRepository
import com.avtar.cabbilling.data.repository.TripRepository

class AppContainer(context: Context) {
    private val database = AppDatabase.getInstance(context)

    val billRepository = BillRepository(database)
    val configRepository = ConfigRepository(database.configDao())
    val tripRepository = TripRepository(database.tripDao())
    val sessionState = SessionState()
}
