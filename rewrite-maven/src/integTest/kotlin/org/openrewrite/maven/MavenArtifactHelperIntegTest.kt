package org.openrewrite.maven

import org.junit.jupiter.api.Test
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.maven.utilities.MavenArtifactHelper

class MavenArtifactHelperIntegTest {

    @Test
    fun downloadArtifactAndDependencies() {
        val artifactPaths = MavenArtifactHelper.downloadArtifactAndDependencies(
                "org.openrewrite.recipe",
                "rewrite-testing-frameworks",
                "1.1.0",
                InMemoryExecutionContext { t -> t.printStackTrace() })
        println(artifactPaths)
    }
}