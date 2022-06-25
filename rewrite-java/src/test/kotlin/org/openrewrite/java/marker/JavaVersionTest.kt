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
package org.openrewrite.java.marker

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

class JavaVersionTest {

    @Test
    fun doesNotMatch() {
        val javaVersion = JavaVersion(UUID.randomUUID(), "", "", "-1", "").majorVersion
        assertThat(javaVersion).isEqualTo(-1)
    }

    @Test
    fun javaVersion8WithPrefixOneAndPattern() {
        val javaVersion = JavaVersion(UUID.randomUUID(), "", "", "1.8.0+x", "").majorVersion
        assertThat(javaVersion).isEqualTo(8)
    }

    @Test
    fun javaVersion11Pattern() {
        val javaVersion = JavaVersion(UUID.randomUUID(), "", "", "11.0.11+x", "").majorVersion
        assertThat(javaVersion).isEqualTo(11)
    }
}
