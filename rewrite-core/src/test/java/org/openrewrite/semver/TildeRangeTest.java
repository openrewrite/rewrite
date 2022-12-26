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

class TildeRangeTest {

    @Test
    void isValidWhenCurrentIsNull() {
        TildeRange tildeRange = TildeRange.build("~1", null).getValue();

        assertThat(tildeRange).isNotNull();
        assertThat(tildeRange.isValid(null, "1.0.0")).isTrue();
    }

    @Test
    void pattern() {
        assertThat(TildeRange.build("~1", null).isValid()).isTrue();
        assertThat(TildeRange.build("~1.2", null).isValid()).isTrue();
        assertThat(TildeRange.build("~1.2.3", null).isValid()).isTrue();
        assertThat(TildeRange.build("~1.2.3.4", null).isValid()).isTrue();
        assertThat(TildeRange.build("~1.2.3.4.5", null).isValid()).isFalse();
    }

    /**
     * ~1.2.3 := >=1.2.3 <1.(2+1).0 := >=1.2.3 <1.3.0
     */
    @Test
    void updatePatch() {
        TildeRange tildeRange = TildeRange.build("~1.2.3", null).getValue();

        assertThat(tildeRange).isNotNull();
        assertThat(tildeRange.isValid("1.0", "1.2.3.0")).isTrue();
        assertThat(tildeRange.isValid("1.0", "1.2.3.1")).isTrue();
        assertThat(tildeRange.isValid("1.0", "1.2.3")).isTrue();
        assertThat(tildeRange.isValid("1.0", "1.2.3.RELEASE")).isTrue();
        assertThat(tildeRange.isValid("1.0", "1.2.4")).isTrue();
        assertThat(tildeRange.isValid("1.0", "1.3.0")).isFalse();
    }

    @Test
    void updateMicro() {
        TildeRange tildeRange = TildeRange.build("~1.2.3.4", null).getValue();

        assertThat(tildeRange).isNotNull();
        assertThat(tildeRange.isValid("1.0", "1.2.3.5")).isTrue();
        assertThat(tildeRange.isValid("1.0", "1.2.3.0")).isFalse();
        assertThat(tildeRange.isValid("1.0", "1.2.3.5.0")).isTrue();
        assertThat(tildeRange.isValid("1.0", "1.2.3")).isFalse();
        assertThat(tildeRange.isValid("1.0", "1.2.4")).isFalse();
        assertThat(tildeRange.isValid("1.0", "1.2.4.0")).isFalse();
        assertThat(tildeRange.isValid("1.0", "1.3.0")).isFalse();
    }

    /**
     * ~1.2 := >=1.2.0 <1.(2+1).0 := >=1.2.0 <1.3.0-0 (Same as 1.2.x)
     */
    @Test
    void updatePatchImplicitZeroPatch() {
        TildeRange tildeRange = TildeRange.build("~1.2", null).getValue();

        assertThat(tildeRange).isNotNull();
        assertThat(tildeRange.isValid("1.0", "1.2.0")).isTrue();
        assertThat(tildeRange.isValid("1.0", "1.2.4")).isTrue();
        assertThat(tildeRange.isValid("1.0", "1.3.0")).isFalse();
    }

    /**
     * ~1 := >=1.0.0 <(1+1).0.0 := >=1.0.0 <2.0.0-0 (Same as 1.x)
     */
    @Test
    void updateMajor() {
        TildeRange tildeRange = TildeRange.build("~1", null).getValue();

        assertThat(tildeRange).isNotNull();
        assertThat(tildeRange.isValid("1.0", "1.0.1")).isTrue();
        assertThat(tildeRange.isValid("1.0", "1.9.9")).isTrue();
        assertThat(tildeRange.isValid("1.0", "2.0.0")).isFalse();
    }
}
