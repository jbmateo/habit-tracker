package app.habittracker.di

import app.habittracker.domain.usecase.SeedDefaultCategoriesIfEmpty
import kotlinx.coroutines.runBlocking
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication =
    startKoin {
        appDeclaration()
        modules(platformModule, sharedModule)
    }.also { app -> runBlocking { app.koin.get<SeedDefaultCategoriesIfEmpty>()() } }
