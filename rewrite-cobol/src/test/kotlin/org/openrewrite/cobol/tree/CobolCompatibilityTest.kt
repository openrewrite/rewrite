package org.openrewrite.cobol.tree

import io.github.classgraph.ClassGraph
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.openrewrite.internal.StringUtils
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream

class CobolCompatibilityTest : CobolTreeTest {
    @ParameterizedTest
    @MethodSource
    fun nist(resourcePath: Path) = roundTrip(StringUtils.readFully(javaClass.getResourceAsStream("/$resourcePath")))

    companion object {
        @JvmStatic
        fun nist(): Stream<Path>? {
            ClassGraph().acceptPaths("/gov/nist").scan().use { scanResult ->
                return scanResult.allResources.paths.stream().map(Paths::get)
            }
        }
    }
}
