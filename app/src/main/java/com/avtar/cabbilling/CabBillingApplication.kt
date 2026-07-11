package com.avtar.cabbilling

import android.app.Application
import com.avtar.cabbilling.di.AppContainer

class CabBillingApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
