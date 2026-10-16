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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.Ssri;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * yarn 1.22.22's __tests__/lockfile.js, and lock entries holding other types as yarn's own code wrote them back.
 */
class LockfileTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void parseStringify() {
        assertThat(roundTrip(object("foo", "bar"))).isEqualTo(object("foo", "bar"));
        assertThat(roundTrip(object("foo", new LinkedHashMap<>()))).isEqualTo(object("foo", new LinkedHashMap<>()));
        Map<String, @Nullable Object> two = object("foo", "foo");
        two.put("bar", "bar");
        assertThat(roundTrip(two)).isEqualTo(two);
        assertThat(roundTrip(object("foo", 5.0))).isEqualTo(object("foo", 5.0));
    }

    @Test
    void parse() {
        assertThat(parsed("foo:\n  bar\n")).isEqualTo(object("foo", "bar"));
        assertThat(parsed("foo \"bar\"")).isEqualTo(object("foo", "bar"));
        assertThat(parsed("\"foo\" \"bar\"")).isEqualTo(object("foo", "bar"));
        assertThat(parsed("foo:\n  bar \"bar\"")).isEqualTo(object("foo", object("bar", "bar")));
        Map<String, @Nullable Object> barAndFoo = object("bar", new LinkedHashMap<>());
        barAndFoo.put("foo", "bar");
        assertThat(parsed("foo:\n  bar:\n  foo \"bar\"")).isEqualTo(object("foo", barAndFoo));
        assertThat(parsed("foo:\n  bar:\n    foo \"bar\""))
                .isEqualTo(object("foo", object("bar", object("foo", "bar"))));
        assertThat(parsed("foo:\r\n  bar:\r\n    foo \"bar\""))
                .isEqualTo(object("foo", object("bar", object("foo", "bar"))));
        Map<String, @Nullable Object> nested = object("foo", object("bar", object("yes", "no")));
        nested.put("bar", object("yes", "no"));
        assertThat(parsed("foo:\n  bar:\n    yes no\nbar:\n  yes no")).isEqualTo(nested);
        assertThat(parsed("foo:\r\n  bar:\r\n    yes no\r\nbar:\r\n  yes no")).isEqualTo(nested);
    }

    @Test
    void stringify() {
        Map<String, @Nullable Object> obj = object("foo", "bar");
        Map<String, @Nullable Object> both = object("a", obj);
        both.put("b", obj);
        assertThat(LockfileStringifier.stringify(both, true, false, null)).isEqualTo("a, b:\n  foo bar\n");
    }

    @Test
    void sortsALongMapHoldingAnInheritedNameAsV8Does() throws IOException {
        Map<String, @Nullable Object> dependencies = new LinkedHashMap<>();
        for (int i = 0; i < 63; i++) {
            dependencies.put("dep-" + i, "1.0.0");
        }
        dependencies.put("constructor", "1.0.0");
        assertThat(LockfileStringifier.stringify(object("a@1.0.0", object("dependencies", dependencies)), false, false,
                null)).isEqualTo(resource("long-map-inherited-name.txt"));
        dependencies.remove("dep-0");
        assertThat(LockfileStringifier.stringify(object("a@1.0.0", object("dependencies", dependencies)), false, false,
                null)).contains("  dependencies:\n    constructor \"1.0.0\"\n");
    }

    static Stream<Arguments> inheritedNameMaps() throws IOException {
        List<Arguments> rows = new ArrayList<>();
        InputStream gzipped = requireNonNull(LockfileTest.class.getResourceAsStream("inherited-name-maps.jsonl.gz"));
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(gzipped),
                StandardCharsets.UTF_8))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                JsonNode row = MAPPER.readTree(line);
                rows.add(Arguments.of(rows.size() + 1, row));
            }
        }
        return rows.stream();
    }

    // yarn's comparator orders two inherited names by the strings `>` converts their functions to
    @ParameterizedTest(name = "map {0}")
    @MethodSource("inheritedNameMaps")
    void sortsAMapHoldingSeveralInheritedNamesAsYarnDoes(int map, JsonNode row) {
        Map<String, @Nullable Object> dependencies = new LinkedHashMap<>();
        for (int i = 0; i < row.get("keys").size(); i++) {
            dependencies.put(row.get("keys").get(i).asText(), row.get("values").get(i).asText());
        }
        assertThat(LockfileStringifier.stringify(object("a@1.0.0", object("dependencies", dependencies)), false, false,
                null)).isEqualTo(row.get("out").asText());
    }

    @Test
    void getLocked() {
        Map<String, @Nullable Object> cache = object("foo", "bar");
        cache.put("bar", new LinkedHashMap<>());
        assertThat(new Lockfile(cache, null, null).getLocked("foo")).isNotNull();
    }

    @Test
    void getLockedNoCache() {
        assertThat(new Lockfile().getLocked("foobar")).isNull();
    }

    @Test
    void getLockedDefaults() {
        Map<String, @Nullable Object> pattern = new Lockfile(object("foobar", object("version", "0.0.0")), null, null)
                .getLocked("foobar");
        assertThat(pattern).isNotNull();
        assertThat(pattern.get("registry")).isEqualTo("npm");
        assertThat(pattern.get("uid")).isEqualTo("0.0.0");
        assertThat(pattern.get("version")).isEqualTo("0.0.0");
    }

    @Test
    void getLockedUnknown() {
        assertThat(new Lockfile(new LinkedHashMap<>(), null, null).getLocked("foobar")).isNull();
    }

    @Test
    void getLockfile() {
        Lockfile lockfile = new Lockfile();
        PackageResolver resolver = YarnClassicScenarios.resolver(YarnClassicScenarios.defaults(), lockfile);
        Manifest foobar = manifest(resolver, "foobar", "0.0.0", "0.0.0", "http://example.com/foobar", "npm");
        Manifest barfoo = manifest(resolver, "barfoo", "0.0.1", "0.1.0", "http://example.com/barfoo", "yarn");
        barfoo.put("dependencies", object("yes", "no"));
        barfoo.put("optionalDependencies", object("no", "yes"));
        barfoo.reference.setPermission("foo", "bar");
        Map<String, Manifest> patterns = new LinkedHashMap<>();
        patterns.put("foobar", foobar);
        patterns.put("barfoo", barfoo);
        patterns.put("foobar@2", foobar);

        Map<String, @Nullable Object> actual = lockfile.getLockfile(patterns);

        Map<String, @Nullable Object> expectedFoobar = entry("0.0.0", null, "http://example.com/foobar", null);
        Map<String, @Nullable Object> expectedBarfoo = entry("0.0.1", "0.1.0", "http://example.com/barfoo", "yarn");
        expectedBarfoo.put("dependencies", object("yes", "no"));
        expectedBarfoo.put("optionalDependencies", object("no", "yes"));
        expectedBarfoo.put("permissions", object("foo", "bar"));
        assertThat(defined(actual.get("barfoo"))).isEqualTo(expectedBarfoo);
        assertThat(defined(actual.get("foobar"))).isEqualTo(expectedFoobar);
        assertThat(actual.get("foobar@2")).isSameAs(actual.get("foobar"));
    }

    @Test
    void getLockfileSorting() {
        Lockfile lockfile = new Lockfile();
        PackageResolver resolver = YarnClassicScenarios.resolver(YarnClassicScenarios.defaults(), lockfile);
        Manifest foobar = manifest(resolver, "foobar", "0.0.0", null, "http://example.com/foobar", "npm");
        Map<String, Manifest> patterns = new LinkedHashMap<>();
        patterns.put("foobar2", foobar);
        patterns.put("foobar1", foobar);

        Map<String, @Nullable Object> actual = lockfile.getLockfile(patterns);

        // upstream expects name 'foobar', which 1.22.22's own getLockfile (run from its bundle) does not write
        assertThat(defined(actual.get("foobar1"))).isEqualTo(entry("0.0.0", null, "http://example.com/foobar", null));
        assertThat(defined(actual.get("foobar2"))).isEqualTo(entry("0.0.0", null, "http://example.com/foobar", null));
    }

    @Test
    void getLockfileHandlesIntegrityField() {
        Lockfile lockfile = new Lockfile();
        PackageResolver resolver = YarnClassicScenarios.resolver(YarnClassicScenarios.defaults(), lockfile);
        Manifest foobar = manifest(resolver, "foobar", "0.0.0", null, "http://example.com/foobar", "npm");
        Ssri.Integrity integrity = Ssri.parse("sha1-foo sha512-bar", Ssri.Release.V5_3_0);
        foobar.remote.integrity = integrity;
        Map<String, Manifest> patterns = new LinkedHashMap<>();
        patterns.put("foobar", foobar);

        Map<String, @Nullable Object> expected = entry("0.0.0", null, "http://example.com/foobar", null);
        expected.put("integrity", "sha1-foo sha512-bar");
        assertThat(defined(lockfile.getLockfile(patterns).get("foobar"))).isEqualTo(expected);
        // yarn records no integrity before 1.10.0
        assertThat(defined(lockfile.getLockfile(patterns, YarnClassicRelease.of("1.9.4")).get("foobar")))
                .isEqualTo(entry("0.0.0", null, "http://example.com/foobar", null));
    }

    /**
     * Upstream's sorting case as the releases before 1.22.22 pass it: the entry two patterns share by remote names the
     * package their names do not infer.
     */
    @Test
    void getLockfileSortingBefore12222() {
        Lockfile lockfile = new Lockfile();
        PackageResolver resolver = YarnClassicScenarios.resolver(YarnClassicScenarios.defaults(), lockfile);
        Manifest foobar = manifest(resolver, "foobar", "0.0.0", null, "http://example.com/foobar", "npm");
        Map<String, Manifest> patterns = new LinkedHashMap<>();
        patterns.put("foobar2", foobar);
        patterns.put("foobar1", foobar);

        Map<String, @Nullable Object> actual = lockfile.getLockfile(patterns, YarnClassicRelease.of("1.22.21"));

        Map<String, @Nullable Object> expected = entry("0.0.0", null, "http://example.com/foobar", null);
        Map<String, @Nullable Object> named = new LinkedHashMap<>();
        named.put("name", "foobar");
        named.putAll(expected);
        assertThat(defined(actual.get("foobar1"))).isEqualTo(named);
        assertThat(actual.get("foobar2")).isSameAs(actual.get("foobar1"));
    }

    @Test
    void parseSingleMergeConflict() {
        LockfileParser.ParseResult result = LockfileParser.parseLockfile("\na:\n  no \"yes\"\n\n<<<<<<< HEAD\nb:\n" +
                "  foo \"bar\"\n=======\nc:\n  bar \"foo\"\n>>>>>>> branch-a\n\nd:\n  yes \"no\"\n");
        assertThat(result.getType()).isEqualTo("merge");
        assertThat(result.getObject()).isEqualTo(abcd());
    }

    @Test
    void parseSingleMergeConflictWithCrlf() {
        LockfileParser.ParseResult result = LockfileParser.parseLockfile("a:\r\n  no \"yes\"\r\n\r\n<<<<<<< HEAD\r\n" +
                "b:\r\n  foo \"bar\"\r\n=======\r\nc:\r\n  bar \"foo\"\r\n>>>>>>> branch-a\r\n\r\nd:\r\n" +
                "  yes \"no\"\r\n");
        assertThat(result.getType()).isEqualTo("merge");
        assertThat(result.getObject()).isEqualTo(abcd());
    }

    @Test
    void parseMultipleMergeConflicts() {
        LockfileParser.ParseResult result = LockfileParser.parseLockfile("\na:\n  no \"yes\"\n\n<<<<<<< HEAD\nb:\n" +
                "  foo \"bar\"\n=======\nc:\n  bar \"foo\"\n>>>>>>> branch-a\n\nd:\n  yes \"no\"\n\n<<<<<<< HEAD\n" +
                "e:\n  foo \"bar\"\n=======\nf:\n  bar \"foo\"\n>>>>>>> branch-b\n");
        Map<String, @Nullable Object> expected = abcd();
        expected.put("e", object("foo", "bar"));
        expected.put("f", object("bar", "foo"));
        assertThat(result.getType()).isEqualTo("merge");
        assertThat(result.getObject()).isEqualTo(expected);
    }

    @Test
    void parseMergeConflictFail() {
        LockfileParser.ParseResult result = LockfileParser.parseLockfile("\n<<<<<<< HEAD\nb:\n  foo: \"bar\n" +
                "=======\nc:\n  bar \"foo\"\n>>>>>>> branch-a\n");
        assertThat(result.getType()).isEqualTo("conflict");
        assertThat((Map<?, ?>) result.getObject()).isEmpty();
    }

    @Test
    void discardsCommonAncestorsInMergeConflicts() {
        LockfileParser.ParseResult result = LockfileParser.parseLockfile("\n<<<<<<< HEAD\nb:\n  foo \"bar\"\n" +
                "||||||| common ancestor\nd:\n  yes \"no\"\n=======\nc:\n  bar \"foo\"\n>>>>>>> branch-a\n");
        Map<String, @Nullable Object> expected = object("b", object("foo", "bar"));
        expected.put("c", object("bar", "foo"));
        assertThat(result.getType()).isEqualTo("merge");
        assertThat(result.getObject()).isEqualTo(expected);
    }

    @Test
    void newerLockfileVersionIsRejected() {
        assertThatThrownBy(() -> Lockfile.fromDirectory("# yarn lockfile v2\n\nfoo:\n  version \"1.0.0\"\n"))
                .isInstanceOf(MessageError.class)
                .hasMessage("Can't install from a lockfile of version 2 as you're on an old yarn version that only " +
                            "supports versions up to 1. Run `$ yarn self-update` to upgrade to the latest version.");
    }

    @Test
    void yarnBerryLockIsSetAside() {
        Lockfile lockfile = Lockfile.fromDirectory("__metadata:\n  version: 6\n\n\"ms@npm:^2.1.3\":\n" +
                                                   "  version: 2.1.3\n");
        assertThat(lockfile.getLockfilev2()).isNotNull();
        assertThat((Map<?, ?>) lockfile.getCache()).isEmpty();
    }

    /**
     * An array js-yaml read is an entry yarn's own {@code getLocked} explodes, its indices kept, the same each time.
     */
    @Test
    void getLockedExplodesAnArrayEntry() {
        Lockfile lockfile = Lockfile.fromDirectory("ms@^2.0.0: [a, b]\nx@1: [a, b]\n");
        Map<String, @Nullable Object> entry = lockfile.getLocked("ms@^2.0.0");
        assertThat(entry).isNotNull();
        assertThat(entry.keySet()).containsExactly("0", "1", "optionalDependencies", "dependencies", "uid",
                "permissions", "registry", "name");
        assertThat(entry.get("1")).isEqualTo("b");
        assertThat(entry.get("name")).isEqualTo("ms");
        assertThat(lockfile.getLocked("ms@^2.0.0")).isSameAs(entry);
    }

    static Stream<Arguments> entries() throws IOException {
        return rows("lock-entries.tsv");
    }

    static Stream<Arguments> reusedEntries() throws IOException {
        return rows("lock-entries-reused.tsv");
    }

    // a reference per pattern, which copies the entry's permissions, sorted as the round trip reuses them
    @ParameterizedTest(name = "{0}")
    @MethodSource("reusedEntries")
    void writesBackAReusedEntryAsYarnDoes(String entries, JsonNode outcome) {
        String answer;
        try {
            Lockfile lockfile = Lockfile.fromDirectory("# yarn lockfile v1\n\n\n" + entries);
            PackageResolver resolver = YarnClassicScenarios.resolver(YarnClassicScenarios.defaults(), lockfile);
            List<String> keys = new ArrayList<>(Js.keys(lockfile.getCache()));
            keys.sort(String::compareTo);
            for (String pattern : keys) {
                PackageRequest request = new PackageRequest(DependencyRequestPattern.builder().pattern(pattern)
                        .registry("npm").build(), resolver);
                Manifest info = request.getLocked("tarball");
                if (info != null) {
                    PackageReference ref = new PackageReference(request, info, requireNonNull(info.remote));
                    ref.addPattern(pattern, info);
                    info.reference = ref;
                }
            }
            answer = LockfileStringifier.stringify(lockfile.getLockfile(resolver.patterns), false, false, null);
        } catch (JsError e) {
            answer = e.getName() + ": " + e.getMessage();
        }
        assertThat(answer).isEqualTo(outcome.has("wrote") ? outcome.get("wrote").asText() :
                outcome.get("threw").asText());
    }

    private static Stream<Arguments> rows(String resource) throws IOException {
        List<Arguments> rows = new ArrayList<>();
        try (InputStream is = requireNonNull(LockfileTest.class.getResourceAsStream(resource));
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("#")) {
                    String[] cells = line.split("\t", -1);
                    String lock = MAPPER.readValue(cells[0], String.class);
                    rows.add(Arguments.of(lock.substring(lock.indexOf("\n\n\n") + 3), MAPPER.readTree(cells[1])));
                }
            }
        }
        return rows.stream();
    }

    private static String resource(String resource) throws IOException {
        try (InputStream is = requireNonNull(LockfileTest.class.getResourceAsStream(resource))) {
            return StringUtils.readFully(is, StandardCharsets.UTF_8);
        }
    }

    // entries whose fields hold other types, each pattern reused as the round trip does; yarn's answers recorded
    @ParameterizedTest(name = "{0}")
    @MethodSource("entries")
    void writesBackAnEntryAsYarnDoes(String entries, JsonNode outcome) {
        String answer;
        try {
            Lockfile lockfile = Lockfile.fromDirectory("# yarn lockfile v1\n\n\n" + entries);
            PackageResolver resolver = YarnClassicScenarios.resolver(YarnClassicScenarios.defaults(), lockfile);
            Map<String, Manifest> patterns = new LinkedHashMap<>();
            for (String pattern : Js.keys(lockfile.getCache())) {
                PackageRequest request = new PackageRequest(DependencyRequestPattern.builder().pattern(pattern)
                        .registry("npm").build(), resolver);
                Manifest info = request.getLocked("tarball");
                if (info != null) {
                    info.reference = new PackageReference(request, info, requireNonNull(info.remote));
                    patterns.put(pattern, info);
                }
            }
            answer = LockfileStringifier.stringify(lockfile.getLockfile(patterns), false, false, null);
        } catch (JsError e) {
            answer = e.getName() + ": " + e.getMessage();
        }
        assertThat(answer).isEqualTo(outcome.has("wrote") ? outcome.get("wrote").asText() :
                outcome.get("threw").asText());
    }

    private static Map<String, @Nullable Object> abcd() {
        Map<String, @Nullable Object> expected = object("a", object("no", "yes"));
        expected.put("b", object("foo", "bar"));
        expected.put("c", object("bar", "foo"));
        expected.put("d", object("yes", "no"));
        return expected;
    }

    private static Manifest manifest(PackageResolver resolver, String name, String version, @Nullable String uid,
                                     String resolved, String registry) {
        Manifest manifest = new Manifest();
        manifest.put("name", name);
        manifest.put("version", version);
        manifest.put("_uid", uid);
        manifest.put("dependencies", new LinkedHashMap<>());
        manifest.put("optionalDependencies", new LinkedHashMap<>());
        PackageRemote remote = new PackageRemote();
        remote.resolved = resolved;
        remote.registry = registry;
        manifest.remote = remote;
        PackageRequest request = new PackageRequest(DependencyRequestPattern.builder().pattern(name).registry("npm")
                .build(), resolver);
        manifest.reference = new PackageReference(request, manifest, remote);
        return manifest;
    }

    private static Map<String, @Nullable Object> entry(String version, @Nullable String uid, String resolved,
                                                      @Nullable String registry) {
        Map<String, @Nullable Object> entry = new LinkedHashMap<>();
        entry.put("version", version);
        if (uid != null) {
            entry.put("uid", uid);
        }
        entry.put("resolved", resolved);
        if (registry != null) {
            entry.put("registry", registry);
        }
        return entry;
    }

    // toEqual ignores undefined properties
    private static Map<String, @Nullable Object> defined(@Nullable Object entry) {
        Map<String, @Nullable Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : ((Map<?, ?>) entry).entrySet()) {
            if (e.getValue() != null) {
                out.put((String) e.getKey(), e.getValue());
            }
        }
        return out;
    }

    private static Map<String, @Nullable Object> object(String key, @Nullable Object value) {
        Map<String, @Nullable Object> map = new LinkedHashMap<>();
        map.put(key, value);
        return map;
    }

    private static @Nullable Object parsed(String text) {
        return LockfileParser.parseLockfile(text).getObject();
    }

    private static @Nullable Object roundTrip(Map<String, @Nullable Object> obj) {
        return parsed(LockfileStringifier.stringify(obj, false, false, null));
    }
}
