package di

import okhttp3.OkHttpClient
import org.koin.dsl.module
import utils.LoggingInterceptor

val appModule = module {

    single { OkHttpClient.Builder().addInterceptor(LoggingInterceptor()).build() }
}