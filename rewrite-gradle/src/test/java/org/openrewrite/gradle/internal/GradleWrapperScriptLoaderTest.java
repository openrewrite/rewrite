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

public class GradleWrapperScriptLoaderTest {

    @Test
    void nearestVersion() {
        GradleWrapperScriptLoader.Nearest nearest = new GradleWrapperScriptLoader().findNearest("6.9.100");
        String v = nearest.getResolved().getVersion();
        assertThat(v).startsWith("6.9.");

        // this version doesn't exist, so we should get the nearest instead
        assertThat(v).isNotEqualTo("6.9.100");
    }

    @Test
    void lastVersion() {
        GradleWrapperScriptLoader.Nearest nearest = new GradleWrapperScriptLoader().findNearest(null);
        String v = nearest.getResolved().getVersion();
        assertThat(Integer.parseInt(v.split("\\.")[0])).isGreaterThanOrEqualTo(8);
    }
}
