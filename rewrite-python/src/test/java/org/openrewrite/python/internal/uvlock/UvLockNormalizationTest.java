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
package org.openrewrite.python.internal.uvlock;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.python.internal.uvlock.UvLockNormalization.normalizeRequiresDistSpecifier;

class UvLockNormalizationTest {

    @Test
    void whitespaceRemovedAndJoinedWithBareComma() {
        // Unlike requires-python, which keeps its ", " separator
        assertThat(normalizeRequiresDistSpecifier(" >=3.0 ,  <4")).isEqualTo(">=3.0,<4");
        assertThat(normalizeRequiresDistSpecifier(">= 3.10, <3.15")).isEqualTo(">=3.10,<3.15");
    }

    @Test
    void clausesSortedByVersion() {
        // The n2-inline-width fixture's declared vs recorded specifier
        assertThat(normalizeRequiresDistSpecifier(">=3.0,<4,!=3.1.0,!=3.2.0,!=3.3.0,!=3.3.1"))
          .isEqualTo(">=3.0,!=3.1.0,!=3.2.0,!=3.3.0,!=3.3.1,<4");
    }

    @Test
    void singleClauseUnchanged() {
        assertThat(normalizeRequiresDistSpecifier("==1.16.0")).isEqualTo("==1.16.0");
    }

    @Test
    void wildcardClausesSortByBaseVersion() {
        assertThat(normalizeRequiresDistSpecifier("<4,==3.1.*")).isEqualTo("==3.1.*,<4");
    }

    @Test
    void unparsableClauseThrows() {
        assertThatThrownBy(() -> normalizeRequiresDistSpecifier(">=not-a-version"))
          .isInstanceOf(IllegalArgumentException.class);
    }
}
