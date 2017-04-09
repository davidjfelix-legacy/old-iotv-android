package io.iotv.app.dagger

import javax.inject.Singleton
import dagger.Component
import io.iotv.app.IotvApplication


@Singleton
@Component(modules =  arrayOf(APIClientModule::class))
interface IotvComponent {
    fun inject(application: IotvApplication)
}