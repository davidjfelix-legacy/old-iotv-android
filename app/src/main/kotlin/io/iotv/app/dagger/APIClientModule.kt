package io.iotv.app.dagger

import dagger.Module
import dagger.Provides
import io.iotv.app.api.IotvAPIClient
import javax.inject.Singleton

@Module
class APIClientModule {

    @Provides
    @Singleton
    fun getApiClient(): IotvAPIClient {
        return IotvAPIClient()
    }
}