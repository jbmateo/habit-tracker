package app.habittracker

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class ArchitectureTest {
    // Gradle runs host tests with the working directory set to `shared/`.
    private val root = File(System.getProperty("user.dir"), "src/commonMain/kotlin/app/habittracker")

    private val allowedDomainPrefixes = listOf(
        "kotlin.",
        "kotlinx.coroutines.",
        "kotlinx.datetime.",
        "app.habittracker.domain.",
    )

    @Test
    fun domainImportsOnlyAllowedPackages() {
        val violations = importsUnder("domain").filter { (_, _, imported) ->
            allowedDomainPrefixes.none { imported.startsWith(it) }
        }
        assertTrue(violations.isEmpty(), "Disallowed imports in domain:\n" + violations.format())
    }

    @Test
    fun presentationDoesNotImportData() {
        val violations = importsUnder("presentation").filter { (_, _, imported) ->
            imported.startsWith("app.habittracker.data.")
        }
        assertTrue(violations.isEmpty(), "presentation must not import data:\n" + violations.format())
    }

    @Test
    fun scannerFindsSourceRoot() {
        assertTrue(root.isDirectory, "Source root not found: ${root.absolutePath}")
    }

    private data class Import(val file: File, val line: Int, val imported: String)

    private fun importsUnder(dir: String): List<Import> =
        File(root, dir).takeIf { it.isDirectory }
            ?.walkTopDown()
            ?.filter { it.isFile && it.extension == "kt" }
            ?.flatMap { file ->
                file.readLines().mapIndexedNotNull { index, text ->
                    val trimmed = text.trim()
                    if (trimmed.startsWith("import ")) Import(file, index + 1, trimmed.removePrefix("import ").trim()) else null
                }
            }
            ?.toList()
            ?: emptyList()

    private fun List<Import>.format() =
        joinToString("\n") { "${it.file.relativeTo(root)}:${it.line}: import ${it.imported}" }
}
