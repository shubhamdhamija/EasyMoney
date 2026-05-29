package com.invest.easymoney.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OpenAiClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OpenAiRetrofit
