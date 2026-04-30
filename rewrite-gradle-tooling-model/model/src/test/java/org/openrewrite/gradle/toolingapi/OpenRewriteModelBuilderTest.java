/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.gradle.toolingapi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenRewriteModelBuilderTest {

    @Test
    void java8UsesGradle814() {
        assertThat(OpenRewriteModelBuilder.defaultGradleVersion(8)).isEqualTo("8.14.3");
    }

    @Test
    void java24UsesGradle814() {
        assertThat(OpenRewriteModelBuilder.defaultGradleVersion(24)).isEqualTo("8.14.3");
    }

    @Test
    void java25RequiresGradle91() {
        assertThat(OpenRewriteModelBuilder.defaultGradleVersion(25)).isEqualTo("9.1.0");
    }

    @Test
    void futureJavaVersionsResolveToLatestKnownDistribution() {
        assertThat(OpenRewriteModelBuilder.defaultGradleVersion(99)).isEqualTo("9.1.0");
    }
}
