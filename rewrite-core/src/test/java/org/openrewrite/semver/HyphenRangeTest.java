/*
 * Copyright 2021 the original author or authors.
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

class HyphenRangeTest {

    @Test
    void isValidWhenCurrentIsNull() {
        HyphenRange hyphenRange = HyphenRange.build("1-2", null).getValue();

        assertThat(hyphenRange).isNotNull();
        assertThat(hyphenRange.isValid(null, "1.0.0")).isTrue();
    }

    @Test
    void pattern() {
        assertThat(HyphenRange.build("1 - 2", null).isValid()).isTrue();
        assertThat(HyphenRange.build("1.0.0.0 - 2", null).isValid()).isTrue();
        assertThat(HyphenRange.build("1 - 2.0.0.0", null).isValid()).isTrue();
        assertThat(HyphenRange.build("1.0.0.0 - 2.0.0.0", null).isValid()).isTrue();
        assertThat(HyphenRange.build("1", null).isValid()).isFalse();
        assertThat(HyphenRange.build("1 - 2.x", null).isValid()).isFalse();
        assertThat(HyphenRange.build("1.0.0.0.0 - 2", null).isValid()).isFalse();
    }

    /**
     * 1.2.3 - 2.3.4 := >=1.2.3 <=2.3.4
     */
    @Test
    void inclusiveSet() {
        HyphenRange hyphenRange = HyphenRange.build("1.2.3 - 2.3.4", null).getValue();

        assertThat(hyphenRange).isNotNull();
        assertThat(hyphenRange.isValid("1.0", "1.2.2")).isFalse();
        assertThat(hyphenRange.isValid("1.0", "1.2.2.0")).isFalse();
        assertThat(hyphenRange.isValid("1.0", "1.2.3.RELEASE")).isTrue();
        assertThat(hyphenRange.isValid("1.0", "1.2.3.0.RELEASE")).isTrue();
        assertThat(hyphenRange.isValid("1.0", "1.2.3")).isTrue();
        assertThat(hyphenRange.isValid("1.0", "1.2.3.0")).isTrue();
        assertThat(hyphenRange.isValid("1.0", "1.2.3.0.0")).isTrue();
        assertThat(hyphenRange.isValid("1.0", "2.3.4")).isTrue();
        assertThat(hyphenRange.isValid("1.0", "2.3.4.0")).isTrue();
        assertThat(hyphenRange.isValid("1.0", "2.3.4.1")).isFalse();
        assertThat(hyphenRange.isValid("1.0", "2.3.5")).isFalse();
        assertThat(hyphenRange.isValid("1.0", "2.3.5.0")).isFalse();
    }

    /**
     * 1.2 - 2 := >=1.2.0 <=2.0.0
     */
    @Test
    void partialVersion() {
        HyphenRange hyphenRange = HyphenRange.build("1.2 - 2", null).getValue();

        assertThat(hyphenRange).isNotNull();
        assertThat(hyphenRange.isValid("1.0", "1.1.9")).isFalse();
        assertThat(hyphenRange.isValid("1.0", "1.1.9.9")).isFalse();
        assertThat(hyphenRange.isValid("1.0", "1.2.0")).isTrue();
        assertThat(hyphenRange.isValid("1.0", "1.2.0.0")).isTrue();
        assertThat(hyphenRange.isValid("1.0", "1.2.0.0.0")).isTrue();
        assertThat(hyphenRange.isValid("1.0", "2.0.0")).isTrue();
        assertThat(hyphenRange.isValid("1.0", "2.0.0.0")).isTrue();
        assertThat(hyphenRange.isValid("1.0", "2.0.1")).isFalse();
        assertThat(hyphenRange.isValid("1.0", "2.0.0.1")).isFalse();
    }
}
