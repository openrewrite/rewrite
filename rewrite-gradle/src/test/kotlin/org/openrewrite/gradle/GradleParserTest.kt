package org.openrewrite.gradle

import org.junit.jupiter.api.Test
import org.openrewrite.Parser
import org.openrewrite.Recipe
import org.openrewrite.groovy.GroovyParser
import org.openrewrite.groovy.GroovyRecipeTest
import org.openrewrite.groovy.tree.G
import org.openrewrite.java.search.FindMethods

class GradleParserTest : GroovyRecipeTest {
    override val parser: Parser<G.CompilationUnit>
        get() = GradleParser(GroovyParser.builder())

    @Test
    fun findDependenciesBlock() = assertChanged(
        recipe = FindMethods("org.gradle.api.Project dependencies(groovy.lang.Closure)", true),
        before = """
            dependencies {
                api 'com.google.guava:guava:23.0'
            }
        """,
        after = """
            /*~~>*/dependencies {
                api 'com.google.guava:guava:23.0'
            }
        """
    )
}
