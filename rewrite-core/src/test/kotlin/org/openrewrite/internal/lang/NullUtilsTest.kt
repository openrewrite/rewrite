package org.openrewrite.internal.lang

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.internal.lang.nonnull.DefaultNonNullTest
import org.openrewrite.internal.lang.nullable.NonNullTest

class NullUtilsTest {

    @Test
    fun testPackageNonNullDefault() {
        val results = NullUtils.findNonNullFields(DefaultNonNullTest::class.java)
        assertThat(results).hasSize(4)
        assertThat(results[0].name).isEqualTo("aCoolNonNullName")
        assertThat(results[1].name).isEqualTo("beCoolNonNullName")
        assertThat(results[2].name).isEqualTo("coolNonNullName")
        assertThat(results[3].name).isEqualTo("yourCoolNonNullName")
    }

    @Test
    fun testNonNulls() {
        val results = NullUtils.findNonNullFields(NonNullTest::class.java)
        assertThat(results).hasSize(4)
        assertThat(results[0].name).isEqualTo("aCoolNonNullName")
        assertThat(results[1].name).isEqualTo("beCoolNonNullName")
        assertThat(results[2].name).isEqualTo("coolNonNullName")
        assertThat(results[3].name).isEqualTo("yourCoolNonNullName")
    }

    @Test
    fun noMemberFields() {
        val results = NullUtils.findNonNullFields(NoMembers::class.java)
        assertThat(results).isEmpty()
    }

    class NoMembers {
    }
}