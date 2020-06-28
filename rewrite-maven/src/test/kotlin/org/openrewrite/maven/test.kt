import org.junit.jupiter.api.Assertions.assertEquals
import org.openrewrite.maven.tree.Maven

fun assertRefactored(pom: Maven.Pom, refactored: String) {
    assertEquals(refactored.trimIndent(), pom.printTrimmed())
}
