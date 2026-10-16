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
package org.openrewrite.javascript.internal.lock.npm;

import com.fasterxml.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.fixture;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.json;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.node;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.obj;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.opts;

/**
 * Parity with arborist over a dependency whose package.json holds a version or name of an unexpected JSON type
 * ({@code npm-arborist/generate/version-shapes.js}).
 */
class VersionShapesTest {

    private static final JsonNode SHAPES = fixture("version-shapes");

    private static final String GIT_RESOLVED =
            "git+ssh://git@github.com/a/b.git#0123456789abcdef0123456789abcdef01234567";

    static Stream<Arguments> versions() {
        List<Arguments> cases = new ArrayList<>();
        for (JsonNode c : SHAPES.get("versions")) {
            cases.add(Arguments.of(c.get("version").toString(), c.get("spec").asText(), c.get("expected")));
        }
        return cases.stream();
    }

    @ParameterizedTest(name = "version {0} against {1}")
    @MethodSource("versions")
    void readersOfVersion(String versionJson, String spec, JsonNode expected) {
        Object version = json(versionJson);
        Node root = node(opts().path("/r").loadOverrides(true)
                .pkg(obj("name", "root", "version", "1.0.0", "overrides", obj("w@" + spec, spec))));
        Node w = node(opts().parent(root).name("w").pkg(obj("name", "w", "version", version)).resolved(GIT_RESOLVED));
        Node x = node(opts().parent(root).name("x")
                .pkg(obj("name", "x", "version", "1.0.0", "dependencies", obj("w", spec))));
        Node y = node(opts().parent(x).name("w").pkg(obj("name", "w", "version", "2.0.0")));
        Node p = node(opts().parent(root).name("p")
                .pkg(obj("name", "p", "version", "1.0.0", "dependencies", obj("w", spec))));
        Node g = node(opts().parent(root).name("g")
                .pkg(obj("name", "g", "version", "1.0.0", "dependencies", obj("w", "github:a/b#semver:" + spec))));
        Node same = node(opts().path("/elsewhere/node_modules/w").pkg(obj("name", "w", "version", version)));
        Node string = node(opts().path("/elsewhere/node_modules/w")
                .pkg(obj("name", "w", "version", String.valueOf(version))));

        Map<String, Object> actual = new LinkedHashMap<>();
        actual.put("version", attempt(w::getVersion));
        actual.put("error", attempt(() -> p.getEdgesOut().get("w").getError()));
        actual.put("valid", attempt(() -> p.getEdgesOut().get("w").isValid()));
        actual.put("gitError", attempt(() -> g.getEdgesOut().get("w").getError()));
        actual.put("nodeRule", attempt(() -> root.getOverrides().getNodeRule(w).getKey()));
        actual.put("matchingRule", attempt(() -> {
            OverrideSet rule = root.getOverrides().getMatchingRule(w);
            return rule == null ? null : rule.getKey();
        }));
        actual.put("canDedupe", attempt(() -> y.canDedupe(false, false)));
        actual.put("canDedupePreferred", attempt(() -> y.canDedupe(true, false)));
        actual.put("matches", attempt(() -> w.matches(y)));
        actual.put("matchesSame", attempt(() -> w.matches(same)));
        actual.put("matchesString", attempt(() -> w.matches(string)));
        actual.put("overridden", attempt(w::isOverridden));
        actual.put("meta", attempt(() -> sortKeys(Shrinkwrap.metaFromNode(w, "/r", NpmOptions.DEFAULT))));
        assertThat(actual).isEqualTo(NpmTreeSupport.<Map<String, Object>>json(expected));
    }

    static Stream<Arguments> names() {
        List<Arguments> cases = new ArrayList<>();
        for (JsonNode c : SHAPES.get("names")) {
            cases.add(Arguments.of(c.get("name").toString(), c.get("expected")));
        }
        return cases.stream();
    }

    @ParameterizedTest(name = "name {0}")
    @MethodSource("names")
    void readersOfPackageName(String nameJson, JsonNode expected) {
        Object name = json(nameJson);
        Node root = node(opts().path("/r").pkg(obj("name", "root", "version", "1.0.0", "dependencies", obj("w", "1"))));
        Node w = node(opts().parent(root).name("w").pkg(obj("name", name, "version", "1.0.0")));
        Node same = node(opts().path("/elsewhere/node_modules/w").pkg(obj("name", name, "version", "1.0.0")));
        Node string = node(opts().path("/elsewhere/node_modules/w")
                .pkg(obj("name", String.valueOf(name), "version", "1.0.0")));
        Node other = node(opts().path("/elsewhere/node_modules/w").pkg(obj("name", "w", "version", "1.0.0")));

        Map<String, Object> actual = new LinkedHashMap<>();
        actual.put("packageName", attempt(w::getPackageName));
        actual.put("matchesSame", attempt(() -> w.matches(same)));
        actual.put("matchesString", attempt(() -> w.matches(string)));
        actual.put("canReplaceWith", attempt(() -> w.canReplaceWith(other)));
        actual.put("canReplaceWithSame", attempt(() -> w.canReplaceWith(same)));
        actual.put("isWorkspace", attempt(w::isWorkspace));
        actual.put("edgesOutGet", attempt(() -> root.getEdgesOut().get(w.getPackageName()) != null));
        actual.put("meta", attempt(() -> sortKeys(Shrinkwrap.metaFromNode(w, "/r", NpmOptions.DEFAULT))));
        assertThat(actual).isEqualTo(NpmTreeSupport.<Map<String, Object>>json(expected));
    }

