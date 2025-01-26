/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.semver;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LatestIntegrationTest {
    private final LatestIntegration latestIntegration = new LatestIntegration(null);

    @Test
    void isValidWhenCurrentIsNull() {
        assertThat(latestIntegration.isValid(null, "1.0.0")).isTrue();
    }

    @Test
    void nonNumericPartsValid() {
        assertThat(latestIntegration.isValid("1.0", "1.1.1.1")).isTrue();
        assertThat(latestIntegration.isValid("1.0", "1.0.RELEASE")).isTrue();
        assertThat(latestIntegration.isValid("1.0", "1.0.0.Final")).isTrue();
        assertThat(latestIntegration.isValid("1.0", "1.1.1")).isTrue();
        assertThat(latestIntegration.isValid("1.0", "1.1")).isTrue();
        assertThat(latestIntegration.isValid("1.0", "1")).isTrue();
        assertThat(latestIntegration.isValid("1.0", "1.1.1.1.1")).isTrue();

        assertThat(latestIntegration.isValid("1.0", "1.1.0-SNAPSHOT")).isTrue();
        assertThat(latestIntegration.isValid("1.0", "1.1.a")).isTrue();
        assertThat(latestIntegration.isValid("1.0", "1.1.1.1.a")).isTrue();
        assertThat(latestIntegration.isValid("1.0", "2.0.0.Alpha2")).isTrue();

        assertThat(latestIntegration.compare(null, "1.0", "1.1.a")).isLessThan(0);
        assertThat(latestIntegration.compare(null, "1.0", "1.1.1.1.a")).isLessThan(0);
    }
}
