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
package org.openrewrite.python.internal.uvlock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.python.PythonExecutionContextView;
import org.openrewrite.python.PythonPackageIndex;
import org.openrewrite.python.internal.LockFileRegeneration.Reason;
import org.openrewrite.python.internal.LockFileRegeneration.Result;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.python.internal.uvlock.UvLockFixtures.resource;

/**
 * Golden scenarios replay uv 0.10.11 runs against pypi.org (see the fixture README):
 * the engine's output for the same edit must be byte-identical to what real uv wrote.
 * Recorded index/metadata responses under {@code /lock/uv/http/} keep the tests offline.
 */
class UvLockEngineTest {

    RoutedHttp http;
    ExecutionContext ctx;

    @BeforeEach
    void setUp() {
        http = new RoutedHttp();
        ctx = new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });
        HttpSenderExecutionContextView.view(ctx).setHttpSender(http);
        PythonExecutionContextView.view(ctx).setPackageIndexes(List.of(
          new PythonPackageIndex("pypi", "https://pypi.org/simple", true, null, null, false)));
    }

    /**
     * Serves recorded responses by exact URL so listings can carry the real
     * files.pythonhosted.org URLs the byte-exact lock output requires.
     */
    static final class RoutedHttp implements HttpSender {
        final Map<String, byte[]> routes = new LinkedHashMap<>();
        final List<String> requests = new ArrayList<>();

        void route(String url, String body) {
            route(url, body.getBytes(StandardCharsets.UTF_8));
        }

        void route(String url, byte[] body) {
            routes.put(url, body);
        }

        @Override
        public Response send(Request request) {
            String url = request.getUrl().toString();
            requests.add(url);
            byte[] body = routes.get(url);
            if (body == null) {
                return new Response(404, new ByteArrayInputStream(new byte[0]), () -> {
                });
            }
            return new Response(200, new ByteArrayInputStream(body), () -> {
            });
        }
    }

    private void stubPypiSix() {
        http.route("https://pypi.org/simple/six/", resource("http/six-listing-json"));
        http.route("https://files.pythonhosted.org/packages/b7/ce/149a00dd41f10bc29e5921b496af8b574d8413afcd5e30dfa0ed46c2cc5e/six-1.17.0-py2.py3-none-any.whl.metadata",
          resource("http/six-1.17.0-py2.py3-none-any.whl.metadata"));
        http.route("https://files.pythonhosted.org/packages/d9/5a/e7c31adbe875f2abbb91bd84cf2dc52d792b5a01506781dbcf25c91daf11/six-1.16.0-py2.py3-none-any.whl.metadata",
          resource("http/six-1.16.0-py2.py3-none-any.whl.metadata"));
    }

    // ---- golden scenario (i): pin bump, engine output byte-identical to real uv ----

    @Test
    void pinBumpMatchesRealUvByteForByte() {
        stubPypiSix();
        Result result = UvLockEngine.regenerate(
          resource("i-minimal-update/pyproject.toml"),
          resource("i-minimal-update/uv.lock.v1"),
          ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent()).isEqualTo(resource("i-minimal-update/uv.lock.v2"));
    }

    @Test
    void noOpRelockIsByteIdenticalWithoutNetwork() {
        Result result = UvLockEngine.regenerate(
          resource("i-minimal-update/pyproject.toml"),
          resource("i-minimal-update/uv.lock.v2"),
          ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent()).isEqualTo(resource("i-minimal-update/uv.lock.v2"));
        assertThat(http.requests).isEmpty();
    }

    // ---- golden scenario (ii): removal drops the package and its orphaned transitives ----

    @Test
    void removalMatchesRealUvByteForByte() {
        Result result = UvLockEngine.regenerate(
          resource("r-removal/pyproject.toml"),
          resource("r-removal/uv.lock.before"),
          ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent()).isEqualTo(resource("r-removal/uv.lock.after"));
        assertThat(http.requests).isEmpty();
    }

    // ---- golden scenario: removing the last dependency drops [package.metadata] entirely ----

    @Test
    void removingLastDependencyOmitsMetadataTableLikeUv() {
        Result result = UvLockEngine.regenerate(
          resource("s-remove-last-dep/pyproject.toml"),
          resource("s-remove-last-dep/uv.lock.before"),
          ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent()).isEqualTo(resource("s-remove-last-dep/uv.lock.after"));
        assertThat(http.requests).isEmpty();
    }

    // ---- golden scenario (iii): requires-python bump filters wheels and prunes edges ----

    @Test
    void requiresPythonBumpMatchesRealUvByteForByte() {
        http.route("https://pypi.org/simple/attrs/", resource("http/attrs-listing-json"));
        Result result = UvLockEngine.regenerate(
          resource("h-requires-python/pyproject.toml"),
          resource("h-requires-python/uv.lock.before"),
          ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent()).isEqualTo(resource("h-requires-python/uv.lock.after"));
        assertThat(http.requests).containsExactly("https://pypi.org/simple/attrs/");
    }

    /**
     * A bump lifting the floor past a fork boundary collapses the lock to one environment: the
     * eliminated branch (soupsieve 2.7 for python &lt; 3.9) drops, the survivor (2.8.4) loses its
     * resolution-markers, the root's two fork-disambiguated edges revert to one plain edge, and the
     * top-level resolution-markers are removed -- byte-identical to real uv.
     */
    @Test
    void requiresPythonBumpCollapsingForkMatchesRealUvByteForByte() {
        http.route("https://pypi.org/simple/soupsieve/", resource("http/soupsieve-listing-json"));
        Result result = UvLockEngine.regenerate(
          resource("h4-fork-collapse/pyproject.toml"),
          resource("h4-fork-collapse/uv.lock.before"),
          ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent()).isEqualTo(resource("h4-fork-collapse/uv.lock.after"));
        assertThat(http.requests).containsExactly("https://pypi.org/simple/soupsieve/");
    }

    /**
     * A bump that lifts the floor but still leaves more than one fork branch keeps the lock forked
     * (importlib-resources stays split at 3.10 after &gt;=3.8 -&gt; &gt;=3.9); collapsing it needs
     * marker-space resolution, so the engine fails loud before any network fetch.
     */
    @Test
    void requiresPythonBumpLeavingForkRequiresResolution() {
        Result result = UvLockEngine.regenerate(
          resource("h5-fork-partial/pyproject.toml"),
          resource("h5-fork-partial/uv.lock.before"),
          ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(http.requests).isEmpty();
    }

    @Test
    void constraintRelaxationKeepsLockedVersion() {
        // i2 scenario: ==1.16.0 -> >=1.16 keeps 1.16.0, only the requires-dist line changes
        Result result = UvLockEngine.regenerate(
          resource("i2-upgrade-package/pyproject.toml"),
          resource("i2-upgrade-package/uv.lock.pinned"),
          ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent()).isEqualTo(resource("i2-upgrade-package/uv.lock.relaxed-plain"));
        assertThat(http.requests).isEmpty();
    }

    // ---- no-op rebuilds across the emission corpus prove the metadata mirror ----

    @ParameterizedTest
    @CsvSource({
      "d-extras/pyproject.toml, d-extras/uv.lock",
      "e-markers-forks/pyproject.toml, e-markers-forks/uv.lock",
      "e2-true-fork/pyproject.toml, e2-true-fork/uv.lock",
      "f-dep-groups/pyproject.toml, f-dep-groups/uv.lock",
      "g-optional-deps/pyproject.toml, g-optional-deps/uv.lock",
      "g2-extras-order/pyproject.toml, g2-extras-order/uv.lock",
      "g3-multi-extras/pyproject.toml, g3-multi-extras/uv.lock",
      "h3-requires-python-order/pyproject.toml, h3-requires-python-order/uv.lock",
      "k-multi-index/pyproject.toml, k-multi-index/uv.lock",
      "m3-lexical/pyproject.toml, m3-lexical/uv.lock",
      "n-normalization/pyproject.toml, n-normalization/uv.lock",
      "p-editable-root/pyproject.toml, p-editable-root/uv.lock",
      "o-old-uv/proj-0.7.0/pyproject.toml, o-old-uv/proj-0.7.0/uv.lock.as-0.7.0",
      "s-remove-last-dep/pyproject.toml, s-remove-last-dep/uv.lock.after",
      // directory-sourced deps and the top-level conflicts/supported-markers/required-markers
      // header keys all pass through an unrelated rebuild verbatim
      "v-directory/pyproject.toml, v-directory/uv.lock",
      "w-conflicts/pyproject.toml, w-conflicts/uv.lock",
      "w2-conflicts-groups/pyproject.toml, w2-conflicts-groups/uv.lock",
      "x-supported-required-markers/pyproject.toml, x-supported-required-markers/uv.lock",
    })
    void noOpRebuildIsByteIdentical(String pyprojectPath, String lockPath) {
        ExecutionContext plain = new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });
        HttpSenderExecutionContextView.view(plain).setHttpSender(http);
        // no index override so [tool.uv.index] declarations in the pyproject are honored
        Result result = UvLockEngine.regenerate(resource(pyprojectPath), resource(lockPath), plain);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent()).isEqualTo(resource(lockPath));
        assertThat(http.requests).isEmpty();
    }

    // ---- old-revision locks keep their emission style on edit (BEHAVIOR.md §5) ----

    @Test
    void noOpRebuildKeepsRevisionlessStyle() {
        // proj-0.5.0's checked-in pyproject is the edited state; this is the pre-edit one
        String pyproject = """
          [project]
          name = "fixture-o"
          version = "0.1.0"
          requires-python = ">=3.12"
          dependencies = [
              "six>=1.16",
              "iniconfig>=2.0",
          ]
          """;
        Result result = UvLockEngine.regenerate(pyproject, resource("o-old-uv/proj-0.5.0/uv.lock.as-0.5.0"), ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent()).isEqualTo(resource("o-old-uv/proj-0.5.0/uv.lock.as-0.5.0"));
        assertThat(http.requests).isEmpty();
    }

    @Test
    void editOfRevisionlessLockDoesNotIntroduceUploadTime() {
        stubPypiSix();
        // pin down to 1.16.0 on the uv 0.5.0-format lock: unlike current uv, which would
        // rewrite the whole file at revision 3, the surgical edit keeps the old style.
        // The expectation is engine-defined by design (real uv cannot produce it); the
        // checked-in file was reviewed against the listing fixture and the 0.5.0 style.
        Result result = UvLockEngine.regenerate(
          resource("o-old-uv/proj-0.5.0/pyproject.toml"),
          resource("o-old-uv/proj-0.5.0/uv.lock.as-0.5.0"),
          ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent()).isEqualTo(resource("o-old-uv/proj-0.5.0/uv.lock.engine-edited"));
    }

    @Test
    void configuredUvReleaseRewritesAChangedPreRevision3LockInItsOwnShape() {
        // uv stamps the whole file at its revision and gives every artifact, the untouched iniconfig too, its upload time
        stubPypiSix();
        http.route("https://pypi.org/simple/iniconfig/", resource("off1-add-leaf/http/iniconfig-listing-json"));
        PythonExecutionContextView.view(ctx).setUvVersion("0.10.11");
        Result result = UvLockEngine.regenerate(
          resource("o-old-uv/proj-0.5.0/pyproject.toml"),
          resource("o-old-uv/proj-0.5.0/uv.lock.as-0.5.0"),
          ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent()).isEqualTo(resource("o-old-uv/proj-0.5.0/uv.lock.after-change-current-uv"));

        Result unchanged = UvLockEngine.regenerate(
          resource("o-old-uv/proj-0.5.0/pyproject.toml").replace("six==1.16.0", "six>=1.16"),
          resource("o-old-uv/proj-0.5.0/uv.lock.as-0.5.0"),
          ctx);
        assertThat(unchanged.getErrorMessage()).isNull();
        assertThat(unchanged.getLockFileContent()).isEqualTo(resource("o-old-uv/proj-0.5.0/uv.lock.as-0.5.0"));
    }

    @Test
    void lockSatisfyingTheManifestByMeaningIsKeptVerbatimUntilAnythingElseChanges() {
        // uv compares provides-extras as a set and specifiers by value, so neither reordering nor a trailing zero rewrites
        String pyproject = resource("g-optional-deps/pyproject.toml")
          .replace("\"six>=1.16\"", "\"six>=1.16.0\"")
          .replace("cli = [\n    \"click>=8.1\",\n]\nyaml = [\n    \"pyyaml>=6.0\",\n]", "yaml = [\n    \"pyyaml>=6.0\",\n]\ncli = [\n    \"click>=8.1\",\n]");
        assertThat(pyproject).contains("yaml = [\n    \"pyyaml>=6.0\",\n]\ncli");
        Result result = UvLockEngine.regenerate(pyproject, resource("g-optional-deps/uv.lock"), ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent()).isEqualTo(resource("g-optional-deps/uv.lock"));

        // once anything else in the lock changes, uv writes the metadata fresh from the manifest
        result = UvLockEngine.regenerate(pyproject + "\n[tool.uv]\nconstraint-dependencies = [\"six>=1\"]\n",
          resource("g-optional-deps/uv.lock"), ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent())
          .contains("[manifest]\nconstraints = [{ name = \"six\", specifier = \">=1\" }]")
          .contains("{ name = \"six\", specifier = \">=1.16.0\" }")
          .contains("provides-extras = [\"yaml\", \"cli\"]");

        // a lock written before provides-extras existed (version 1 without a revision) is never judged on it
        String revisionless = resource("g-optional-deps/uv.lock")
          .replace("revision = 3\n", "")
          .replace("provides-extras = [\"cli\", \"yaml\"]\n", "");
        result = UvLockEngine.regenerate(resource("g-optional-deps/pyproject.toml"), revisionless, ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent()).isEqualTo(revisionless);
        assertThat(http.requests).isEmpty();
    }

    @Test
    void declaredGroupsAreRecordedAsUvNormalizesThem() {
        String pyproject = resource("f-dep-groups/pyproject.toml").replace("lint = [", "Lint = [");
        Result result = UvLockEngine.regenerate(pyproject, resource("f-dep-groups/uv.lock"), ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent()).isEqualTo(resource("f-dep-groups/uv.lock"));

        result = UvLockEngine.regenerate(pyproject + "test_integration = []\n", resource("f-dep-groups/uv.lock"), ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent()).contains("lint = [{ name = \"pyflakes\"").contains("\ntest-integration = []\n");

        // an empty legacy dev-dependencies list still declares the dev group
        String withEmptyDev = resource("g-optional-deps/uv.lock").replace("provides-extras = [\"cli\", \"yaml\"]\n",
          "provides-extras = [\"cli\", \"yaml\"]\n\n[package.metadata.requires-dev]\ndev = []\n");
        result = UvLockEngine.regenerate(resource("g-optional-deps/pyproject.toml") + "\n[tool.uv]\ndev-dependencies = []\n",
          withEmptyDev, ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent()).isEqualTo(withEmptyDev);
        assertThat(http.requests).isEmpty();
    }

    @Test
    void includedGroupsAreRecordedFlattenedIntoTheIncludingGroup() {
        String pyproject = resource("f-dep-groups/pyproject.toml")
          .replace("dev = [\n    \"iniconfig>=2.0\",\n]", "dev = [\n    { include-group = \"Lint\" },\n    \"iniconfig>=2.0\",\n]");
        String lock = resource("f-dep-groups/uv.lock").replace(
          "dev = [{ name = \"iniconfig\", specifier = \">=2.0\" }]",
          "dev = [\n    { name = \"iniconfig\", specifier = \">=2.0\" },\n    { name = \"pyflakes\", specifier = \">=3.0\" },\n]");
        assertThat(pyproject).contains("include-group");
        assertThat(lock).contains("{ name = \"pyflakes\", specifier = \">=3.0\" },\n]");
        Result result = UvLockEngine.regenerate(pyproject, lock, ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent()).isEqualTo(lock);
        assertThat(http.requests).isEmpty();
    }

    @Test
    void tablesWrittenAsDottedKeysOrInlineTablesReadAsTheirTable() {
        String table = "[project.optional-dependencies]\ncli = [\n    \"click>=8.1\",\n]\nyaml = [\n    \"pyyaml>=6.0\",\n]";
        String base = resource("g-optional-deps/pyproject.toml");
        assertThat(base).contains(table);
        String dotted = base.replace(table, "").replace("dependencies = [",
          "optional-dependencies.cli = [\"click>=8.1\"]\n\"optional-dependencies\".yaml = [\"pyyaml>=6.0\"]\ndependencies = [");
        String inline = base.replace(table, "").replace("dependencies = [",
          "optional-dependencies = { cli = [\"click>=8.1\"], yaml = [\"pyyaml>=6.0\"] }\ndependencies = [");
        for (String pyproject : new String[]{dotted, inline}) {
            Result result = UvLockEngine.regenerate(pyproject, resource("g-optional-deps/uv.lock"), ctx);
            assertThat(result.getErrorMessage()).as(pyproject).isNull();
            assertThat(result.getLockFileContent()).as(pyproject).isEqualTo(resource("g-optional-deps/uv.lock"));
        }
    }

    // a lock uv 0.13.4 rewrites reads every locked package's files anew
    private void routeDepGroupsListings() {
        http.route("https://pypi.org/simple/six/", resource("http/six-listing-json"));
        http.route("https://pypi.org/simple/iniconfig/", resource("off1-add-leaf/http/iniconfig-listing-json"));
        http.route("https://pypi.org/simple/pyflakes/", resource("off3-cascade-flake8/http/pyflakes-listing-json"));
    }

    @Test
    void defaultGroupsAreRecordedOnTheProjectFromRevisionFive() {
        PythonExecutionContextView.view(ctx).setUvVersion("0.13.4");
        routeDepGroupsListings();
        String pyproject = resource("f-dep-groups/pyproject.toml") + "\n[tool.uv]\ndefault-groups = [\"Lint\", \"dev\"]\n";
        Result result = UvLockEngine.regenerate(pyproject, resource("f-dep-groups/uv.lock"), ctx);
        assertThat(result.getLockFileContent()).isEqualTo(resource("f-dep-groups/uv.lock"));

        String removal = pyproject.replace("lint = [\n    \"pyflakes>=3.0\",\n]\n", "");
        result = UvLockEngine.regenerate(removal, resource("f-dep-groups/uv.lock"), ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent()).contains("source = { virtual = \".\" }\ndefault-groups = [\"dev\", \"lint\"]\n");

        // the implicit default is not recorded
        result = UvLockEngine.regenerate(removal.replace("[\"Lint\", \"dev\"]", "[\"dev\"]"), resource("f-dep-groups/uv.lock"), ctx);
        assertThat(result.getLockFileContent()).doesNotContain("default-groups");
    }

    @Test
    void aChangedLockListsItsPackagesInUvsOrder() {
        // an older release could leave a package out of order, and uv 0.13.4 sorts them by name and version
        PythonExecutionContextView.view(ctx).setUvVersion("0.13.4");
        routeDepGroupsListings();
        String lock = resource("f-dep-groups/uv.lock");
        int start = lock.indexOf("[[package]]\nname = \"fixture-f\"");
        int end = lock.indexOf("[[package]]", start + 1);
        String outOfOrder = lock.substring(0, start) + lock.substring(end) + "\n" + lock.substring(start, end).trim() + "\n";
        String removal = resource("f-dep-groups/pyproject.toml").replace("lint = [\n    \"pyflakes>=3.0\",\n]\n", "");
        Result result = UvLockEngine.regenerate(removal, outOfOrder, ctx);
        assertThat(result.getErrorMessage()).isNull();
        String written = result.getLockFileContent();
        assertThat(written.indexOf("name = \"fixture-f\"")).isLessThan(written.indexOf("name = \"iniconfig\""));
    }

    @Test
    void gitDeclarationsRecordAsUvWritesThem() {
        // forms recorded by uv 0.13.4 in the corpus locks
        assertThat(UvLockMetadataBuilder.recordedGit("git+https://github.com/hauntsaninja/mypy_primer@e1f6fe93da5de0cbaf0ab00341d05968a0331fae"))
          .isEqualTo("https://github.com/hauntsaninja/mypy_primer?rev=e1f6fe93da5de0cbaf0ab00341d05968a0331fae");
        assertThat(UvLockMetadataBuilder.recordedGit("git+https://github.com/Unity-Technologies/ml-agents.git#subdirectory=ml-agents-envs"))
          .isEqualTo("https://github.com/Unity-Technologies/ml-agents.git?subdirectory=ml-agents-envs");
        assertThat(UvLockMetadataBuilder.recordedGit("git+https://github.com/huggingface/OpenEnv.git"))
          .isEqualTo("https://github.com/huggingface/OpenEnv.git");

        // with both a revision and a subdirectory the order of uv's parameters is not settled here
        assertThat(UvLockMetadataBuilder.recordedGit("git+https://github.com/a/b@v1#subdirectory=c")).isNull();
        assertThat(UvLockMetadataBuilder.recordedGit("git+https://github.com/a/b@feature/x")).isNull();

        // the user before the host is not a revision
        assertThat(UvLockMetadataBuilder.recordedGit("git+ssh://git@github.com/a/b.git"))
          .isEqualTo("ssh://git@github.com/a/b.git");
    }

    // ---- structured failures ----

    @Test
    void missingLockRequiresResolution() {
        Result result = UvLockEngine.regenerate(resource("i-minimal-update/pyproject.toml"), null, ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
    }

    @Test
    void markeredAdditionRequiresResolution() {
        // plain adds now resolve (T2); a marker/extra on the added declaration is still deferred
        Result result = UvLockEngine.regenerate(
          """
            [project]
            name = "fixture-i"
            version = "0.1.0"
            requires-python = ">=3.12"
            dependencies = [
                "six==1.17.0",
                "attrs>=23.0; python_version < '3.13'",
            ]
            """,
          resource("i-minimal-update/uv.lock.v2"),
          ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getPackageName()).isEqualTo("attrs");
    }

    @Test
    void newGitSourceIsUnsupported() {
        Result result = UvLockEngine.regenerate(
          """
            [project]
            name = "fixture-i"
            version = "0.1.0"
            requires-python = ">=3.12"
            dependencies = [
                "six==1.17.0",
                "iniconfig>=2.0",
                "mylib",
            ]

            [tool.uv.sources]
            mylib = { git = "https://github.com/example/mylib" }
            """,
          resource("i-minimal-update/uv.lock.v2"),
          ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.UNSUPPORTED_ENTRY_TYPE);
        assertThat(result.getFailure().getPackageName()).isEqualTo("mylib");
    }

    // ---- direct-URL / git sources: pass through untouched, fail loud when targeted ----

    @Test
    void urlSourcedPackagePassesThroughVerbatim() {
        Result result = UvLockEngine.regenerate(
          resource("t-url-source/pyproject.toml"),
          resource("t-url-source/uv.lock"),
          ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent()).isEqualTo(resource("t-url-source/uv.lock"));
        assertThat(http.requests).isEmpty();
    }

    @Test
    void gitSourcedPackagePassesThroughVerbatim() {
        Result result = UvLockEngine.regenerate(
          resource("u-git-source/pyproject.toml"),
          resource("u-git-source/uv.lock"),
          ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent()).isEqualTo(resource("u-git-source/uv.lock"));
        assertThat(http.requests).isEmpty();
    }

    @Test
    void removingDirectorySourcedDependencyDropsItsEntry() {
        Result result = UvLockEngine.regenerate(
          """
            [project]
            name = "dirdep"
            version = "0.1.0"
            requires-python = ">=3.9"
            dependencies = []

            [tool.uv.sources]
            foo = { path = "libs/foo" }
            """,
          resource("v-directory/uv.lock"),
          ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent()).isEqualTo(
          """
            version = 1
            revision = 3
            requires-python = ">=3.9"

            [[package]]
            name = "dirdep"
            version = "0.1.0"
            source = { virtual = "." }
            """);
        assertThat(http.requests).isEmpty();
    }

    @Test
    void workspaceLockRequiresResolution() {
        Result result = UvLockEngine.regenerate(
          """
            [project]
            name = "workspace-root"
            version = "0.1.0"
            requires-python = ">=3.12"
            """,
          resource("q-workspace/uv.lock"),
          ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("Workspace");
    }

    @Test
    void downwardRequiresPythonBumpRequiresResolution() {
        Result result = UvLockEngine.regenerate(
          """
            [project]
            name = "fixture-h"
            version = "0.1.0"
            requires-python = ">=3.10"
            dependencies = [
                "attrs>=23.0",
            ]
            """,
          resource("h-requires-python/uv.lock.after"),
          ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("Lowering");
    }

    @Test
    void forkedDeclarationEditRequiresResolution() {
        Result result = UvLockEngine.regenerate(
          """
            [project]
            name = "fixture-e2"
            version = "0.1.0"
            requires-python = ">=3.10"
            dependencies = [
                "typing-extensions>=4.10; python_version >= '3.11'",
                "typing-extensions<4.5; python_version < '3.11'",
            ]
            """,
          resource("e2-true-fork/uv.lock"),
          ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
    }

    @Test
    void pinExcludedByNewPython() {
        String sixListing = """
          {"files": [
            {"filename": "six-1.17.0-py2.py3-none-any.whl",
             "url": "https://files.pythonhosted.org/packages/b7/ce/149a00dd41f10bc29e5921b496af8b574d8413afcd5e30dfa0ed46c2cc5e/six-1.17.0-py2.py3-none-any.whl",
             "hashes": {"sha256": "4721f391ed90541fddacab5acf947aa0d3dc7d27b2e1e8eda2be8970586c3274"},
             "requires-python": "<3.13"}
          ]}
          """;
        String iniconfigListing = """
          {"files": [
            {"filename": "iniconfig-2.3.0-py3-none-any.whl",
             "url": "https://files.pythonhosted.org/packages/cb/b1/3846dd7f199d53cb17f49cba7e651e9ce294d8497c8c150530ed11865bb8/iniconfig-2.3.0-py3-none-any.whl",
             "hashes": {"sha256": "f631c04d2c48c52b84d0d0549c99ff3859c98df65b3101406327ecc7d53fbf12"},
             "requires-python": ">=3.8"}
          ]}
          """;
        http.route("https://pypi.org/simple/six/", sixListing);
        http.route("https://pypi.org/simple/iniconfig/", iniconfigListing);
        Result result = UvLockEngine.regenerate(
          """
            [project]
            name = "fixture-i"
            version = "0.1.0"
            requires-python = ">=3.13"
            dependencies = [
                "six==1.17.0",
                "iniconfig>=2.0",
            ]
            """,
          resource("i-minimal-update/uv.lock.v2"),
          ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.PIN_EXCLUDED_BY_PYTHON);
        assertThat(result.getFailure().getPackageName()).isEqualTo("six");
    }

    @Test
    void malformedLockIsReported() {
        Result result = UvLockEngine.regenerate(
          resource("i-minimal-update/pyproject.toml"),
          "version = 1\n\n[mystery]\nkey = \"value\"\n",
          ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.MALFORMED_LOCK);
        assertThat(result.getFailure().getDetail()).contains("[mystery]");
    }

    @Test
    void malformedManifestIsReported() {
        Result result = UvLockEngine.regenerate(
          "[project\nname = broken",
          resource("i-minimal-update/uv.lock.v2"),
          ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.MALFORMED_MANIFEST);
        // the underlying TOML parse error is appended for diagnosability, not swallowed
        assertThat(result.getFailure().getDetail())
          .contains("could not be parsed as TOML:")
          .contains("Syntax error");
    }

    @Test
    void unreachableIndexIsReported() {
        // no route for the listing -> 404 from the only configured index
        Result result = UvLockEngine.regenerate(
          resource("i-minimal-update/pyproject.toml"),
          resource("i-minimal-update/uv.lock.v1"),
          ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.PACKAGE_NOT_FOUND);
    }

    // ---- python bump must never leave a package without any distribution ----

    @Test
    void pythonBumpLeavingNoDistributionFails() {
        Result result = UvLockEngine.regenerate(
          """
            [project]
            name = "fixture-bump"
            version = "0.1.0"
            requires-python = ">=3.12"
            dependencies = [
                "legacy>=1.0",
            ]
            """,
          """
            version = 1
            requires-python = ">=3.9"

            [[package]]
            name = "fixture-bump"
            version = "0.1.0"
            source = { virtual = "." }
            dependencies = [
                { name = "legacy" },
            ]

            [package.metadata]
            requires-dist = [{ name = "legacy", specifier = ">=1.0" }]

            [[package]]
            name = "legacy"
            version = "1.0.0"
            source = { registry = "https://pypi.org/simple" }
            wheels = [
                { url = "https://files.pythonhosted.org/b/legacy-1.0.0-cp39-cp39-manylinux_2_17_x86_64.whl", hash = "sha256:aaaa", size = 100 },
            ]
            """,
          ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.PIN_EXCLUDED_BY_PYTHON);
        assertThat(result.getFailure().getPackageName()).isEqualTo("legacy");
        assertThat(result.getFailure().getDetail()).contains("no sdist");
        assertThat(http.requests).isEmpty();
    }

    // ---- version selection honors constraints from unchanged sections ----

    private static final String MULTI_SECTION_LOCK = """
      version = 1
      requires-python = ">=3.12"

      [[package]]
      name = "fixture-f3"
      version = "0.1.0"
      source = { virtual = "." }
      dependencies = [
          { name = "six" },
      ]

      [package.dev-dependencies]
      dev = [
          { name = "six" },
      ]

      [package.metadata]
      requires-dist = [{ name = "six", specifier = "<2" }]

      [package.metadata.requires-dev]
      dev = [{ name = "six", specifier = ">=1.16" }]

      [[package]]
      name = "six"
      version = "1.16.0"
      source = { registry = "https://pypi.org/simple" }
      wheels = [
          { url = "https://files.pythonhosted.org/f3/six-1.16.0-py3-none-any.whl", hash = "sha256:aaaa", size = 100 },
      ]
      """;

    private static String multiSectionPyproject(String devConstraint) {
        return """
          [project]
          name = "fixture-f3"
          version = "0.1.0"
          requires-python = ">=3.12"
          dependencies = [
              "six<2",
          ]

          [dependency-groups]
          dev = [
              "six%s",
          ]
          """.formatted(devConstraint);
    }

    private void stubMultiSectionSix() {
        http.route("https://pypi.org/simple/six/", """
          {"files": [
            {"filename": "six-1.16.0-py3-none-any.whl", "url": "https://files.pythonhosted.org/f3/six-1.16.0-py3-none-any.whl", "hashes": {"sha256": "aaaa"}, "requires-python": ">=3.6"},
            {"filename": "six-1.16.5-py3-none-any.whl", "url": "https://files.pythonhosted.org/f3/six-1.16.5-py3-none-any.whl", "hashes": {"sha256": "bbbb"}, "requires-python": ">=3.6"},
            {"filename": "six-3.0.0-py3-none-any.whl", "url": "https://files.pythonhosted.org/f3/six-3.0.0-py3-none-any.whl", "hashes": {"sha256": "cccc"}, "requires-python": ">=3.6"}
          ]}
          """);
        http.route("https://files.pythonhosted.org/f3/six-1.16.5-py3-none-any.whl.metadata",
          "Metadata-Version: 2.1\nName: six\nVersion: 1.16.5\n");
    }

    @Test
    void versionSelectionIntersectsConstraintsFromAllSections() {
        stubMultiSectionSix();
        // the dev group's bump excludes the pin, but requires-dist's unchanged <2 must still cap the pick
        Result result = UvLockEngine.regenerate(multiSectionPyproject(">=1.16.1"), MULTI_SECTION_LOCK, ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent())
          .contains("version = \"1.16.5\"")
          .doesNotContain("3.0.0");
    }

    @Test
    void conflictingConstraintsAcrossSectionsAreReported() {
        stubMultiSectionSix();
        Result result = UvLockEngine.regenerate(multiSectionPyproject(">=2"), MULTI_SECTION_LOCK, ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_CONFLICT);
        assertThat(result.getFailure().getPackageName()).isEqualTo("six");
    }

    // ---- reachability sweep: non-root editable packages are not roots ----

    @Test
    void editablePackageOrphanedByRemovalIsSwept() {
        Result result = UvLockEngine.regenerate(
          """
            [project]
            name = "fixture-f4"
            version = "0.1.0"
            requires-python = ">=3.12"
            dependencies = [
                "iniconfig>=2.0",
            ]
            """,
          """
            version = 1
            requires-python = ">=3.12"

            [[package]]
            name = "fixture-f4"
            version = "0.1.0"
            source = { virtual = "." }
            dependencies = [
                { name = "iniconfig" },
                { name = "wrapper" },
            ]

            [package.metadata]
            requires-dist = [
                { name = "iniconfig", specifier = ">=2.0" },
                { name = "wrapper", specifier = ">=1.0" },
            ]

            [[package]]
            name = "helper"
            version = "0.1.0"
            source = { editable = "libs/helper" }

            [[package]]
            name = "iniconfig"
            version = "2.3.0"
            source = { registry = "https://pypi.org/simple" }
            sdist = { url = "https://files.pythonhosted.org/f4/iniconfig-2.3.0.tar.gz", hash = "sha256:aaaa", size = 100 }

            [[package]]
            name = "wrapper"
            version = "1.0.0"
            source = { registry = "https://pypi.org/simple" }
            dependencies = [
                { name = "helper" },
            ]
            sdist = { url = "https://files.pythonhosted.org/f4/wrapper-1.0.0.tar.gz", hash = "sha256:bbbb", size = 100 }
            """,
          ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent())
          .contains("iniconfig")
          .doesNotContain("wrapper")
          .doesNotContain("helper");
        assertThat(http.requests).isEmpty();
    }

    // ---- markers gating on multiple extras ----

    private static final String MULTI_EXTRA_LOCK = """
      version = 1
      requires-python = ">=3.12"

      [[package]]
      name = "fixture-f5"
      version = "0.1.0"
      source = { virtual = "." }

      [package.metadata]
      requires-dist = [{ name = "six", marker = "extra == 'a' and extra == 'x'", specifier = ">=1.16" }]
      provides-extras = ["a", "x"]
      """;

    private static String multiExtraPyproject(String groupX) {
        return """
          [project]
          name = "fixture-f5"
          version = "0.1.0"
          requires-python = ">=3.12"

          [project.optional-dependencies]
          a = []
          x = [%s]
          """.formatted(groupX);
    }

    @Test
    void unchangedMultiExtraDeclarationPassesThroughVerbatim() {
        Result result = UvLockEngine.regenerate(
          multiExtraPyproject("\n    \"six>=1.16; extra == 'a'\",\n"), MULTI_EXTRA_LOCK, ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent()).isEqualTo(MULTI_EXTRA_LOCK);
        assertThat(http.requests).isEmpty();
    }

    @Test
    void editingMultiExtraDeclarationRequiresResolution() {
        Result result = UvLockEngine.regenerate(
          multiExtraPyproject("\n    \"six>=1.17; extra == 'a'\",\n"), MULTI_EXTRA_LOCK, ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("multiple extras");
    }

    @Test
    void removingMultiExtraDeclarationRequiresResolution() {
        Result result = UvLockEngine.regenerate(multiExtraPyproject(""), MULTI_EXTRA_LOCK, ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("multiple extras");
    }

    // ---- rewritten dependency edges record extras sorted (g3-multi-extras) ----

    @Test
    void rewrittenEdgeRecordsExtrasSorted() {
        http.route("https://pypi.org/simple/mainpkg/", """
          {"files": [
            {"filename": "mainpkg-1.1.0-py3-none-any.whl", "url": "https://files.pythonhosted.org/f8/mainpkg-1.1.0-py3-none-any.whl", "hashes": {"sha256": "dddd"}, "requires-python": ">=3.9"}
          ]}
          """);
        http.route("https://files.pythonhosted.org/f8/mainpkg-1.1.0-py3-none-any.whl.metadata",
          "Metadata-Version: 2.1\nName: mainpkg\nVersion: 1.1.0\nRequires-Dist: target[zeta,alpha]==2.0.0\n");
        Result result = UvLockEngine.regenerate(
          """
            [project]
            name = "fixture-f8"
            version = "0.1.0"
            requires-python = ">=3.12"
            dependencies = [
                "mainpkg==1.1.0",
            ]
            """,
          """
            version = 1
            requires-python = ">=3.12"

            [[package]]
            name = "fixture-f8"
            version = "0.1.0"
            source = { virtual = "." }
            dependencies = [
                { name = "mainpkg" },
            ]

            [package.metadata]
            requires-dist = [{ name = "mainpkg", specifier = "==1.0.0" }]

            [[package]]
            name = "mainpkg"
            version = "1.0.0"
            source = { registry = "https://pypi.org/simple" }
            dependencies = [
                { name = "target", extra = ["alpha", "zeta"] },
            ]
            sdist = { url = "https://files.pythonhosted.org/f8/mainpkg-1.0.0.tar.gz", hash = "sha256:aaaa", size = 100 }

            [[package]]
            name = "pad"
            version = "1.0.0"
            source = { registry = "https://pypi.org/simple" }
            sdist = { url = "https://files.pythonhosted.org/f8/pad-1.0.0.tar.gz", hash = "sha256:bbbb", size = 100 }

            [[package]]
            name = "target"
            version = "2.0.0"
            source = { registry = "https://pypi.org/simple" }
            sdist = { url = "https://files.pythonhosted.org/f8/target-2.0.0.tar.gz", hash = "sha256:cccc", size = 100 }

            [package.optional-dependencies]
            alpha = [
                { name = "pad" },
            ]
            zeta = [
                { name = "pad" },
            ]
            """,
          ctx);
        assertThat(result.getErrorMessage()).isNull();
        // declared unsorted [zeta, alpha]; the recorded edge sorts them
        assertThat(result.getLockFileContent())
          .contains("{ name = \"target\", extra = [\"alpha\", \"zeta\"] }");
    }

    @Test
    void excludeNewerSelectsAmongFilesUploadedBeforeTheCutoffOfTheirPackage() {
        // a week before this moment, the fixture's P1W span, puts the cutoff at 2026-10-14T00:00:00Z
        ctx.putMessage(UvLockEngine.CURRENT_TIME, Instant.parse("2026-10-21T00:00:00Z"));
        http.route("https://pypi.org/simple/six/", """
          {"files": [
            {"filename": "six-1.17.0-py3-none-any.whl", "url": "https://files.pythonhosted.org/f3/six-1.17.0-py3-none-any.whl", "hashes": {"sha256": "aaaa"}, "upload-time": "2026-10-13T23:59:59.999Z"},
            {"filename": "six-1.18.0-py3-none-any.whl", "url": "https://files.pythonhosted.org/f3/six-1.18.0-py3-none-any.whl", "hashes": {"sha256": "bbbb"}, "upload-time": "2026-10-14T00:00:00Z"},
            {"filename": "six-1.19.0-py3-none-any.whl", "url": "https://files.pythonhosted.org/f3/six-1.19.0-py3-none-any.whl", "hashes": {"sha256": "cccc"}}
          ]}
          """);
        http.route("https://files.pythonhosted.org/f3/six-1.17.0-py3-none-any.whl.metadata",
          "Metadata-Version: 2.1\nName: six\nVersion: 1.17.0\n");
        Result result = UvLockEngine.regenerate(
          resource("m4-exclude-newer-span/pyproject.toml").replace("six==1.16.0", "six>=1.17"),
          resource("m4-exclude-newer-span/uv.lock"),
          ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent())
          .contains("version = \"1.17.0\"")
          .doesNotContain("1.18.0")
          .doesNotContain("1.19.0");

        // exclude-newer-package lifts the cutoff for one package
        http.route("https://files.pythonhosted.org/f3/six-1.19.0-py3-none-any.whl.metadata",
          "Metadata-Version: 2.1\nName: six\nVersion: 1.19.0\n");
        String lifted = resource("m4-exclude-newer-span/uv.lock").replace("exclude-newer-span = \"P1W\"\n",
          "exclude-newer-span = \"P1W\"\n\n[options.exclude-newer-package]\nsix = false\n");
        result = UvLockEngine.regenerate(
          resource("m4-exclude-newer-span/pyproject.toml").replace("six==1.16.0", "six>=1.17"), lifted, ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent()).contains("version = \"1.19.0\"");
    }

    // ---- lazy wheel metadata: listing advertises no PEP 658 sidecar ----

    @Test
    void lazyWheelMetadataWhenListingAdvertisesNoSidecar() {
        byte[] wheel = tinyWheel("six-1.18.0.dist-info", "Metadata-Version: 2.1\nName: six\nVersion: 1.18.0\n");
        http.route("https://pypi.org/simple/six/", """
          {"files": [
            {"filename": "six-1.18.0-py2.py3-none-any.whl",
             "url": "https://files.pythonhosted.org/lazy/six-1.18.0-py2.py3-none-any.whl",
             "hashes": {"sha256": "9f7b21c8a5c48e271941bcbcca206c8e10ab1a624b2a60875bd0ab5a4ec53ec1"},
             "size": %d,
             "requires-python": ">=3.6"}
          ]}
          """.formatted(wheel.length));
        http.route("https://files.pythonhosted.org/lazy/six-1.18.0-py2.py3-none-any.whl", wheel);
        Result result = UvLockEngine.regenerate(
          """
            [project]
            name = "fixture-i"
            version = "0.1.0"
            requires-python = ">=3.12"
            dependencies = [
                "six==1.18.0",
                "iniconfig>=2.0",
            ]
            """,
          resource("i-minimal-update/uv.lock.v2"),
          ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent())
          .contains("version = \"1.18.0\"")
          .contains("six-1.18.0-py2.py3-none-any.whl");
        // no sidecar (404) -> the wheel bytes themselves were fetched and read
        assertThat(http.requests).contains(
          "https://files.pythonhosted.org/lazy/six-1.18.0-py2.py3-none-any.whl.metadata",
          "https://files.pythonhosted.org/lazy/six-1.18.0-py2.py3-none-any.whl");
    }

    @Test
    void requirementsOnOnePackageUnderDifferentMarkersForkTheLockAndShareAnEdge() {
        byte[] wheel = tinyWheel("six-1.18.0.dist-info", """
          Metadata-Version: 2.1
          Name: six
          Version: 1.18.0
          Requires-Dist: iniconfig>=2.0; sys_platform == 'win32'
          Requires-Dist: iniconfig>=2.0; sys_platform == 'linux'
          """);
        http.route("https://pypi.org/simple/six/", """
          {"files": [
            {"filename": "six-1.18.0-py2.py3-none-any.whl",
             "url": "https://files.pythonhosted.org/lazy/six-1.18.0-py2.py3-none-any.whl",
             "hashes": {"sha256": "9f7b21c8a5c48e271941bcbcca206c8e10ab1a624b2a60875bd0ab5a4ec53ec1"},
             "size": %d,
             "requires-python": ">=3.6"}
          ]}
          """.formatted(wheel.length));
        http.route("https://files.pythonhosted.org/lazy/six-1.18.0-py2.py3-none-any.whl", wheel);
        Result result = UvLockEngine.regenerate(
          """
            [project]
            name = "fixture-i"
            version = "0.1.0"
            requires-python = ">=3.12"
            dependencies = [
                "six==1.18.0",
                "iniconfig>=2.0",
            ]
            """,
          resource("i-minimal-update/uv.lock.v2"),
          ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent())
          .contains("""
            resolution-markers = [
                "sys_platform == 'win32'",
                "sys_platform == 'linux'",
                "sys_platform != 'linux' and sys_platform != 'win32'",
            ]
            """)
          .contains("""
            dependencies = [
                { name = "iniconfig", marker = "sys_platform == 'linux' or sys_platform == 'win32'" },
            ]
            """);
    }

    @Test
    void movingAPackageAGitPackageDependsOnRequiresResolution() {
        byte[] wheel = tinyWheel("six-1.18.0.dist-info", "Metadata-Version: 2.1\nName: six\nVersion: 1.18.0\n");
        http.route("https://pypi.org/simple/six/", """
          {"files": [
            {"filename": "six-1.18.0-py2.py3-none-any.whl",
             "url": "https://files.pythonhosted.org/lazy/six-1.18.0-py2.py3-none-any.whl",
             "hashes": {"sha256": "9f7b21c8a5c48e271941bcbcca206c8e10ab1a624b2a60875bd0ab5a4ec53ec1"},
             "size": %d,
             "requires-python": ">=3.6"}
          ]}
          """.formatted(wheel.length));
        http.route("https://files.pythonhosted.org/lazy/six-1.18.0-py2.py3-none-any.whl", wheel);
        Result result = UvLockEngine.regenerate(
          """
            [project]
            name = "gitdep"
            version = "0.1.0"
            requires-python = ">=3.12"
            dependencies = ["gitpkg", "six==1.18.0"]

            [tool.uv.sources]
            gitpkg = { git = "https://github.com/example/gitpkg", tag = "1.0" }
            """,
          """
            version = 1
            revision = 3
            requires-python = ">=3.12"

            [[package]]
            name = "gitdep"
            version = "0.1.0"
            source = { virtual = "." }
            dependencies = [
                { name = "gitpkg" },
                { name = "six" },
            ]

            [package.metadata]
            requires-dist = [
                { name = "gitpkg", git = "https://github.com/example/gitpkg?tag=1.0" },
                { name = "six", specifier = "==1.17.0" },
            ]

            [[package]]
            name = "gitpkg"
            version = "1.0"
            source = { git = "https://github.com/example/gitpkg?tag=1.0#0123456789abcdef0123456789abcdef01234567" }
            dependencies = [
                { name = "six" },
            ]

            [[package]]
            name = "six"
            version = "1.17.0"
            source = { registry = "https://pypi.org/simple" }
            wheels = [
                { url = "https://files.pythonhosted.org/packages/b7/ce/149a00dd41f10bc29e5921b496af8b574d8413afcd5e30dfa0ed46c2cc5e/six-1.17.0-py2.py3-none-any.whl", hash = "sha256:4721f391ed90541fddacab5acf947aa0d3dc7d27b2e1e8eda2be8970586c3274", size = 11050, upload-time = "2024-12-04T17:35:26.475Z" },
            ]
            """,
          ctx);
        // the lock records no requirements for a git package, so nothing shows it still accepts six 1.18.0
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getPackageName()).isEqualTo("six");
    }

    @Test
    void retargetingARegistryDeclarationToGitRequiresResolution() {
        Result result = UvLockEngine.regenerate(
          """
            [project]
            name = "fixture-i"
            version = "0.1.0"
            requires-python = ">=3.12"
            dependencies = [
                "six @ git+https://github.com/benjaminp/six@1.17.0",
                "iniconfig>=2.0",
            ]
            """,
          resource("i-minimal-update/uv.lock.v2"),
          ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.UNSUPPORTED_ENTRY_TYPE);
        assertThat(result.getFailure().getPackageName()).isEqualTo("six");
    }

    @Test
    void addedPep508GitDeclarationIsUnsupported() {
        Result result = UvLockEngine.regenerate(
          """
            [project]
            name = "fixture-i"
            version = "0.1.0"
            requires-python = ">=3.12"
            dependencies = [
                "six==1.17.0",
                "iniconfig>=2.0",
                "mylib @ git+https://github.com/example/mylib@v1",
            ]
            """,
          resource("i-minimal-update/uv.lock.v2"),
          ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.UNSUPPORTED_ENTRY_TYPE);
        assertThat(result.getFailure().getPackageName()).isEqualTo("mylib");
        assertThat(http.requests).isEmpty();
    }

    @Test
    void extrasAddedToAGitDeclarationAreNotIgnored() {
        Result result = UvLockEngine.regenerate(
          """
            [project]
            name = "gitprobe"
            version = "0.1.0"
            requires-python = ">=3.9"
            dependencies = ["six[extra]"]

            [tool.uv.sources]
            six = { git = "https://github.com/benjaminp/six", tag = "1.17.0" }
            """,
          resource("u-git-source/uv.lock"),
          ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getPackageName()).isEqualTo("six");
    }

    @Test
    void retargetFromGitToTheRegistryKeepsTheExtrasDeclaredWithIt() {
        byte[] wheel = tinyWheel("six-1.18.0.dist-info", """
          Metadata-Version: 2.1
          Name: six
          Version: 1.18.0
          Provides-Extra: fast
          Requires-Dist: iniconfig; extra == 'fast'
          """);
        byte[] iniconfig = tinyWheel("iniconfig-2.3.0.dist-info",
          "Metadata-Version: 2.1\nName: iniconfig\nVersion: 2.3.0\n");
        http.route("https://pypi.org/simple/iniconfig/", """
          {"files": [
            {"filename": "iniconfig-2.3.0-py3-none-any.whl",
             "url": "https://files.pythonhosted.org/lazy/iniconfig-2.3.0-py3-none-any.whl",
             "hashes": {"sha256": "f631c04d2c48c52b84d0d0549c99ff3859c98df65b3101406327ecc7d53fbf12"},
             "size": %d,
             "requires-python": ">=3.8"}
          ]}
          """.formatted(iniconfig.length));
        http.route("https://files.pythonhosted.org/lazy/iniconfig-2.3.0-py3-none-any.whl", iniconfig);
        http.route("https://pypi.org/simple/six/", """
          {"files": [
            {"filename": "six-1.18.0-py2.py3-none-any.whl",
             "url": "https://files.pythonhosted.org/lazy/six-1.18.0-py2.py3-none-any.whl",
             "hashes": {"sha256": "9f7b21c8a5c48e271941bcbcca206c8e10ab1a624b2a60875bd0ab5a4ec53ec1"},
             "size": %d,
             "requires-python": ">=3.6"}
          ]}
          """.formatted(wheel.length));
        http.route("https://files.pythonhosted.org/lazy/six-1.18.0-py2.py3-none-any.whl", wheel);
        Result result = UvLockEngine.regenerate(
          """
            [project]
            name = "gitprobe"
            version = "0.1.0"
            requires-python = ">=3.9"
            dependencies = ["six[fast]==1.18.0"]
            """,
          resource("u-git-source/uv.lock"),
          ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent()).contains("{ name = \"six\", extra = [\"fast\"] }");
    }

    @Test
    void gitSourceWrittenInlineUnderToolUvIsNotARegistryPackage() {
        Result result = UvLockEngine.regenerate(
          """
            [project]
            name = "gitprobe"
            version = "0.1.0"
            requires-python = ">=3.9"
            dependencies = ["six"]

            [tool.uv]
            sources = { six = { git = "https://github.com/benjaminp/six", tag = "1.17.0" } }
            """,
          resource("u-git-source/uv.lock"),
          ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent()).isEqualTo(resource("u-git-source/uv.lock"));
        assertThat(http.requests).isEmpty();
    }

    @Test
    void aChangedLockRecordsEachIndexAsItIsConfigured() {
        PythonExecutionContextView.view(ctx).setUvVersion("0.13.4");
        byte[] wheel = tinyWheel("six-1.18.0.dist-info", "Metadata-Version: 2.1\nName: six\nVersion: 1.18.0\n");
        http.route("https://pypi.org/simple/six/", """
          {"files": [
            {"filename": "six-1.18.0-py2.py3-none-any.whl",
             "url": "https://files.pythonhosted.org/lazy/six-1.18.0-py2.py3-none-any.whl",
             "hashes": {"sha256": "9f7b21c8a5c48e271941bcbcca206c8e10ab1a624b2a60875bd0ab5a4ec53ec1"},
             "size": %d,
             "requires-python": ">=3.6"}
          ]}
          """.formatted(wheel.length));
        http.route("https://files.pythonhosted.org/lazy/six-1.18.0-py2.py3-none-any.whl", wheel);
        http.route("https://pypi.org/simple/iniconfig/", """
          {"files": [
            {"filename": "iniconfig-2.3.0-py3-none-any.whl",
             "url": "https://files.pythonhosted.org/packages/01/ini/iniconfig-2.3.0-py3-none-any.whl",
             "hashes": {"sha256": "f631c04d2c48c52b84d0d0549c99ff3859c98df65b3101406327ecc7d53fbf12"},
             "size": 7484,
             "requires-python": ">=3.8"}
          ]}
          """);
        // a lock written against https://pypi.org/simple/ records the configured form once anything changes
        String before = resource("i-minimal-update/uv.lock.v2")
          .replace("https://pypi.org/simple\"", "https://pypi.org/simple/\"");
        Result result = UvLockEngine.regenerate(
          """
            [project]
            name = "fixture-i"
            version = "0.1.0"
            requires-python = ">=3.12"
            dependencies = [
                "six==1.18.0",
                "iniconfig>=2.0",
            ]
            """,
          before,
          ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent())
          .contains("registry = \"https://pypi.org/simple\"")
          .doesNotContain("registry = \"https://pypi.org/simple/\"");
    }

    private static byte[] tinyWheel(String distInfoPrefix, String metadata) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(out)) {
                zip.putNextEntry(new ZipEntry(distInfoPrefix + "/METADATA"));
                zip.write(metadata.getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ---- marker clause truth over the requires-python interval ----

    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "varies", value = {
      // == wildcard: containment / exclusion of the wildcard interval
      ">=3.12, <3.13 | python_full_version | == | 3.12.*  | true",
      ">=3.10, <3.13 | python_full_version | == | 3.10.*  | varies",
      ">=3.12        | python_full_version | == | 3.11.*  | false",
      ">=3.12        | python_full_version | == | 3.12.*  | varies",
      ">=3.12        | python_full_version | != | 3.11.*  | true",
      ">=3.12, <3.13 | python_full_version | != | 3.12.*  | false",
      // python_version equality is truncated equality, i.e. an implicit wildcard
      ">=3.12, <3.13 | python_version      | == | 3.12    | true",
      ">=3.12        | python_version      | != | 3.9     | true",
      ">=3.12        | python_version      | == | 3.12    | varies",
      ">=3.12        | python_version      | == | 3.12.1  | varies",
      // exact full-version equality is a single point
      ">=3.12, <3.13 | python_full_version | == | 3.12    | varies",
      ">=3.12        | python_full_version | == | 3.9     | false",
      ">=3.12        | python_full_version | != | 3.9     | true",
      ">=3.11, <=3.11 | python_full_version | == | 3.11   | true",
      // ~= expands to its >=/< pair
      ">=3.10, <3.13 | python_full_version | ~= | 3.10    | true",
      ">=3.12        | python_full_version | ~= | 3.10    | varies",
      ">=3.12        | python_full_version | ~= | 3.10.2  | false",
      ">=3.10, <3.11 | python_full_version | ~= | 3.10.2  | varies",
      // exclusive bounds sit inside the interval
      ">3.11, <3.12  | python_full_version | == | 3.11.*  | true",
      ">3.11         | python_full_version | == | 3.11.*  | varies",
      ">3.11         | python_full_version | != | 3.10.*  | true",
      // wildcard under an ordering operator stays undecided
      ">=3.12        | python_full_version | >= | 3.11.*  | varies",
    })
    void clauseTruthOverRequiresPythonInterval(String requiresPython, String var, String op, String value,
                                               Boolean expected) {
        assertThat(UvLockEngine.clauseTruth(requiresPython, var, op, value)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
      "2024-12-04T17:35:28.174320Z, 2024-12-04T17:35:28.174Z",
      "2024-12-04T17:35:26.475808Z, 2024-12-04T17:35:26.475Z",
      "2026-07-07T14:33:57.900000Z, 2026-07-07T14:33:57.9Z",
      "2026-07-07T14:34:03.040000Z, 2026-07-07T14:34:03.04Z",
      "2021-05-05T14:18:17.237000Z, 2021-05-05T14:18:17.237Z",
      "2021-05-05T14:18:17.000000Z, 2021-05-05T14:18:17Z",
      "2021-05-05T14:18:17Z, 2021-05-05T14:18:17Z",
    })
    void uploadTimeTruncatesToMillisAndTrimsZeros(String pep700, String expected) {
        assertThat(UvLockEngine.formatUploadTime(pep700)).isEqualTo(expected);
    }
}
