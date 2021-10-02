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

    override val recipe: Recipe
        get() = FindMethods("org.gradle.api.Project dependencies(groovy.lang.Closure)", true)

    @Test
    fun gradle() = assertChanged(
        before = """
            plugins {
                id 'java-library'
            }
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
                api 'com.google.guava:guava:23.0'
            }
        """,
        after = """
            plugins {
                id 'java-library'
            }
            
            repositories {
                mavenCentral()
            }
            
            /*~~>*/dependencies {
                api 'com.google.guava:guava:23.0'
            }
        """
    )
}
