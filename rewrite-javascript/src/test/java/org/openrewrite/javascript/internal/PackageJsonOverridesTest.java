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
import org.openrewrite.javascript.internal.lock.DependencyPathSegment;
import org.openrewrite.javascript.internal.lock.ManifestRules.OverridePlace;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.marker.NodeRuntime;
import org.openrewrite.json.JsonParser;
import org.openrewrite.json.tree.Json;
import org.openrewrite.marker.Markup;

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
        String written = PackageJsonOverrides.of(Pnpm, root, before,
                gt < 0 ? added.getKey() : added.getKey().substring(gt + 1), added.getValue(), path)
                .applyToWorkspaceManifest();
        assertThat(written).isEqualTo(read(dir.resolve("workspace")));
    }

    @Test
    void aWorkspaceManifestItDoesNotEditInPlaceIsRefused() {
        Json.Document root = manifest("{\"name\":\"p\"}");
        assertThatThrownBy(() -> workspace("overrides: {a: 1.0.0}\n", root, "7.0.0"))
          .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> workspace("---\npackages: []\n", root, "7.0.0"))
          .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> workspace("base: &b 1\nother: *b\n", root, "7.0.0"))
          .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> workspace("packages: [\n", root, "7.0.0"))
          .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Recorded with corepack 0.35.0: it refuses a range, a version that is no string and a devEngines name that is
     * none, which pnpm run directly reads past, so the host's not saying which runs declines those.
     */
    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
      "{} ; 11",
      "{\"packageManager\":\"pnpm@10.34.6\"} ; 10",
      "{\"packageManager\":\"pnpm@9.15.9+sha512.abc\"} ; 9",
      "{\"packageManager\":\"pnpm@^9.0.0\"} ; -1",
      "{\"packageManager\":\"yarn@4.10.3\"} ; 11",
      "{\"devEngines\":{\"packageManager\":{\"name\":\"pnpm\",\"version\":\"^10.0.0\"}}} ; -1",
      "{\"devEngines\":{\"packageManager\":{\"name\":\"pnpm\",\"version\":\">=9\"}}} ; -1",
      "{\"devEngines\":{\"packageManager\":{\"name\":\"pnpm\",\"version\":\"^11.0.0\"}}} ; -1",
      "{\"devEngines\":{\"packageManager\":{\"name\":\"pnpm\",\"version\":\"9 || 10\"}}} ; -1",
      "{\"devEngines\":{\"packageManager\":{\"name\":\"pnpm\",\"version\":\"^12.0.0\"}}} ; -1",
      "{\"devEngines\":{\"packageManager\":{\"name\":\"pnpm\",\"version\":\">=10.32.1 <=10.34.6\"}}} ; -1",
      "{\"devEngines\":{\"packageManager\":{\"name\":\"pnpm\",\"version\":\"11.0.6 - 11.2.2\"}}} ; -1",
      "{\"packageManager\":\"pnpm@10.0.0\"} ; 10",
      "{\"devEngines\":{\"packageManager\":{\"name\":\"pnpm\"}},\"packageManager\":\"pnpm@10.34.6\"} ; -1",
      "{\"devEngines\":{\"packageManager\":[{\"name\":\"npm\"},{\"name\":\"pnpm\",\"version\":\"9.15.9\"}]}} ; 9",
      "{\"devEngines\":{\"packageManager\":[{\"name\":\"pnpm\",\"version\":\"9.15.9\"},{\"name\":\"npm\"}]}} ; 11",
      "{\"devEngines\":{\"packageManager\":{\"name\":\"pnpm\",\"version\":\"10.34.6\",\"onFail\":\"ignore\"}}} ; -1",
      "{\"devEngines\":{\"packageManager\":{\"name\":\"pnpm\",\"version\":\"10.34.6\",\"onFail\":\"warn\"}}} ; -1",
      "{\"devEngines\":{\"packageManager\":{\"name\":\"pnpm\",\"version\":\"10.34.6\",\"onFail\":\"error\"}}} ; 10",
      "{\"devEngines\":{\"packageManager\":{\"name\":\"pnpm\",\"version\":10}}} ; -1",
      "{\"devEngines\":{\"packageManager\":{\"name\":false}},\"packageManager\":\"pnpm@10.34.6\"} ; -1",
      "{\"devEngines\":{\"packageManager\":{\"name\":0}},\"packageManager\":\"pnpm@9.15.9\"} ; -1",
      "{\"packageManager\":5} ; -1",
    })
    void thePnpmReleaseIsTheOnePnpmRuns(String root, int release) {
        assertThat(release(manifest(root), null)).isEqualTo(release);
    }

    /**
     * pnpm run directly keeps 11.2.2 under pmOnFail ignore, while corepack 0.35.0 starts the packageManager release
     * (recorded), so which release writes is the host's to say: declined.
     */
    @Test
    void theWorkspacesPmOnFailDecidesWhetherTheNamedReleaseRuns() {
        Json.Document root = manifest("{\"packageManager\":\"pnpm@10.34.6\"}");
        assertThat(release(root, "pmOnFail: ignore\n")).isEqualTo(-1);
        assertThat(release(root, "pmOnFail: download\n")).isEqualTo(10);
        assertThat(release(root, "pmOnFail: ''\n")).isEqualTo(10);
        assertThat(release(root, "pmOnFail: .nan\n")).isEqualTo(10);
        assertThat(release(root, "pmOnFail: no\n")).isEqualTo(-1);
        assertThat(release(root, "pmOnFail: off\n")).isEqualTo(-1);
    }

    @Test
    void anOverrideIsDecidedAgainFromLaterRevisionsOfTheRootAndTheWorkspaceManifest() {
        Json.Document pnpm9 = manifest("{\"packageManager\":\"pnpm@9.15.9\"}");
        Json.Document pnpm11 = manifest("{\"packageManager\":\"pnpm@11.2.2\"}");
        String workspace = "packages:\n  - apps/*\n";
        PackageJsonOverrides scanned = PackageJsonOverrides.of(Pnpm, pnpm9, workspace, "is-number", "7.0.0", null);
        assertThat(scanned.from(pnpm9, workspace)).isSameAs(scanned);
        assertThat(scanned.getPlace()).isEqualTo(OverridePlace.PNPM_FIELD);
        assertThat(scanned.from(pnpm9, "pmOnFail: ignore\n").getRelease()).isEqualTo(-1);

        PackageJsonOverrides changed = scanned.from(pnpm11, workspace);
        assertThat(changed.getPlace()).isEqualTo(OverridePlace.WORKSPACE_MANIFEST);
        assertThat(changed.writesAs(scanned)).isFalse();
        assertThat(changed.toString()).isEqualTo("pnpm-workspace.yaml for pnpm 11.2.2");
        assertThat(changed.writesAs(changed.from(pnpm11, workspace + "autoInstallPeers: false\n"))).isTrue();
        assertThat(changed.from(pnpm11, "packages: [apps/*]\n").declined()).isNull();
        assertThat(changed.from(pnpm11, "packages:\n  - &apps apps/*\n").declined()).contains("an anchor");
    }

    @Test
    void eachPackageManagerReadsItsOwnPlace() {
        Json.Document plain = manifest("{\"name\":\"p\"}");
        assertThat(place(Npm, plain, null)).isEqualTo(OverridePlace.OVERRIDES);
        assertThat(place(Bun, plain, null)).isEqualTo(OverridePlace.OVERRIDES);
        assertThat(place(Bun, manifest("{\"resolutions\":{}}"), null))
          .isEqualTo(OverridePlace.RESOLUTIONS);
        assertThat(place(Bun, manifest("{\"overrides\":{},\"resolutions\":{}}"), null))
          .isEqualTo(OverridePlace.OVERRIDES);
        assertThat(place(YarnClassic, plain, null)).isEqualTo(OverridePlace.RESOLUTIONS);
        assertThat(place(YarnBerry, plain, null)).isEqualTo(OverridePlace.RESOLUTIONS);
        assertThat(place(Pnpm, plain, null)).isEqualTo(OverridePlace.WORKSPACE_MANIFEST);
        assertThat(place(Pnpm, manifest("{\"packageManager\":\"pnpm@9.15.9\"}"), null))
          .isEqualTo(OverridePlace.PNPM_FIELD);
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
      " ; ; WORKSPACE_MANIFEST",
      "\"pnpm\":{} ; ; PNPM_FIELD",
      "\"pnpm\":null ; ; WORKSPACE_MANIFEST",
      "\"pnpm\":{} ; overrides: {left-pad: 1.3.0} ; WORKSPACE_MANIFEST",
      "\"pnpm\":{} ; overrides: {} ; PNPM_FIELD",
      "\"resolutions\":{\"left-pad\":\"1.3.0\"} ; ; PNPM_FIELD",
      "\"resolutions\":{} ; ; WORKSPACE_MANIFEST",
      "\"pnpm\":{\"overrides\":{\"left-pad\":\"1.3.0\"}} ; overrides: {wrappy: 1.0.2} ; PNPM_FIELD",
      "\"pnpm\":null,\"resolutions\":{\"left-pad\":\"1.3.0\"} ; ; RESOLUTIONS",
    })
    void pnpm10ReadsPackageJsonsOverridesInPlaceOfTheWorkspaces(@Nullable String fields, @Nullable String workspace,
                                                                 OverridePlace place) {
        Json.Document root = manifest("{\"packageManager\":\"pnpm@10.34.6\"" + (fields == null ? "" : "," + fields) +
                                      "}");
        assertThat(place(Pnpm, root, workspace)).isEqualTo(place);
    }

    @Test
    void anOverrideThePackageManagerCannotReadIsDeclined() {
        Json.Document plain = manifest("{\"name\":\"p\"}");
        List<DependencyPathSegment> oneParent = PackageJsonOverrides.parsePath("is-odd");
        List<DependencyPathSegment> twoParents = PackageJsonOverrides.parsePath("is-even>is-odd");
        assertThat(declined(Npm, plain, twoParents)).isNull();
        assertThat(declined(Bun, plain, oneParent)).contains("nested overrides");
        assertThat(declined(Pnpm, plain, oneParent)).isNull();
        assertThat(declined(Pnpm, plain, twoParents)).contains("ERR_PNPM_INVALID_SELECTOR");
        assertThat(declined(Pnpm, manifest("{\"packageManager\":\"pnpm@8.15.9\"}"), null))
          .contains("pnpm 8.15.9 is not a release whose writes are recorded");
        // pnpm 10.0.0 rejects a pnpm-workspace.yaml without packages and reads no overrides from it
        assertThat(declined(Pnpm, manifest("{\"packageManager\":\"pnpm@10.0.0\"}"), null))
          .contains("pnpm 10.0.0 is not a release whose writes are recorded");
        assertThat(declined(YarnBerry, plain, oneParent)).isNull();
        assertThat(declined(YarnBerry, plain, twoParents)).contains("YN0057");
        assertThat(declined(YarnBerry, plain, PackageJsonOverrides.parsePath("is-odd@3.0.1"))).isNull();
        assertThat(declined(YarnBerry, plain, PackageJsonOverrides.parsePath("is-odd@npm:3.0.1"))).isNull();
        assertThat(declined(YarnBerry, plain, PackageJsonOverrides.parsePath("is-odd@^3.0.0")))
          .contains("exact version only, not is-odd@^3.0.0");
        assertThat(declined(YarnClassic, plain, twoParents)).isNull();
        assertThat(declined(YarnClassic, plain, PackageJsonOverrides.parsePath("is-odd@3.0.1")))
          .contains("is-odd@3.0.1");
        assertThat(declined(Npm, manifest("{\"overrides\":\"x\"}"), null)).contains("not an object");
    }

    // the Bun release the root's NodeRuntime marker names reads the override, as it installs the lock
    @Test
    void bunReadsAnOverrideAsTheReleaseTheMarkerNames() {
        Json.Document root = manifest("{\"name\":\"p\"}");
        List<DependencyPathSegment> oneParent = PackageJsonOverrides.parsePath("is-odd");
        List<DependencyPathSegment> twoParents = PackageJsonOverrides.parsePath("is-even>is-odd");
        assertThat(declined(Bun, ranBy(root, Bun, "1.3.10"), oneParent))
          .isEqualTo("bun does not support nested overrides");
        // from 1.4.0 bun reads an override one parent deep, and warns of a deeper one
        assertThat(declined(Bun, ranBy(root, Bun, "1.4.0"), oneParent)).isNull();
        assertThat(declined(Bun, ranBy(root, Bun, "1.4.0"), twoParents))
          .isEqualTo("Bun currently only supports one level of nested \"overrides\"");
        assertThat(declined(Bun, ranBy(root, Bun, "1.4.0"), null)).isNull();
        // 1.2.x reads overrides flat, as 1.3.10 does
        assertThat(declined(Bun, ranBy(root, Bun, "1.2.0"), oneParent))
          .isEqualTo("bun does not support nested overrides");
        // a release the engine does not reproduce
        assertThat(declined(Bun, ranBy(root, Bun, "1.1.45"), oneParent)).contains("cannot know what bun 1.1.45 writes");
        // a marker of another package manager names no Bun release: 1.3.10 installs
        assertThat(declined(Bun, ranBy(root, Npm, "1.4.0"), oneParent))
          .isEqualTo("bun does not support nested overrides");
    }

    private static Json.Document ranBy(Json.Document root, PackageManager pm, String version) {
        return root.withMarkers(root.getMarkers().add(new NodeRuntime(org.openrewrite.Tree.randomId(), "v24.19.0",
          "darwin", "arm64", null, pm, version)));
    }

    @Test
    void aPnpmOverrideItAlreadyReadsFromResolutionsIsLeftAlone() {
        String pkg = "{\n  \"packageManager\": \"pnpm@10.34.6\",\n  \"resolutions\": {\n" +
                     "    \"is-number\": \"7.0.0\"\n  }\n}\n";
        assertThat(write(pkg, Pnpm, null)).isEqualTo(pkg);
    }

    @Test
    void anOverrideThatWouldNeedAnEscapeIsNotWritten() {
        Json.Document root = manifest("{\"name\":\"p\"}");
        assertThatThrownBy(() -> workspace(null, root, "7.0.0\u0007")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> workspace(null, root, "7.0.0\u2028")).isInstanceOf(IllegalArgumentException.class);
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
        Json.Document written = PackageJsonOverrides.of(Npm, doc, null, "is-number", "7.0.0",
                PackageJsonOverrides.parsePath("is-odd")).apply(doc);
        assertThat(written.printAll(new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.SANITIZED)))
          .isEqualTo("{\n  \"name\": \"p\",\n  \"overrides\": {\n    \"is-odd\": {\n      \"is-number\": \"7.0.0\"\n" +
                     "    }\n  }\n}\n");
    }

    @Test
    void aNullPnpmFieldBesideResolutionsGainsTheEntryInResolutions() {
        String pkg = "{\n  \"packageManager\": \"pnpm@9.15.9\",\n  \"pnpm\": null,\n  \"resolutions\": {\n" +
                     "    \"left-pad\": \"1.3.0\"\n  }\n}\n";
        assertThat(write(pkg, Pnpm, "is-odd")).isEqualTo("{\n  \"packageManager\": \"pnpm@9.15.9\",\n" +
                "  \"pnpm\": null,\n  \"resolutions\": {\n    \"left-pad\": \"1.3.0\",\n" +
                "    \"is-odd>is-number\": \"7.0.0\"\n  }\n}\n");
        assertThat(declined(Pnpm, manifest("{\"packageManager\":\"pnpm@9.15.9\",\"pnpm\":null}"), null))
          .contains("pnpm is not an object");
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

    @Test
    void aNestedOverrideOnALeafPinnedOtherwiseUnderDotMovesTheDot() {
        String pkg = "{\n  \"overrides\": {\n    \"is-odd\": {\n      \"is-number\": {\n" +
                     "        \".\": \"6.0.0\",\n        \"kind-of\": \"1\"\n      }\n    }\n  }\n}\n";
        assertThat(write(pkg, Npm, "is-odd")).isEqualTo(pkg.replace("6.0.0", "7.0.0"));
    }

    // an object's "." is the package's own override and the keys beside it stay (npm 11.17.0 keeps an equal pin)
    @Test
    void aGlobalOverrideOfAPackageOverriddenByAnObjectSetsItsDot() {
        String pinned = "{\n  \"overrides\": {\n    \"is-number\": {\n      \".\": \"7.0.0\",\n" +
                        "      \"kind-of\": \"4.0.0\"\n    }\n  }\n}\n";
        assertThat(write(pinned, Npm, null)).isEqualTo(pinned);
        assertThat(write(pinned.replace("7.0.0", "6.0.0"), Npm, null)).isEqualTo(pinned);
        assertThat(write("{\n  \"overrides\": {\n    \"is-number\": {\n      \"kind-of\": \"4.0.0\"\n" +
                         "    }\n  }\n}\n", Npm, null)).isEqualTo("{\n  \"overrides\": {\n" +
                "    \"is-number\": {\n      \"kind-of\": \"4.0.0\",\n      \".\": \"7.0.0\"\n    }\n  }\n}\n");
    }

    // recorded with yarn 1.22.22: a bare name is **/name, and the first resolution whose glob matches applies
    @Test
    void yarnClassicEditsTheResolutionItReadsAsTheSameGlob() {
        String pkg = "{\n  \"resolutions\": {\n    \"**/is-number\": \"6.0.0\"\n  }\n}\n";
        assertThat(write(pkg, YarnClassic, null)).isEqualTo(pkg.replace("6.0.0", "7.0.0"));
        String bare = "{\n  \"resolutions\": {\n    \"is-number\": \"6.0.0\"\n  }\n}\n";
        assertThat(write(bare, YarnClassic, null)).isEqualTo(bare.replace("6.0.0", "7.0.0"));
        String scoped = "{\n  \"resolutions\": {\n    \"**/@s/p\": \"6.0.0\"\n  }\n}\n";
        assertThat(write(scoped, YarnClassic, "@s/p", null)).isEqualTo(scoped.replace("6.0.0", "7.0.0"));
        String path = "{\n  \"resolutions\": {\n    \"is-odd/is-number\": \"6.0.0\"\n  }\n}\n";
        assertThat(write(path, YarnClassic, null)).contains("\"is-odd/is-number\": \"6.0.0\"",
          "\"is-number\": \"7.0.0\"");
    }

    // recorded with Yarn 4.10.3 and pnpm 11.2.2: an override scoped to a parent wins over a global one there
    @Test
    void anOverrideScopedToAParentOutranksAGlobalOne() {
        Json.Document berry = manifest("{\"resolutions\":{\"is-odd/is-number\":\"6.0.0\",\"@s/is-number\":\"1\"," +
                                       "\"@s/a@1.0.0/is-number\":\"2\"}}");
        assertThat(PackageJsonOverrides.of(YarnBerry, berry, null, "is-number", "7.0.0", null).outranked())
          .isEqualTo("Yarn applies is-odd/is-number, @s/a@1.0.0/is-number before a global override of is-number");
        assertThat(PackageJsonOverrides.of(YarnBerry, berry, null, "is-number", "7.0.0",
          PackageJsonOverrides.parsePath("is-odd")).outranked()).isNull();
        Json.Document root = manifest("{\"name\":\"p\"}");
        assertThat(PackageJsonOverrides.of(Pnpm, root, "overrides:\n  is-odd>is-number: 6.0.0\n  left-pad: 1.3.0\n",
          "is-number", "7.0.0", null).outranked())
          .isEqualTo("pnpm applies is-odd>is-number before a global override of is-number");
        Json.Document pnpm9 = manifest("{\"packageManager\":\"pnpm@9.15.9\",\"pnpm\":{\"overrides\":" +
                                       "{\"is-odd@3>is-number@<7\":\"6.0.0\"}}}");
        assertThat(PackageJsonOverrides.of(Pnpm, pnpm9, null, "is-number", "7.0.0", null).outranked())
          .contains("is-odd@3>is-number@<7");
        assertThat(PackageJsonOverrides.of(Npm, root, null, "is-number", "7.0.0", null).outranked()).isNull();
    }

    // recorded with yarn 1.22.22: a resolution find tries before the one written wins where its glob matches
    @Test
    void aYarnClassicResolutionTriedBeforeTheGlobalOneOutranksIt() {
        Json.Document before = manifest("{\"resolutions\":{\"is-odd/is-number\":\"6.0.0\",\"left-pad\":\"1.3.0\"," +
                                        "\"**/is-number\":\"6.0.0\",\"a/is-number\":\"5.0.0\",\"b/is-number\":\"%\"}}");
        assertThat(PackageJsonOverrides.of(YarnClassic, before, null, "is-number", "7.0.0", null).outranked())
          .isEqualTo("yarn applies is-odd/is-number before a global override of is-number");
        Json.Document appended = manifest("{\"resolutions\":{\"is-odd/is-number\":\"6.0.0\"," +
                                          "\"a/is-number\":\"5.0.0\",\"b/is-number\":\"%\"}}");
        assertThat(PackageJsonOverrides.of(YarnClassic, appended, null, "is-number", "7.0.0", null).outranked())
          .isEqualTo("yarn applies is-odd/is-number, a/is-number before a global override of is-number");
        Json.Document after = manifest("{\"resolutions\":{\"**/is-number\":\"6.0.0\"," +
                                       "\"is-odd/is-number\":\"6.0.0\"}}");
        assertThat(PackageJsonOverrides.of(YarnClassic, after, null, "is-number", "7.0.0", null).outranked())
          .isNull();
    }

    @Test
    void aPnpmFieldOrPnpmOverridesThatIsNoObjectIsDeclined() {
        String pnpm10 = "\"packageManager\":\"pnpm@10.34.6\"";
        assertThat(declined(Pnpm, manifest("{" + pnpm10 + ",\"pnpm\":\"hoist\"}"), null))
          .isEqualTo("the root's pnpm is not an object");
        assertThat(declined(Pnpm, manifest("{" + pnpm10 + ",\"pnpm\":{\"overrides\":\"none\"}}"), null))
          .isEqualTo("the root's pnpm.overrides is not an object");
        assertThat(declined(Npm, manifest("{\"overrides\":[]}"), null))
          .isEqualTo("the root's overrides is not an object");
        assertThat(declined(YarnClassic, manifest("{\"resolutions\":1}"), null))
          .isEqualTo("the root's resolutions is not an object");
    }

    private static String write(String pkg, PackageManager pm, @Nullable String path) {
        return write(pkg, pm, "is-number", path);
    }

    private static String write(String pkg, PackageManager pm, String packageName, @Nullable String path) {
        Json.Document root = manifest(pkg);
        return PackageJsonOverrides.of(pm, root, null, packageName, "7.0.0",
                path == null ? null : PackageJsonOverrides.parsePath(path)).apply(root).printAll();
    }

    private static OverridePlace place(PackageManager pm, Json.Document root,
                                                    @Nullable String workspace) {
        return PackageJsonOverrides.of(pm, root, workspace == null ? null : workspace + "\n", "is-number", "7.0.0",
                null).getPlace();
    }

    private static @Nullable String declined(PackageManager pm, Json.Document root,
                                             @Nullable List<DependencyPathSegment> path) {
        return PackageJsonOverrides.of(pm, root, null, "is-number", "7.0.0", path).declined();
    }

    private static int release(Json.Document root, @Nullable String workspace) {
        return PackageJsonOverrides.of(Pnpm, root, workspace, "is-number", "7.0.0", null).getRelease();
    }

    private static String workspace(@Nullable String workspace, Json.Document root, String version) {
        return PackageJsonOverrides.of(Pnpm, root, workspace, "is-number", version, null)
                .applyToWorkspaceManifest();
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
