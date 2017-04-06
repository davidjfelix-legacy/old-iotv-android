package io.iotv.app

import android.app.Application
import android.content.Context
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric

class IotvApplication : Application() {
    companion object {
        fun from(context: Context): IotvApplication {
            return context.applicationContext as IotvApplication
        }
    }

    override fun onCreate() {
        super.onCreate()
       Fabric.with(this, Crashlytics())

    }
}