package org.openrewrite.semver

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DependencyMatcherTest {

    @Test
    fun groupArtifact() {
        assertThat(DependencyMatcher.build("org.springframework.boot:*").isValid).isTrue
    }
}
