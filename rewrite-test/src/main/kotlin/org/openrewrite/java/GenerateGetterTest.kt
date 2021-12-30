package org.openrewrite.java

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Issue

interface GenerateGetterTest : JavaRecipeTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/1301")
    @Disabled
    @Test
    fun getterForPrimitiveInteger(jp: JavaParser) = assertChanged(
        recipe = GenerateGetter("counter"),
        before = """
            class T {
                int counter;
            }
        """,
        after = """
            class T {
                int counter;
                
                int getCounter() {
                    return counter;
                }
            }
        """
    )
}