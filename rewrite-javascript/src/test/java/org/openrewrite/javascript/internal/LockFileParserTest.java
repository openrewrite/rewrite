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

import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.marker.NodeResolutionResult.ResolvedDependency;

import static org.assertj.core.api.Assertions.assertThat;

class LockFileParserTest {

    @Test
    void parsesTopLevelDependency() {
        String lock = "{\n" +
                "  \"lockfileVersion\": 3,\n" +
                "  \"packages\": {\n" +
                "    \"\": { \"name\": \"my-app\" },\n" +
                "    \"node_modules/lodash\": {\n" +
                "      \"version\": \"4.17.21\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        LockFileParser.ParseResult result = LockFileParser.parse(lock);

        assertThat(result.getAll()).hasSize(1);
        ResolvedDependency dep = result.getAll().get(0);
        assertThat(dep.getName()).isEqualTo("lodash");
        assertThat(dep.getVersion()).isEqualTo("4.17.21");
        assertThat(result.getTopLevel()).containsKey("lodash");
        assertThat(result.getTopLevel().get("lodash")).isSameAs(dep);
    }

    @Test
    void parsesScopedTopLevelPackage() {
        String lock = "{\n" +
                "  \"packages\": {\n" +
                "    \"\": { },\n" +
                "    \"node_modules/@types/node\": { \"version\": \"20.0.0\" }\n" +
                "  }\n" +
                "}";
        LockFileParser.ParseResult result = LockFileParser.parse(lock);

        assertThat(result.getAll()).hasSize(1);
        assertThat(result.getAll().get(0).getName()).isEqualTo("@types/node");
        assertThat(result.getAll().get(0).getVersion()).isEqualTo("20.0.0");
        assertThat(result.getTopLevel()).containsKey("@types/node");
    }

    @Test
    void parsesNestedDependency() {
        String lock = "{\n" +
                "  \"packages\": {\n" +
                "    \"\": { },\n" +
                "    \"node_modules/foo\": { \"version\": \"1.0.0\" },\n" +
                "    \"node_modules/foo/node_modules/bar\": { \"version\": \"2.0.0\" }\n" +
                "  }\n" +
                "}";
        LockFileParser.ParseResult result = LockFileParser.parse(lock);

        assertThat(result.getAll()).extracting(d -> d.getName() + "@" + d.getVersion())
                .containsExactlyInAnyOrder("foo@1.0.0", "bar@2.0.0");
        // Only `foo` is top-level; `bar` is nested.
        assertThat(result.getTopLevel().keySet()).containsExactly("foo");
    }

    @Test
    void parsesScopedNestedPackage() {
        String lock = "{\n" +
                "  \"packages\": {\n" +
                "    \"\": { },\n" +
                "    \"node_modules/foo\": { \"version\": \"1.0.0\" },\n" +
                "    \"node_modules/foo/node_modules/@scope/bar\": { \"version\": \"2.0.0\" }\n" +
                "  }\n" +
                "}";
        LockFileParser.ParseResult result = LockFileParser.parse(lock);

        assertThat(result.getAll()).extracting(d -> d.getName() + "@" + d.getVersion())
                .containsExactlyInAnyOrder("foo@1.0.0", "@scope/bar@2.0.0");
        assertThat(result.getTopLevel().keySet()).containsExactly("foo");
    }
}
