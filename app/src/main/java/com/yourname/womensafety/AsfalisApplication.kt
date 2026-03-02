package com.yourname.womensafety

import android.app.Application
import com.yourname.womensafety.data.AppServiceLocator

class AsfalisApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppServiceLocator.init(this)
    }
}
