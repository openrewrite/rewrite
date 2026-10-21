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
package org.openrewrite.python.internal.uvlock.marker;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkerParserTest {

    private final MarkerInterner interner = new MarkerInterner();

    private String render(String marker) throws MarkerParser.MarkerSyntaxException {
        return MarkerTree.parse(interner, marker).toString();
    }

    @Test
    void starPatternsFollowUvsVersionGrammar() throws Exception {
        assertThat(render("python_version == ' 3.8.*'")).isEqualTo("python_full_version == '3.8.*'");
        assertThat(render("python_version == 'V3.8.*'")).isEqualTo("python_full_version == '3.8.*'");
        assertThat(render("python_full_version == '1!3.8.*'")).isEqualTo("python_full_version == '3.8.*'");
        assertThat(render("python_version != ' 3.8.*'")).isEqualTo("python_full_version != '3.8.*'");

        assertThat(render("python_version == '3.8.* '")).isEqualTo("true");
    }

    @Test
    void releaseSegmentAtLongMaxIsIgnoredRatherThanOverflowing() throws Exception {
        assertThat(render("python_version > '3.9223372036854775806'")).isEqualTo("python_full_version >= '3.9223372036854775807'");

        // uv keeps this clause, its segments being u64, but its successor does not fit a long
        assertThat(render("python_version > '3.9223372036854775807'")).isEqualTo("true");
    }
}
