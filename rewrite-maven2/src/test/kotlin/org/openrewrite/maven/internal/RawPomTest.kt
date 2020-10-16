package org.openrewrite.maven.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RawPomTest {
    @Test
    fun profileActivationByJdk() {
        assertThat(RawPom.ProfileActivation("11", emptyMap()).isActive(null)).isTrue()
        assertThat(RawPom.ProfileActivation("[,12)", emptyMap()).isActive(null)).isTrue()
        assertThat(RawPom.ProfileActivation("[,11]", emptyMap()).isActive(null)).isFalse()
    }
}
