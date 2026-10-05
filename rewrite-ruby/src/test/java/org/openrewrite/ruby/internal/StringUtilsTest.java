/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.ruby.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class StringUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = {"%[", "%s[", "%q[", "%Q["})
    void percentDelimiters(String delimiter) {
        assertThat(StringUtils.endDelimiter(delimiter)).isEqualTo("]");
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"", "'", "`"})
    void stringDelimiters(String delimiter) {
        assertThat(StringUtils.endDelimiter(delimiter)).isEqualTo(delimiter);
    }

    @Test
    void empty() {
        assertThat(StringUtils.endDelimiter("")).isEqualTo("");
    }
}
