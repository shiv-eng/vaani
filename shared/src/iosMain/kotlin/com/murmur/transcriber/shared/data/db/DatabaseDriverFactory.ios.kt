package com.murmur.transcriber.shared.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.murmur.transcriber.shared.db.MurmurDatabase

// Only compiled when this module's build.gradle.kts registers ios targets,
// which requires a macOS build host. See the isMacOs check there.
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(MurmurDatabase.Schema, "murmur.db")
}
