package com.adaptyui.example

import android.app.Application
import com.adapty.Adapty
import com.adapty.models.AdaptyConfig
import com.adapty.utils.AdaptyLogLevel

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        Adapty.logLevel = if (BuildConfig.DEBUG) AdaptyLogLevel.VERBOSE else AdaptyLogLevel.NONE
        Adapty.activate(
            this,
            AdaptyConfig.Builder("YOUR_ADAPTY_KEY").build(),
        )
    }
}