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
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.ResolvedDependency;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        assertThat(result.getTopLevel().get("@types/node")).isSameAs(result.getAll().get(0));
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

    @Test
    void extractsTransitiveDependencies() {
        String lock = "{\n" +
                "  \"packages\": {\n" +
                "    \"\": { },\n" +
                "    \"node_modules/express\": {\n" +
                "      \"version\": \"4.18.0\",\n" +
                "      \"dependencies\": { \"accepts\": \"^1.3.8\", \"body-parser\": \"^1.20.0\" },\n" +
                "      \"devDependencies\": { \"mocha\": \"^10.0.0\" },\n" +
                "      \"peerDependencies\": { \"react\": \"*\" },\n" +
                "      \"optionalDependencies\": { \"fsevents\": \"^2.0.0\" }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        LockFileParser.ParseResult result = LockFileParser.parse(lock);
        ResolvedDependency express = result.getAll().get(0);

        assertThat(express.getDependencies())
                .extracting(d -> d.getName() + "@" + d.getVersionConstraint())
                .containsExactlyInAnyOrder("accepts@^1.3.8", "body-parser@^1.20.0");
        assertThat(express.getDevDependencies())
                .extracting(NodeResolutionResult.Dependency::getName).containsExactly("mocha");
        assertThat(express.getPeerDependencies())
                .extracting(NodeResolutionResult.Dependency::getName).containsExactly("react");
        assertThat(express.getOptionalDependencies())
                .extracting(NodeResolutionResult.Dependency::getName).containsExactly("fsevents");
        // None of these deps have their own entry in this lock, so they stay unresolved.
        assertThat(express.getDependencies()).allSatisfy(d ->
                assertThat(d.getResolved()).isNull());
    }

    @Test
    void linksTransitiveDependencies() {
        String lock = "{\n" +
                "  \"packages\": {\n" +
                "    \"\": { },\n" +
                "    \"node_modules/a\": { \"version\": \"1.0.0\", \"dependencies\": { \"b\": \"^1.0.0\" } },\n" +
                "    \"node_modules/b\": { \"version\": \"1.0.0\", \"dependencies\": { \"c\": \"^1.0.0\" } },\n" +
                "    \"node_modules/c\": { \"version\": \"1.0.0\" }\n" +
                "  }\n" +
                "}";
        LockFileParser.ParseResult result = LockFileParser.parse(lock);

        ResolvedDependency a = result.getTopLevel().get("a");
        NodeResolutionResult.Dependency bRequest = a.getDependencies().get(0);
        assertThat(bRequest.getResolved()).isSameAs(result.getTopLevel().get("b"));
        NodeResolutionResult.Dependency cRequest = bRequest.getResolved().getDependencies().get(0);
        assertThat(cRequest.getResolved()).isSameAs(result.getTopLevel().get("c"));
    }

    @Test
    void resolvesNestedShadowedVersion() {
        String lock = "{\n" +
                "  \"packages\": {\n" +
                "    \"\": { },\n" +
                "    \"node_modules/a\": { \"version\": \"1.0.0\", \"dependencies\": { \"b\": \"^1.0.0\" } },\n" +
                "    \"node_modules/b\": { \"version\": \"2.0.0\" },\n" +
                "    \"node_modules/a/node_modules/b\": { \"version\": \"1.5.0\" }\n" +
                "  }\n" +
                "}";
        LockFileParser.ParseResult result = LockFileParser.parse(lock);

        // Top-level b is 2.0.0, but a's nested node_modules shadow it with 1.5.0.
        assertThat(result.getTopLevel().get("b").getVersion()).isEqualTo("2.0.0");
        ResolvedDependency a = result.getTopLevel().get("a");
        NodeResolutionResult.Dependency bRequest = a.getDependencies().get(0);
        assertThat(bRequest.getResolved()).isNotNull();
        assertThat(bRequest.getResolved().getVersion()).isEqualTo("1.5.0");
    }

    @Test
    void fallsBackToSemverMatchForPathsOutsideTheWalk() {
        // b exists only under a synthetic path (the shape the yarn/pnpm adapters
        // emit for duplicate versions) that no node_modules walk-up can reach.
        String lock = "{\n" +
                "  \"packages\": {\n" +
                "    \"\": { },\n" +
                "    \"node_modules/a\": { \"version\": \"1.0.0\", \"dependencies\": { \"b\": \"^1.0.0\" } },\n" +
                "    \"node_modules/.pnpm/0/node_modules/b\": { \"version\": \"1.5.0\" },\n" +
                "    \"node_modules/.pnpm/1/node_modules/b\": { \"version\": \"2.0.0\" }\n" +
                "  }\n" +
                "}";
        LockFileParser.ParseResult result = LockFileParser.parse(lock);

        ResolvedDependency a = result.getTopLevel().get("a");
        NodeResolutionResult.Dependency bRequest = a.getDependencies().get(0);
        assertThat(bRequest.getResolved()).isNotNull();
        assertThat(bRequest.getResolved().getVersion()).isEqualTo("1.5.0");
    }

    @Test
    void linksCyclicDependencies() {
        String lock = "{\n" +
                "  \"packages\": {\n" +
                "    \"\": { },\n" +
                "    \"node_modules/a\": { \"version\": \"1.0.0\", \"dependencies\": { \"b\": \"^1.0.0\" } },\n" +
                "    \"node_modules/b\": { \"version\": \"1.0.0\", \"dependencies\": { \"a\": \"^1.0.0\" } }\n" +
                "  }\n" +
                "}";
        LockFileParser.ParseResult result = LockFileParser.parse(lock);

        ResolvedDependency a = result.getTopLevel().get("a");
        ResolvedDependency b = result.getTopLevel().get("b");
        assertThat(a.getDependencies().get(0).getResolved()).isSameAs(b);
        assertThat(b.getDependencies().get(0).getResolved()).isSameAs(a);
    }

    @Test
    void extractsEnginesAndLicense() {
        String lock = "{\n" +
                "  \"packages\": {\n" +
                "    \"\": { },\n" +
                "    \"node_modules/lodash\": {\n" +
                "      \"version\": \"4.17.21\",\n" +
                "      \"engines\": { \"node\": \">=12\" },\n" +
                "      \"license\": \"MIT\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        LockFileParser.ParseResult result = LockFileParser.parse(lock);
        ResolvedDependency lodash = result.getAll().get(0);

        assertThat(lodash.getEngines()).containsExactly(Map.entry("node", ">=12"));
        assertThat(lodash.getLicense()).isEqualTo("MIT");
    }

    @Test
    void skipsRootEntry() {
        String lock = "{\n" +
                "  \"packages\": {\n" +
                "    \"\": { \"name\": \"my-app\", \"version\": \"1.0.0\" }\n" +
                "  }\n" +
                "}";
        LockFileParser.ParseResult result = LockFileParser.parse(lock);
        assertThat(result.getAll()).isEmpty();
        assertThat(result.getTopLevel()).isEmpty();
    }

    @Test
    void toleratesUnknownExtraFields() {
        String lock = "{\n" +
                "  \"lockfileVersion\": 3,\n" +
                "  \"name\": \"my-app\",\n" +
                "  \"requires\": true,\n" +
                "  \"newFutureField\": { \"foo\": \"bar\" },\n" +
                "  \"packages\": {\n" +
                "    \"\": { },\n" +
                "    \"node_modules/lodash\": {\n" +
                "      \"version\": \"4.17.21\",\n" +
                "      \"resolved\": \"https://registry.npmjs.org/lodash/-/lodash-4.17.21.tgz\",\n" +
                "      \"integrity\": \"sha512-...\",\n" +
                "      \"unknownExtra\": 42\n" +
                "    }\n" +
                "  }\n" +
                "}";
        LockFileParser.ParseResult result = LockFileParser.parse(lock);
        assertThat(result.getAll()).hasSize(1);
        assertThat(result.getAll().get(0).getName()).isEqualTo("lodash");
    }

    @Test
    void throwsOnMalformedJson() {
        assertThatThrownBy(() -> LockFileParser.parse("{ not valid json"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("malformed lock JSON");
    }

    @Test
    void throwsOnMissingPackagesKey() {
        assertThatThrownBy(() -> LockFileParser.parse("{ \"lockfileVersion\": 3 }"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("packages");
    }
}
