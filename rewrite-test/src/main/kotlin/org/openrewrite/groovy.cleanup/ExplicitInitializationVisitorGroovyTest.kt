package org.openrewrite.groovy.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.groovy.GroovyRecipeTest
import org.openrewrite.java.cleanup.ExplicitInitialization

class ExplicitInitializationVisitorGroovyTest : GroovyRecipeTest {
    override val recipe: Recipe
        get() = ExplicitInitialization()

    @Issue("https://github.com/openrewrite/rewrite/issues/1272")
    @Test
    fun removeExplicitInitialization() = assertChanged(
        before = """
            class Test {
                private int a = 0
                private long b = 0L
                private short c = 0
                private int d = 1
                private long e = 2L
                private int f

                private boolean h = false
                private boolean i = true

                private Object j = new Object()
                private Object k = null

                int[] l = null
                
                private final Long n = null
            }
        """,
        after = """
            class Test {
                private int a
                private long b
                private short c
                private int d = 1
                private long e = 2L
                private int f
            
                private boolean h
                private boolean i = true
            
                private Object j = new Object()
                private Object k
            
                int[] l
                
                private final Long n = null
            }
        """
    )
}