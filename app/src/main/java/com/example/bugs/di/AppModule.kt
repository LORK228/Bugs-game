package com.example.bugs.di

import com.example.bugs.AppDatabase
import com.example.bugs.viewmodel.GameViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { AppDatabase.getDatabase(androidContext()) }
    single { get<AppDatabase>().appDao() }
    viewModel { GameViewModel(get()) }
}