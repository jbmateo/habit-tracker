package app.habittracker.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.wrapConnection
import co.touchlab.sqliter.DatabaseConfiguration

actual class DriverFactory {
    actual fun create(): SqlDriver {
        val schema = HabitDatabase.Schema
        return NativeSqliteDriver(
            DatabaseConfiguration(
                name = "habits.db",
                version = schema.version.toInt(),
                create = { connection -> wrapConnection(connection) { schema.create(it) } },
                upgrade = { connection, oldVersion, newVersion ->
                    wrapConnection(connection) { schema.migrate(it, oldVersion.toLong(), newVersion.toLong()) }
                },
                extendedConfig = DatabaseConfiguration.Extended(foreignKeyConstraints = true),
            ),
        )
    }
}
