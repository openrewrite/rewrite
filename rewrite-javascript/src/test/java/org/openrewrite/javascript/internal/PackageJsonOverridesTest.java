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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.SourceFile;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.json.JsonParser;
import org.openrewrite.json.tree.Json;
import org.openrewrite.marker.Markup;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager.Bun;
import static org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager.Npm;
import static org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager.Pnpm;
import static org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager.YarnBerry;
import static org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager.YarnClassic;

/**
 * Where each package manager reads an override and how the recipe writes one; the pnpm-workspace.yaml rows are
 * pnpm's own writes, recorded with {@code pnpm config set --location=project --json overrides}.
 */
class PackageJsonOverridesTest {

    private static final String WRITES = "lock/upgrade-transitive/pnpm-writes/";

    /**
     * Cases where pnpm rewrites what the edit leaves alone (indentation, pnpm 10's key order); the recipe keeps it.
     */
    private static final List<String> RE_RENDERED = Arrays.asList("11.2.2/overrides-four-spaces",
            "11.2.2/packages-sequence-unindented", "10.34.6/overrides-four-spaces",
            "10.34.6/packages-sequence-unindented", "10.34.6/unordered", "10.34.6/overrides-unordered",
            "10.34.6/overrides-empty", "10.34.6/blank-lines-first");

