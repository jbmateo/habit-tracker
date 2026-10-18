package app.habittracker.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.habittracker.data.db.HabitDatabase
import java.util.Properties

fun inMemoryDb(): HabitDatabase {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties().apply { put("foreign_keys", "true") })
    HabitDatabase.Schema.create(driver)
    return HabitDatabase(driver)
}
