package io.iotv.app

import android.app.Application
import android.content.Context
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import io.iotv.app.dagger.DaggerIotvComponent
import io.iotv.app.dagger.IotvComponent

class IotvApplication : Application() {
    companion object {
        fun from(context: Context): IotvApplication {
            return context.applicationContext as IotvApplication
        }
    }

    init {
        val iotvComponent: IotvComponent = DaggerIotvComponent
                .builder()
                .build()
        iotvComponent.inject(this)
    }

    override fun onCreate() {
        super.onCreate()
        Fabric.with(this, Crashlytics())

    }
}