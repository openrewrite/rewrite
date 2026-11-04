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
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.Markers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.tree.Space.EMPTY;

class ChangeStringLiteralTest {

    @Test
    void valueSpelledWithAnEscapedDelimiterKeepsItsQuotes() {
        assertThat(replace("\"x", "\"\\\"x\"")).isEqualTo("\"baz\"");
    }

    @Test
    void gStringFragmentSpelledWithAnEscapeGetsNoDelimiters() {
        // A slashy-looking fragment: its value includes the slashes, so they cannot be delimiters
        assertThat(replace("/x\ny/", "/x\\ny/")).isEqualTo("baz");
    }

    @Test
    void dollarSlashyKeepsItsOwnClosingDelimiter() {
        assertThat(replace("a\nb", "$/a\\nb/$")).isEqualTo("$/baz/$");
    }

    private static String replace(String value, String valueSource) {
        J.Literal l = new J.Literal(randomId(), EMPTY, Markers.EMPTY, value, valueSource, null, JavaType.Primitive.String);
        return ChangeStringLiteral.withStringValue(l, "baz").getValueSource();
    }
}
