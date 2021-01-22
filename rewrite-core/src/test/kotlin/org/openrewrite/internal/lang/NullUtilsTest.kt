/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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