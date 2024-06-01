/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.maven.tree;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

public class PomTest {

    @ParameterizedTest
    @ValueSource(strings={"jar", "bundle"})
    void hasJarPackagingWithJarPackagingTypes(String type) {
        Pom pom = Pom.builder().packaging(type).build();
        assertThat(pom.hasJarPackaging()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings={"pom", "ejb"})
    void hasJarPackagingWithNonJarPackagingTypes(String type) {
        Pom pom = Pom.builder().packaging(type).build();
        assertThat(pom.hasJarPackaging()).isFalse();
    }

    @Test
    void hasJarPackagingWithNullJarPackaging() {
        Pom pom = Pom.builder() .build();
        assertThat(pom.hasJarPackaging()).isFalse();
    }
}
