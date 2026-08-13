/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.golang;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.CategoryDescriptor;
import org.openrewrite.config.Environment;

import static org.assertj.core.api.Assertions.assertThat;

class GoCategoryDescriptorTest {

    @Test
    void golangPackageDisplaysAsGo() {
        // given
        Environment environment = Environment.builder()
                .scanRuntimeClasspath("org.openrewrite.golang")
                .build();

        // when
        CategoryDescriptor descriptor = environment.listCategoryDescriptors().stream()
                .filter(d -> "org.openrewrite.golang".equals(d.getPackageName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No category descriptor for org.openrewrite.golang"));

        // then
        assertThat(descriptor.getDisplayName()).isEqualTo("Go");
    }
}
