package org.openrewrite.gradle.search

import org.junit.jupiter.api.Test
import org.openrewrite.gradle.GradleRecipeTest

class FindDependencyHandler : GradleRecipeTest {
    @Test
    fun findDependenciesBlock() = assertChanged(
        recipe = fromRuntimeClasspath("org.openrewrite.gradle.search.FindDependencyHandler"),
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
