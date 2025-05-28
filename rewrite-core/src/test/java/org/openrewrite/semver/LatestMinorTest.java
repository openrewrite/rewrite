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

public class LatestMinorTest {
    private final LatestMinor latestMinor = new LatestMinor(null);

    @Test
    void isValidWhenCurrentIsNull() {
        assertThat(latestMinor.isValid(null, "1.0.0")).isTrue();
    }
    @Test
    void isValid() {
        assertThat(latestMinor.isValid("1.0.0", "1.0.0")).isTrue();
        assertThat(latestMinor.isValid("1.0.0", "1.0.0.1")).isTrue();
        assertThat(latestMinor.isValid("1.0.0", "1.0.1")).isTrue();
        assertThat(latestMinor.isValid("1.0", "1.0.1")).isTrue();
        assertThat(latestMinor.isValid("1.0.0", "1.1.0")).isTrue();
        assertThat(latestMinor.isValid("1.0.0", "2.0.0")).isFalse();
    }

    @Test
    void noSnapshots() {
        assertThat(latestMinor.isValid("1.0.0", "1.1.0-SNAPSHOT")).isFalse();
    }
}