    static Stream<Arguments> locked() {
        List<Arguments> cases = new ArrayList<>();
        for (JsonNode c : SHAPES.get("locked")) {
            cases.add(Arguments.of(c.get("lockVersion").toString(), c.get("version").toString(), c.get("expected")));
        }
        return cases.stream();
    }

    @ParameterizedTest(name = "lock entry version {0}, package version {1}")
    @MethodSource("locked")
    void lockEntryAppliesOnlyToTheSameVersion(String lockVersionJson, String versionJson, JsonNode expected) {
        Shrinkwrap meta = new Shrinkwrap("/r", NpmOptions.DEFAULT);
        meta.setData(obj("lockfileVersion", 3, "packages", obj("node_modules/w",
                obj("version", json(lockVersionJson), "resolved", "https://r/w.tgz", "integrity", "sha512-w"))));
        Node root = node(opts().path("/r").meta(meta)
                .pkg(obj("name", "root", "version", "1.0.0", "dependencies", obj("w", "*"))));
        Node w = node(opts().parent(root).name("w").pkg(obj("name", "w", "version", json(versionJson))));

        Map<String, Object> actual = new LinkedHashMap<>();
        actual.put("resolved", w.getResolved());
        actual.put("integrity", w.getIntegrity());
        assertThat(actual).isEqualTo(NpmTreeSupport.<Map<String, Object>>json(expected));
    }

    static Stream<Arguments> bundled() {
        List<Arguments> cases = new ArrayList<>();
        for (JsonNode c : SHAPES.get("bundled")) {
            cases.add(Arguments.of(c.get("bundleDependencies").toString(), c.get("expected")));
        }
        return cases.stream();
    }

    @ParameterizedTest(name = "bundleDependencies {0}")
    @MethodSource("bundled")
    void bundledReadsTheNormalizedOrTheRawList(String bundleDependenciesJson, JsonNode expected) {
        Node root = node(opts().path("/r").pkg(obj("name", "root", "version", "1.0.0", "dependencies", obj("w", "1"),
                "bundleDependencies", json(bundleDependenciesJson))));

        Map<String, Object> actual = new LinkedHashMap<>();
        actual.put("normalized", attempt(() -> root.getEdgesOut().get("w").isBundled()));
        root.setPackage(obj("name", "root", "version", "1.0.0", "dependencies", obj("w", "1"),
                "bundleDependencies", json(bundleDependenciesJson)));
        actual.put("raw", attempt(() -> root.getEdgesOut().get("w").isBundled()));
        assertThat(actual).isEqualTo(NpmTreeSupport.<Map<String, Object>>json(expected));
    }

    static Stream<Arguments> folder() {
        List<Arguments> cases = new ArrayList<>();
        for (JsonNode c : SHAPES.get("folder")) {
            cases.add(Arguments.of(c.get("name").toString(), c.get("expected").asText()));
        }
        return cases.stream();
    }

    @ParameterizedTest(name = "name {0}")
    @MethodSource("folder")
    void pathlessNodeTakesItsNameFromThePackage(String nameJson, String expected) {
        assertThat(attempt(() -> node(opts().pkg(obj("name", json(nameJson), "version", "1.0.0"))).getName()))
                .isEqualTo(expected);
    }

    private static @Nullable Object attempt(Callable<@Nullable Object> read) {
        try {
            return read.call();
        } catch (Exception e) {
            return "THROW: " + e.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Object sortKeys(@Nullable Object value) {
        if (value instanceof List) {
            List<Object> sorted = new ArrayList<>();
            for (Object element : (List<Object>) value) {
                sorted.add(sortKeys(element));
            }
            return sorted;
        }
        if (value instanceof Map) {
            Map<String, Object> sorted = new TreeMap<>();
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) value).entrySet()) {
                sorted.put(entry.getKey(), sortKeys(entry.getValue()));
            }
            return sorted;
        }
        return value;
    }
}
