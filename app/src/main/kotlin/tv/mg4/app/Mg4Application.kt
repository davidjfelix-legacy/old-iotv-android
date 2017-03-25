package tv.mg4.app

import android.app.Application
import android.content.Context
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric

class Mg4Application: Application() {
    companion object {
        fun from(context: Context): Mg4Application {
            return context.applicationContext as Mg4Application
        }
    }

    override fun onCreate() {
        super.onCreate()
       Fabric.with(this, Crashlytics())

    }
}