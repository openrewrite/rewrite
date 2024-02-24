/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.internal.lang;

import org.junit.jupiter.api.Test;
import org.openrewrite.internal.lang.nonnull.DefaultNonNullTest;
import org.openrewrite.internal.lang.nullable.NonNullTest;

import static org.assertj.core.api.Assertions.assertThat;

class NullUtilsTest {

    @Test
    void packageNonNullDefault() {
        var results = NullUtils.findNonNullFields(DefaultNonNullTest.class);
        assertThat(results).hasSize(4);
        assertThat(results.get(0).getName()).isEqualTo("aCoolNonNullName");
        assertThat(results.get(1).getName()).isEqualTo("beCoolNonNullName");
        assertThat(results.get(2).getName()).isEqualTo("coolNonNullName");
        assertThat(results.get(3).getName()).isEqualTo("yourCoolNonNullName");
    }

    @Test
    void nonNulls() {
        var results = NullUtils.findNonNullFields(NonNullTest.class);
        assertThat(results).hasSize(4);
        assertThat(results.get(0).getName()).isEqualTo("aCoolNonNullName");
        assertThat(results.get(1).getName()).isEqualTo("beCoolNonNullName");
        assertThat(results.get(2).getName()).isEqualTo("coolNonNullName");
        assertThat(results.get(3).getName()).isEqualTo("yourCoolNonNullName");
    }

    @Test
    void noMemberFields() {
        var results = NullUtils.findNonNullFields(NoMembers.class);
        assertThat(results).isEmpty();
    }

    static class NoMembers {
    }
}
