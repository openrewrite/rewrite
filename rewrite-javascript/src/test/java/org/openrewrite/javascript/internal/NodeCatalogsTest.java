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
package org.openrewrite.javascript.internal;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A catalog entry that was written unquoted may only stay unquoted if YAML still reads the new
 * constraint as the string it is. Two ways it would not: the value opens with an indicator, so the
 * file stops parsing at all, or it parses and resolves to another type, leaving a number or a boolean
 * where a version belongs. Both are reachable from ordinary npm range syntax.
 */
class NodeCatalogsTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "~1.5.0", "^1.2.3", "1.4.1", "10.1.0-rc.1", "1.4.2-osera-00001",
            "1.x", "v2", "latest", "next", "1.0.0-alpha.1+build.7"})
    void constraintsThatCanStandUnquoted(String constraint) {
        assertThat(NodeCatalogs.canBePlainScalar(constraint)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // Opens with an indicator, so the file no longer parses.
            ">=2.0.0", "*", "-1.0.0", "?1", "|1", ">1",
            // Parses, but YAML resolves it to a number.
            "2", "2.0", "1_0", "0x1F", "0b11", ".inf",
            // Parses, but YAML resolves it to a boolean or null.
            "true", "False", "yes", "NO", "on", "off", "null", "~",
            // Structural: mapping value, comment, document marker, stray whitespace.
            "a: b", "a #c", "---", " 1.0.0", "1.0.0 "})
    void constraintsThatMustBeQuoted(String constraint) {
        assertThat(NodeCatalogs.canBePlainScalar(constraint)).isFalse();
    }
}
