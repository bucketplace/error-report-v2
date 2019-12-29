package di

import okhttp3.OkHttpClient
import org.koin.dsl.module
import utils.LoggingInterceptor
import java.util.logging.Level
import java.util.logging.Logger

val appModule = module {

    single {
        Logger.getLogger(OkHttpClient::class.java.name).level = Level.FINE
        OkHttpClient.Builder()
            .addInterceptor(LoggingInterceptor())
            .build()
    }
}