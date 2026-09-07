package com.quietstack.vaani.shared.di

import com.quietstack.vaani.shared.data.db.DatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { DatabaseDriverFactory() }
}
