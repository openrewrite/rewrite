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
 * Recorded index/metadata responses under {@code /uvlock/http/} keep the tests offline.
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
    void removingDirectorySourcedDependencyIsUnsupported() {
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
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.UNSUPPORTED_ENTRY_TYPE);
        assertThat(result.getFailure().getPackageName()).isEqualTo("foo");
        assertThat(http.requests).isEmpty();
    }

    @Test
    void removingUrlSourcedDependencyIsUnsupported() {
        Result result = UvLockEngine.regenerate(
          """
            [project]
            name = "urlsdist"
            version = "0.1.0"
            requires-python = ">=3.9"
            dependencies = []

            [tool.uv.sources]
            six = { url = "https://files.pythonhosted.org/packages/94/e7/b2c673351809dca68a0e064b6af791aa332cf192da575fd474ed7d6f16a2/six-1.17.0.tar.gz" }
            """,
          resource("t-url-source/uv.lock"),
          ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.UNSUPPORTED_ENTRY_TYPE);
        assertThat(result.getFailure().getPackageName()).isEqualTo("six");
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
