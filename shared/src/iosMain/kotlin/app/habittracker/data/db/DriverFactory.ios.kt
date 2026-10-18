package app.habittracker.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.wrapConnection
import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.DatabaseFileContext

private const val DATABASE_NAME = "habits.db"

private fun databaseConfiguration(): DatabaseConfiguration {
    val schema = HabitDatabase.Schema
    return DatabaseConfiguration(
        name = DATABASE_NAME,
        version = schema.version.toInt(),
        create = { connection -> wrapConnection(connection) { schema.create(it) } },
        upgrade = { connection, oldVersion, newVersion ->
            wrapConnection(connection) { schema.migrate(it, oldVersion.toLong(), newVersion.toLong()) }
        },
        extendedConfig = DatabaseConfiguration.Extended(foreignKeyConstraints = true),
    )
}

/** Deletes the database file at the exact location [NativeSqliteDriver] opens it. Used by UI tests for a fresh start. */
internal fun deleteHabitsDatabase() {
    DatabaseFileContext.deleteDatabase(DATABASE_NAME)
}

actual class DriverFactory {
    actual fun create(): SqlDriver = NativeSqliteDriver(databaseConfiguration())
}
