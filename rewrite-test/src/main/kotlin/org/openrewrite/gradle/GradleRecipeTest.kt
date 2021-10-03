package org.openrewrite.gradle

import org.openrewrite.Parser
import org.openrewrite.groovy.GroovyParser
import org.openrewrite.groovy.GroovyRecipeTest
import org.openrewrite.groovy.tree.G

interface GradleRecipeTest : GroovyRecipeTest {
    override val parser: Parser<G.CompilationUnit>
        get() = GradleParser(GroovyParser.builder())
}