    static Stream<Arguments> writes() throws IOException, URISyntaxException {
        Path root = Paths.get(Objects.requireNonNull(PackageJsonOverridesTest.class.getClassLoader()
                .getResource(WRITES)).toURI());
        List<Arguments> cases = new ArrayList<>();
        try (Stream<Path> releases = Files.list(root)) {
            for (Path release : (Iterable<Path>) releases.sorted()::iterator) {
                try (Stream<Path> dirs = Files.list(release)) {
                    dirs.sorted().forEach(d -> cases.add(Arguments.of(release.getFileName() + "/" + d.getFileName(),
                            d)));
                }
            }
        }
        return cases.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("writes")
    void writesTheWorkspaceManifestAsPnpmDoes(String name, Path dir) throws IOException {
        String release = name.substring(0, name.indexOf('/'));
        Map<String, String> set = new ObjectMapper().readValue(read(dir.resolve("config-set")),
                new TypeReference<LinkedHashMap<String, String>>() {
                });
        Map.Entry<String, String> added = new ArrayList<>(set.entrySet()).get(set.size() - 1);
        Json.Document root = manifest("{\"name\":\"p\",\"packageManager\":\"pnpm@" + release + "\"}");
        String before = Files.exists(dir.resolve("workspace-before")) ? read(dir.resolve("workspace-before")) : null;
        int gt = added.getKey().indexOf('>');
        List<DependencyPathSegment> path = gt < 0 ? null :
                PackageJsonOverrides.parsePath(added.getKey().substring(0, gt));
        String written = PackageJsonOverrides.applyWorkspaceOverride(before, root,
                gt < 0 ? added.getKey() : added.getKey().substring(gt + 1), added.getValue(), path);
        String pnpm = read(dir.resolve("workspace"));
        if (RE_RENDERED.contains(name)) {
            assertThat((Map<?, ?>) new Yaml().load(written)).isEqualTo(new Yaml().load(pnpm));
        } else {
            assertThat(written).isEqualTo(pnpm);
        }
    }

    @Test
    void aWorkspaceManifestItDoesNotEditInPlaceIsRefused() {
        Json.Document root = manifest("{\"name\":\"p\"}");
        assertThatThrownBy(() -> PackageJsonOverrides.applyWorkspaceOverride("overrides: {a: 1.0.0}\n", root,
                "is-number", "7.0.0", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PackageJsonOverrides.applyWorkspaceOverride("---\npackages: []\n", root,
                "is-number", "7.0.0", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PackageJsonOverrides.applyWorkspaceOverride("base: &b 1\nother: *b\n", root,
                "is-number", "7.0.0", null)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
      "{} ; 11",
      "{\"packageManager\":\"pnpm@10.34.6\"} ; 10",
      "{\"packageManager\":\"pnpm@9.15.9+sha512.abc\"} ; 9",
      "{\"packageManager\":\"pnpm@^9.0.0\"} ; 11",
      "{\"packageManager\":\"yarn@4.10.3\"} ; 11",
      "{\"devEngines\":{\"packageManager\":{\"name\":\"pnpm\",\"version\":\"^10.0.0\"}}} ; 10",
      "{\"devEngines\":{\"packageManager\":{\"name\":\"pnpm\",\"version\":\">=9\"}}} ; 11",
      "{\"devEngines\":{\"packageManager\":{\"name\":\"pnpm\",\"version\":\"9 || 10\"}}} ; -1",
      "{\"devEngines\":{\"packageManager\":{\"name\":\"pnpm\",\"version\":\"^12.0.0\"}}} ; 12",
      "{\"devEngines\":{\"packageManager\":{\"name\":\"pnpm\"}},\"packageManager\":\"pnpm@10.34.6\"} ; 11",
      "{\"devEngines\":{\"packageManager\":[{\"name\":\"npm\"},{\"name\":\"pnpm\",\"version\":\"9.15.9\"}]}} ; 9",
    })
    void thePnpmReleaseIsTheOnePnpmRuns(String root, int release) {
        assertThat(PackageJsonOverrides.pnpmRelease(manifest(root))).isEqualTo(release);
    }

    @Test
    void eachPackageManagerReadsItsOwnPlace() {
        Json.Document plain = manifest("{\"name\":\"p\"}");
        assertThat(PackageJsonOverrides.place(Npm, plain)).isEqualTo(PackageJsonOverrides.Place.OVERRIDES);
        assertThat(PackageJsonOverrides.place(Bun, plain)).isEqualTo(PackageJsonOverrides.Place.OVERRIDES);
        assertThat(PackageJsonOverrides.place(Bun, manifest("{\"resolutions\":{}}")))
          .isEqualTo(PackageJsonOverrides.Place.RESOLUTIONS);
        assertThat(PackageJsonOverrides.place(Bun, manifest("{\"overrides\":{},\"resolutions\":{}}")))
          .isEqualTo(PackageJsonOverrides.Place.OVERRIDES);
        assertThat(PackageJsonOverrides.place(YarnClassic, plain)).isEqualTo(PackageJsonOverrides.Place.RESOLUTIONS);
        assertThat(PackageJsonOverrides.place(YarnBerry, plain)).isEqualTo(PackageJsonOverrides.Place.RESOLUTIONS);
        assertThat(PackageJsonOverrides.place(Pnpm, plain)).isEqualTo(PackageJsonOverrides.Place.WORKSPACE_MANIFEST);
        assertThat(PackageJsonOverrides.place(Pnpm, manifest("{\"packageManager\":\"pnpm@10.34.6\"}")))
          .isEqualTo(PackageJsonOverrides.Place.WORKSPACE_MANIFEST);
        assertThat(PackageJsonOverrides.place(Pnpm, manifest("{\"packageManager\":\"pnpm@10.34.6\",\"pnpm\":{}}")))
          .isEqualTo(PackageJsonOverrides.Place.PNPM_FIELD);
        assertThat(PackageJsonOverrides.place(Pnpm, manifest("{\"packageManager\":\"pnpm@10.34.6\",\"pnpm\":null}")))
          .isEqualTo(PackageJsonOverrides.Place.WORKSPACE_MANIFEST);
        assertThat(PackageJsonOverrides.place(Pnpm, manifest("{\"packageManager\":\"pnpm@9.15.9\"}")))
          .isEqualTo(PackageJsonOverrides.Place.PNPM_FIELD);
    }

    @Test
    void anOverrideThePackageManagerCannotReadIsDeclined() {
        Json.Document plain = manifest("{\"name\":\"p\"}");
        List<DependencyPathSegment> oneParent = PackageJsonOverrides.parsePath("is-odd");
        List<DependencyPathSegment> twoParents = PackageJsonOverrides.parsePath("is-even>is-odd");
        assertThat(PackageJsonOverrides.declined(Npm, plain, twoParents)).isNull();
        assertThat(PackageJsonOverrides.declined(Bun, plain, oneParent)).contains("nested overrides");
        assertThat(PackageJsonOverrides.declined(Pnpm, plain, oneParent)).isNull();
        assertThat(PackageJsonOverrides.declined(Pnpm, plain, twoParents)).contains("ERR_PNPM_INVALID_SELECTOR");
        assertThat(PackageJsonOverrides.declined(Pnpm, manifest("{\"packageManager\":\"pnpm@8.15.9\"}"), null))
          .contains("pnpm 8.x");
        assertThat(PackageJsonOverrides.declined(YarnBerry, plain, oneParent)).isNull();
        assertThat(PackageJsonOverrides.declined(YarnBerry, plain, twoParents)).contains("YN0057");
        assertThat(PackageJsonOverrides.declined(YarnClassic, plain, twoParents)).isNull();
        assertThat(PackageJsonOverrides.declined(YarnClassic, plain, PackageJsonOverrides.parsePath("is-odd@3.0.1")))
          .contains("is-odd@3.0.1");
        assertThat(PackageJsonOverrides.declined(Npm, manifest("{\"overrides\":\"x\"}"), null))
          .contains("not an object");
    }

    @Test
    void eachPackageManagerGetsItsOwnKey() {
        String pkg = "{\n  \"name\": \"p\"\n}\n";
        assertThat(write(pkg, YarnClassic, "is-even>is-odd")).contains("\"**/is-even/is-odd/is-number\": \"7.0.0\"");
        assertThat(write(pkg, YarnBerry, "is-odd@3.0.1")).contains("\"is-odd@3.0.1/is-number\": \"7.0.0\"");
        assertThat(write("{\n  \"name\": \"p\",\n  \"packageManager\": \"pnpm@9.15.9\"\n}\n", Pnpm, "is-odd@3.0.1"))
          .contains("\"is-odd@3.0.1>is-number\": \"7.0.0\"");
        assertThat(write(pkg, Npm, "is-even>is-odd@0.1.2"))
          .contains("\"is-even\": {\n      \"is-odd@0.1.2\": {\n        \"is-number\": \"7.0.0\"");
        assertThat(write(pkg, Pnpm, null)).isEqualTo(pkg);
    }

    @Test
    void aNestedOverrideKeepsThePinsItLandsOn() {
        String pkg = "{\n  \"overrides\": {\n    \"is-odd\": \"3.0.1\",\n    \"is-number\": {\n" +
                     "      \"kind-of\": \"1\"\n" +
                     "    }\n  }\n}\n";
        assertThat(write(pkg, Npm, "is-odd")).isEqualTo("{\n  \"overrides\": {\n    \"is-odd\": {\n" +
                "      \".\": \"3.0.1\",\n      \"is-number\": \"7.0.0\"\n    },\n    \"is-number\": {\n" +
                "      \"kind-of\": \"1\"\n    }\n  }\n}\n");
    }

    @Test
    void aNestedOverrideAlreadyPinnedUnderDotIsLeftAlone() {
        String pkg = "{\n  \"overrides\": {\n    \"is-odd\": {\n      \"is-number\": {\n        \".\": \"7.0.0\"\n" +
                     "      }\n    }\n  }\n}\n";
        assertThat(write(pkg, Npm, "is-odd")).isEqualTo(pkg);
    }

    @Test
    void aNestedOverrideIsWrittenPastAnEarlierWarning() {
        Json.Document doc = Markup.warn(manifest("{\n  \"name\": \"p\"\n}\n"), new RuntimeException("earlier"));
        Json.Document written = PackageJsonOverrides.applyOverride(doc, Npm, "is-number", "7.0.0",
                PackageJsonOverrides.parsePath("is-odd"));
        assertThat(written.printAll(new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.SANITIZED)))
          .isEqualTo("{\n  \"name\": \"p\",\n  \"overrides\": {\n    \"is-odd\": {\n      \"is-number\": \"7.0.0\"\n" +
                     "    }\n  }\n}\n");
    }

    @Test
    void anEmptyPnpmObjectGainsTheOverrides() {
        String pkg = "{\n  \"packageManager\": \"pnpm@10.34.6\",\n  \"pnpm\": {}\n}\n";
        assertThat(write(pkg, Pnpm, null)).isEqualTo("{\n  \"packageManager\": \"pnpm@10.34.6\",\n  \"pnpm\": {\n" +
                "    \"overrides\": {\n      \"is-number\": \"7.0.0\"\n    }\n  }\n}\n");
    }

    @Test
    void aGlobalOverrideIsAppendedInPlace() {
        String pkg = "{\n  \"name\":    \"p\",\n  \"overrides\": { \"already-here\": \"1.0.0\" }\n}\n";
        assertThat(write(pkg, Npm, null))
          .isEqualTo("{\n  \"name\":    \"p\",\n  \"overrides\": { \"already-here\": \"1.0.0\", " +
                     "\"is-number\": \"7.0.0\" }\n}\n");
        assertThat(write("{\n  \"overrides\": {\n    \"is-number\": \"7.0.0\"\n  }\n}\n", Npm, null))
          .isEqualTo("{\n  \"overrides\": {\n    \"is-number\": \"7.0.0\"\n  }\n}\n");
    }

    private static String write(String pkg, PackageManager pm, @Nullable String path) {
        return PackageJsonOverrides.applyOverride(manifest(pkg), pm, "is-number", "7.0.0",
                path == null ? null : PackageJsonOverrides.parsePath(path)).printAll();
    }

    private static Json.Document manifest(String json) {
        ExecutionContext ctx = new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });
        SourceFile parsed = JsonParser.builder().build().parse(ctx, json).findFirst()
                .orElseThrow(IllegalStateException::new);
        return (Json.Document) parsed;
    }

    private static String read(Path file) {
        try {
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
