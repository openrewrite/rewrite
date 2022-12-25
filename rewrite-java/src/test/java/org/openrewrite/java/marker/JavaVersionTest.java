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
package org.openrewrite.java.marker;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JavaVersionTest {

    @Test
    void doesNotMatch() {
        var javaVersion = new JavaVersion(UUID.randomUUID(), "", "", "-1", "").getMajorVersion();
        assertThat(javaVersion).isEqualTo(-1);
    }

    @Test
    void javaVersion8WithPrefixOneAndPattern() {
        var javaVersion = new JavaVersion(UUID.randomUUID(), "", "", "1.8.0+x", "").getMajorVersion();
        assertThat(javaVersion).isEqualTo(8);
    }

    @Test
    void javaVersion11Pattern() {
        var javaVersion = new JavaVersion(UUID.randomUUID(), "", "", "11.0.11+x", "").getMajorVersion();
        assertThat(javaVersion).isEqualTo(11);
    }
}
