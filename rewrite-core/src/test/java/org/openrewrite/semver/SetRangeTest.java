/*
 * Copyright 2022 the original author or authors.
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

class SetRangeTest {
    @Test
    void exclusiveRange() {
        SetRange setRange = SetRange.build("(1,2)", null).getValue();

        assertThat(setRange.isValid(null, "0.1")).isFalse();
        assertThat(setRange.isValid(null, "1")).isFalse();
        assertThat(setRange.isValid(null, "1.0")).isFalse();
        assertThat(setRange.isValid(null, "1.1")).isTrue();
        assertThat(setRange.isValid(null, "2")).isFalse();
        assertThat(setRange.isValid(null, "2.0")).isFalse();
        assertThat(setRange.isValid(null, "2.1")).isFalse();
    }

    @Test
    void inclusiveRange() {
        SetRange setRange = SetRange.build("[1,2]", null).getValue();

        assertThat(setRange.isValid(null, "0.1")).isFalse();
        assertThat(setRange.isValid(null, "1")).isTrue();
        assertThat(setRange.isValid(null, "1.0")).isTrue();
        assertThat(setRange.isValid(null, "1.1")).isTrue();
        assertThat(setRange.isValid(null, "2")).isTrue();
        assertThat(setRange.isValid(null, "2.0")).isTrue();
        assertThat(setRange.isValid(null, "2.1")).isFalse();
    }

    @Test
    void inclusiveLowOnly() {
        SetRange setRange = SetRange.build("[1,2)", null).getValue();

        assertThat(setRange.isValid(null, "0.1")).isFalse();
        assertThat(setRange.isValid(null, "1")).isTrue();
        assertThat(setRange.isValid(null, "1.0")).isTrue();
        assertThat(setRange.isValid(null, "1.1")).isTrue();
        assertThat(setRange.isValid(null, "2")).isFalse();
        assertThat(setRange.isValid(null, "2.0")).isFalse();
        assertThat(setRange.isValid(null, "2.1")).isFalse();
    }

    @Test
    void inclusiveHighOnly() {
        SetRange setRange = SetRange.build("(1,2]", null).getValue();

        assertThat(setRange.isValid(null, "0.1")).isFalse();
        assertThat(setRange.isValid(null, "1")).isFalse();
        assertThat(setRange.isValid(null, "1.0")).isFalse();
        assertThat(setRange.isValid(null, "1.1")).isTrue();
        assertThat(setRange.isValid(null, "2")).isTrue();
        assertThat(setRange.isValid(null, "2.0")).isTrue();
        assertThat(setRange.isValid(null, "2.1")).isFalse();
    }

    @Test
    void inclusiveLowUnqualifiedHigh() {
        SetRange setRange = SetRange.build("[1,)", null).getValue();

        assertThat(setRange.isValid(null, "0")).isFalse();
        assertThat(setRange.isValid(null, "1")).isTrue();
        assertThat(setRange.isValid(null, "1.0")).isTrue();
        assertThat(setRange.isValid(null, "2.0")).isTrue();
        assertThat(setRange.isValid(null, "9999.0")).isTrue();
    }

    @Test
    void inclusiveHighUnqualifiedLow() {
        SetRange setRange = SetRange.build("(,9999]", null).getValue();

        assertThat(setRange.isValid(null, "1")).isTrue();
        assertThat(setRange.isValid(null, "1.0")).isTrue();
        assertThat(setRange.isValid(null, "2.0")).isTrue();
        assertThat(setRange.isValid(null, "9999.0")).isTrue();
    }
}
