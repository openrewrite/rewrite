/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.gradle.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GradleWrapperScriptLoaderTest {

    private final GradleWrapperScriptLoader loader = new GradleWrapperScriptLoader();

    @Test
    void exactMatchReturnsRequestedVersion() {
        GradleWrapperScriptLoader.Nearest nearest = loader.findNearest("8.0");
        assertThat(nearest.getResolved().getVersion()).isEqualTo("8.0");
    }

    @Test
    void unmappedVersionFallsBackToNearest() {
        GradleWrapperScriptLoader.Nearest nearest = loader.findNearest("99.0");
        // Should resolve to the latest known version rather than failing
        assertThat(nearest.getResolved()).isNotNull();
        assertThat(nearest.getResolved().getVersion()).isNotEqualTo("99.0");
    }

    @Test
    void unmappedPatchVersionFallsBackToNearestBelow() {
        // Request a version between two known versions; should get the one just below
        GradleWrapperScriptLoader.Nearest nearest = loader.findNearest("8.0.9");
        assertThat(nearest.getResolved()).isNotNull();
        assertThat(nearest.getResolved().getVersion()).isEqualTo("8.0.2");
    }
}
