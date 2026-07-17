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
package org.openrewrite.python.internal.pipfilelock;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.python.PythonExecutionContextView;
import org.openrewrite.python.PythonPackageIndex;
import org.openrewrite.python.internal.LockFileRegeneration;
import org.openrewrite.python.internal.LockFileRegeneration.Reason;
import org.openrewrite.python.internal.LockFileRegeneration.Result;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Expected {@code _meta.hash} values were recorded from the repo's python3 oracle
 * ({@code src/test/resources/pipfilelock/oracle.py hash}); tests never execute python.
 */
class PipenvLockEngineTest {

    private static final String WHEEL_2324 = "a".repeat(64);
    private static final String SDIST_2324 = "b".repeat(64);
    private static final String WHEEL_2310 = "c".repeat(64);
    private static final String SDIST_2310 = "d".repeat(64);
    private static final String CERTIFI = "e".repeat(64);
    private static final String IDNA = "f".repeat(64);

    private static final String HASH_UPGRADE = "ced9cbb952e77a887d5ea7e0414f526fdfcf8400b3dc93ffb9e903b148cd6518";
    private static final String HASH_BUMP = "9929204341e286c76af541dd6463d932ce482a416e080bb00cd17ab801b10c5e";
    private static final String HASH_REMOVAL = "ed6d5d614626ae28e274e453164affb26694755170ccab3aa5866f093d51d3e4";

    MockWebServer server;
    ExecutionContext ctx;
    final Map<String, Supplier<MockResponse>> routes = new HashMap<>();

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                Supplier<MockResponse> response = routes.get(request.getPath());
                return response != null ? response.get() : new MockResponse().setResponseCode(404);
            }
        });
        server.start();
        ctx = new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });
        PythonExecutionContextView.view(ctx).setPackageIndexes(List.of(
          new PythonPackageIndex("pypi", server.url("/simple").toString(), true, null, null, false)));
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
        routes.clear();
    }

    private void listing(String pkg, String... files) {
        String body = "{\"files\": [" + String.join(", ", files) + "]}";
        routes.put("/simple/" + pkg + "/", () -> new MockResponse()
          .setHeader("Content-Type", "application/vnd.pypi.simple.v1+json")
          .setBody(body));
    }

    private void metadata(String filename, String content) {
        routes.put("/packages/" + filename + ".metadata", () -> new MockResponse().setBody(content));
    }

    private static String wheel(String filename, String sha256, String requiresPython) {
        return String.format(
          "{\"filename\": \"%s\", \"url\": \"/packages/%s\", \"hashes\": {\"sha256\": \"%s\"}, " +
            "\"requires-python\": \"%s\", \"core-metadata\": true}",
          filename, filename, sha256, requiresPython);
    }

    private static String wheelWithoutHash(String filename, String requiresPython) {
        return String.format(
          "{\"filename\": \"%s\", \"url\": \"/packages/%s\", \"hashes\": {}, " +
            "\"requires-python\": \"%s\", \"core-metadata\": true}",
          filename, filename, requiresPython);
    }

    private static String sdist(String filename, String sha256, String requiresPython) {
        return String.format(
          "{\"filename\": \"%s\", \"url\": \"/packages/%s\", \"hashes\": {\"sha256\": \"%s\"}, " +
            "\"requires-python\": \"%s\"}",
          filename, filename, sha256, requiresPython);
    }

    private void stubRequestsBaseline() {
        listing("requests",
          wheel("requests-2.31.0-py3-none-any.whl", WHEEL_2310, ">=3.7"),
          sdist("requests-2.31.0.tar.gz", SDIST_2310, ">=3.7"),
          wheel("requests-2.32.4-py3-none-any.whl", WHEEL_2324, ">=3.8"),
          sdist("requests-2.32.4.tar.gz", SDIST_2324, ">=3.8"));
        metadata("requests-2.32.4-py3-none-any.whl", """
          Metadata-Version: 2.1
          Name: requests
          Version: 2.32.4
          Requires-Python: >=3.8
          Requires-Dist: certifi>=2017.4.17
          Requires-Dist: idna<4,>=2.5
          """);
    }

    private static String baseLock() {
        return """
          {
              "_meta": {
                  "hash": {
                      "sha256": "0000000000000000000000000000000000000000000000000000000000000000"
                  },
                  "pipfile-spec": 6,
                  "requires": {
                      "python_version": "3.11"
                  },
                  "sources": [
                      {
                          "name": "pypi",
                          "url": "https://pypi.org/simple",
                          "verify_ssl": true
                      }
                  ]
              },
              "default": {
                  "certifi": {
                      "hashes": [
                          "sha256:%s"
                      ],
                      "markers": "python_version >= '3.6'",
                      "version": "==2024.2.2"
                  },
                  "idna": {
                      "hashes": [
                          "sha256:%s"
                      ],
                      "markers": "python_version >= '3.5'",
                      "version": "==3.7"
                  },
                  "requests": {
                      "hashes": [
                          "sha256:%s",
                          "sha256:%s"
                      ],
                      "index": "pypi",
                      "markers": "python_version >= '3.7'",
                      "version": "==2.31.0"
                  }
              },
              "develop": {}
          }
          """.formatted(CERTIFI, IDNA, WHEEL_2310, SDIST_2310);
    }

    @Test
    void closureUnchangedUpgradeEmitsFullLockByteForByte() {
        stubRequestsBaseline();
        String pipfile = """
          [[source]]
          url = "https://pypi.org/simple"
          verify_ssl = true
          name = "pypi"

          [packages]
          requests = ">=2.32.0"

          [requires]
          python_version = "3.11"
          """;

        Result result = PipenvLockEngine.regenerate(pipfile, baseLock(), ctx);

        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo("""
          {
              "_meta": {
                  "hash": {
                      "sha256": "%s"
                  },
                  "pipfile-spec": 6,
                  "requires": {
                      "python_version": "3.11"
                  },
                  "sources": [
                      {
                          "name": "pypi",
                          "url": "https://pypi.org/simple",
                          "verify_ssl": true
                      }
                  ]
              },
              "default": {
                  "certifi": {
                      "hashes": [
                          "sha256:%s"
                      ],
                      "markers": "python_version >= '3.6'",
                      "version": "==2024.2.2"
                  },
                  "idna": {
                      "hashes": [
                          "sha256:%s"
                      ],
                      "markers": "python_version >= '3.5'",
                      "version": "==3.7"
                  },
                  "requests": {
                      "hashes": [
                          "sha256:%s",
                          "sha256:%s"
                      ],
                      "index": "pypi",
                      "markers": "python_version >= '3.8'",
                      "version": "==2.32.4"
                  }
              },
              "develop": {}
          }
          """.formatted(HASH_UPGRADE, CERTIFI, IDNA, WHEEL_2324, SDIST_2324));
    }

    @Test
    void requiresBumpRevalidatesEveryPinAndRewritesMeta() {
        listing("requests",
          wheel("requests-2.31.0-py3-none-any.whl", WHEEL_2310, ">=3.7"),
          sdist("requests-2.31.0.tar.gz", SDIST_2310, ">=3.7"));
        listing("certifi", wheel("certifi-2024.2.2-py3-none-any.whl", CERTIFI, ">=3.6"));
        listing("idna", wheel("idna-3.7-py3-none-any.whl", IDNA, ">=3.5"));
        String pipfile = """
          [[source]]
          url = "https://pypi.org/simple"
          verify_ssl = true
          name = "pypi"

          [packages]
          requests = ">=2.31.0"

          [requires]
          python_version = "3.12"
          """;

        Result result = PipenvLockEngine.regenerate(pipfile, baseLock(), ctx);

        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(
          baseLock()
            .replace("0".repeat(64), HASH_BUMP)
            .replace("\"python_version\": \"3.11\"", "\"python_version\": \"3.12\""));
    }

    @Test
    void requiresBumpFailsWhenPinExcludedByNewPython() {
        listing("requests",
          wheel("requests-2.31.0-py3-none-any.whl", WHEEL_2310, ">=3.7"),
          sdist("requests-2.31.0.tar.gz", SDIST_2310, ">=3.7"));
        listing("certifi", wheel("certifi-2024.2.2-py3-none-any.whl", CERTIFI, ">=3.6"));
        listing("idna", wheel("idna-3.7-py3-none-any.whl", IDNA, ">=3.5,<3.12"));
        String pipfile = """
          [[source]]
          url = "https://pypi.org/simple"
          verify_ssl = true
          name = "pypi"

          [packages]
          requests = ">=2.31.0"

          [requires]
          python_version = "3.12"
          """;

        Result result = PipenvLockEngine.regenerate(pipfile, baseLock(), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure()).isNotNull();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.PIN_EXCLUDED_BY_PYTHON);
        assertThat(result.getFailure().getPackageName()).isEqualTo("idna");
        assertThat(result.getErrorMessage()).contains("idna==3.7");
    }

    @Test
    void removalDropsTopLevelEntryAndRetainsTransitiveOrphans() {
        String pipfile = """
          [[source]]
          url = "https://pypi.org/simple"
          verify_ssl = true
          name = "pypi"

          [packages]

          [requires]
          python_version = "3.11"
          """;

        Result result = PipenvLockEngine.regenerate(pipfile, baseLock(), ctx);

        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDetail()).contains("ORPHANS_RETAINED");
        assertThat(result.getLockFileContent()).isEqualTo("""
          {
              "_meta": {
                  "hash": {
                      "sha256": "%s"
                  },
                  "pipfile-spec": 6,
                  "requires": {
                      "python_version": "3.11"
                  },
                  "sources": [
                      {
                          "name": "pypi",
                          "url": "https://pypi.org/simple",
                          "verify_ssl": true
                      }
                  ]
              },
              "default": {
                  "certifi": {
                      "hashes": [
                          "sha256:%s"
                      ],
                      "markers": "python_version >= '3.6'",
                      "version": "==2024.2.2"
                  },
                  "idna": {
                      "hashes": [
                          "sha256:%s"
                      ],
                      "markers": "python_version >= '3.5'",
                      "version": "==3.7"
                  }
              },
              "develop": {}
          }
          """.formatted(HASH_REMOVAL, CERTIFI, IDNA));
    }

    @Test
    void crlfNewlineStyleOfExistingLockIsPreserved() {
        String pipfile = """
          [[source]]
          url = "https://pypi.org/simple"
          verify_ssl = true
          name = "pypi"

          [packages]

          [requires]
          python_version = "3.11"
          """;

        Result result = PipenvLockEngine.regenerate(pipfile, baseLock().replace("\n", "\r\n"), ctx);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getLockFileContent()).contains("\r\n");
        assertThat(result.getLockFileContent().replace("\r\n", "\n")).doesNotContain("\r");
    }

    @Test
    void additionRequiresResolution() {
        String pipfile = """
          [[source]]
          url = "https://pypi.org/simple"
          verify_ssl = true
          name = "pypi"

          [packages]
          requests = ">=2.31.0"
          flask = "*"

          [requires]
          python_version = "3.11"
          """;

        Result result = PipenvLockEngine.regenerate(pipfile, baseLock(), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getPackageName()).isEqualTo("flask");
    }

    @Test
    void untouchedVcsEntryPassesThrough() {
        String pipfile = """
          [[source]]
          url = "https://pypi.org/simple"
          verify_ssl = true
          name = "pypi"

          [packages]
          requests = ">=2.31.0"
          mylib = {git = "https://github.com/example/mylib.git", ref = "main"}

          [requires]
          python_version = "3.11"
          """;
        String lock = baseLock().replace("""
                  "requests": {
          """, """
                  "mylib": {
                      "git": "https://github.com/example/mylib.git",
                      "ref": "abc1234567890abc1234567890abc1234567890a"
                  },
                  "requests": {
          """);

        Result result = PipenvLockEngine.regenerate(pipfile, lock, ctx);

        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getLockFileContent()).contains("\"ref\": \"abc1234567890abc1234567890abc1234567890a\"");
    }

    @Test
    void reTargetingVersionedEntryToVcsIsUnsupported() {
        String pipfile = """
          [[source]]
          url = "https://pypi.org/simple"
          verify_ssl = true
          name = "pypi"

          [packages]
          requests = {git = "https://github.com/psf/requests.git", ref = "main"}

          [requires]
          python_version = "3.11"
          """;

        Result result = PipenvLockEngine.regenerate(pipfile, baseLock(), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.UNSUPPORTED_ENTRY_TYPE);
        assertThat(result.getFailure().getPackageName()).isEqualTo("requests");
    }

    @Test
    void noSatisfyingVersionIsAResolutionConflict() {
        stubRequestsBaseline();
        String pipfile = """
          [[source]]
          url = "https://pypi.org/simple"
          verify_ssl = true
          name = "pypi"

          [packages]
          requests = "==9.9.9"

          [requires]
          python_version = "3.11"
          """;

        Result result = PipenvLockEngine.regenerate(pipfile, baseLock(), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_CONFLICT);
        assertThat(result.getFailure().getPackageName()).isEqualTo("requests");
        assertThat(result.getFailure().getIndexUrl()).isEqualTo(server.url("/simple").toString());
    }

    @Test
    void newRequirementOutsideTheLockRequiresResolution() {
        listing("requests",
          wheel("requests-2.31.0-py3-none-any.whl", WHEEL_2310, ">=3.7"),
          wheel("requests-2.33.0-py3-none-any.whl", "3".repeat(64), ">=3.8"));
        metadata("requests-2.33.0-py3-none-any.whl", """
          Metadata-Version: 2.1
          Name: requests
          Version: 2.33.0
          Requires-Python: >=3.8
          Requires-Dist: brand-new-package>=1.0
          """);
        String pipfile = """
          [[source]]
          url = "https://pypi.org/simple"
          verify_ssl = true
          name = "pypi"

          [packages]
          requests = ">=2.33.0"

          [requires]
          python_version = "3.11"
          """;

        Result result = PipenvLockEngine.regenerate(pipfile, baseLock(), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getPackageName()).isEqualTo("requests");
        assertThat(result.getFailure().getDetail()).contains("brand-new-package");
    }

    @Test
    void sdistOnlyReleaseWithoutStaticMetadataFails() throws IOException {
        byte[] sdistZip = zip("requests-2.34.0/PKG-INFO", """
          Metadata-Version: 2.1
          Name: requests
          Version: 2.34.0
          """);
        listing("requests",
          wheel("requests-2.31.0-py3-none-any.whl", WHEEL_2310, ">=3.7"),
          sdist("requests-2.34.0.zip", "4".repeat(64), ">=3.8"));
        routes.put("/packages/requests-2.34.0.zip", () -> {
            Buffer buffer = new Buffer();
            buffer.write(sdistZip);
            return new MockResponse().setBody(buffer);
        });
        String pipfile = """
          [[source]]
          url = "https://pypi.org/simple"
          verify_ssl = true
          name = "pypi"

          [packages]
          requests = "==2.34.0"

          [requires]
          python_version = "3.11"
          """;

        Result result = PipenvLockEngine.regenerate(pipfile, baseLock(), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.DYNAMIC_SDIST_METADATA);
        assertThat(result.getFailure().getPackageName()).isEqualTo("requests");
    }

    @Test
    void fileWithoutDigestFailsHashCollection() {
        listing("requests",
          wheel("requests-2.31.0-py3-none-any.whl", WHEEL_2310, ">=3.7"),
          wheelWithoutHash("requests-2.35.0-py3-none-any.whl", ">=3.8"));
        metadata("requests-2.35.0-py3-none-any.whl", """
          Metadata-Version: 2.1
          Name: requests
          Version: 2.35.0
          """);
        String pipfile = """
          [[source]]
          url = "https://pypi.org/simple"
          verify_ssl = true
          name = "pypi"

          [packages]
          requests = "==2.35.0"

          [requires]
          python_version = "3.11"
          """;

        Result result = PipenvLockEngine.regenerate(pipfile, baseLock(), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.HASH_UNAVAILABLE);
        assertThat(result.getFailure().getPackageName()).isEqualTo("requests");
    }

    @Test
    void downloadsAndHashesFilesWhenListingLacksDigests() throws Exception {
        byte[] wheelBytes = "wheel-bytes-2.32.4".getBytes(StandardCharsets.UTF_8);
        // fragment-less PEP 503 HTML listing: no #sha256= on the 2.32.4 anchor
        routes.put("/simple/requests/", () -> new MockResponse()
          .setHeader("Content-Type", "text/html")
          .setBody("""
            <html><body>
            <a href="/packages/requests-2.31.0-py3-none-any.whl#sha256=%s" data-requires-python="&gt;=3.7">requests-2.31.0-py3-none-any.whl</a>
            <a href="/packages/requests-2.32.4-py3-none-any.whl" data-requires-python="&gt;=3.8" data-core-metadata="true">requests-2.32.4-py3-none-any.whl</a>
            </body></html>
            """.formatted(WHEEL_2310)));
        metadata("requests-2.32.4-py3-none-any.whl", """
          Metadata-Version: 2.1
          Name: requests
          Version: 2.32.4
          Requires-Python: >=3.8
          Requires-Dist: certifi>=2017.4.17
          Requires-Dist: idna<4,>=2.5
          """);
        routes.put("/packages/requests-2.32.4-py3-none-any.whl", () -> {
            Buffer buffer = new Buffer();
            buffer.write(wheelBytes);
            return new MockResponse().setBody(buffer);
        });
        String pipfile = """
          [[source]]
          url = "https://pypi.org/simple"
          verify_ssl = true
          name = "pypi"

          [packages]
          requests = ">=2.32.0"

          [requires]
          python_version = "3.11"
          """;

        Result result = PipenvLockEngine.regenerate(pipfile, baseLock(), ctx);

        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getLockFileContent()).contains("\"version\": \"==2.32.4\"");
        assertThat(result.getLockFileContent()).contains("sha256:" + sha256Hex(wheelBytes));
    }

    @Test
    void bz2OnlyReleaseFailsStructured() {
        listing("requests",
          wheel("requests-2.31.0-py3-none-any.whl", WHEEL_2310, ">=3.7"),
          sdist("requests-2.36.0.tar.bz2", "6".repeat(64), ">=3.8"));
        String pipfile = """
          [[source]]
          url = "https://pypi.org/simple"
          verify_ssl = true
          name = "pypi"

          [packages]
          requests = "==2.36.0"

          [requires]
          python_version = "3.11"
          """;

        Result result = PipenvLockEngine.regenerate(pipfile, baseLock(), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_CONFLICT);
        assertThat(result.getFailure().getPackageName()).isEqualTo("requests");
    }

    @Test
    void extrasAreEmittedSorted() {
        stubRequestsBaseline();
        String pipfile = """
          [[source]]
          url = "https://pypi.org/simple"
          verify_ssl = true
          name = "pypi"

          [packages]
          requests = {version = ">=2.32.0", extras = ["socks", "asyncio"]}

          [requires]
          python_version = "3.11"
          """;

        Result result = PipenvLockEngine.regenerate(pipfile, baseLock(), ctx);

        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getLockFileContent()).contains(
          "            \"extras\": [\n" +
            "                \"asyncio\",\n" +
            "                \"socks\"\n" +
            "            ],");
    }

    @Test
    void packageMissingFromIndexIsStructured() {
        // no route for /simple/requests/ at all: the index 404s the package page
        String pipfile = """
          [[source]]
          url = "https://pypi.org/simple"
          verify_ssl = true
          name = "pypi"

          [packages]
          requests = ">=2.32.0"

          [requires]
          python_version = "3.11"
          """;

        Result result = PipenvLockEngine.regenerate(pipfile, baseLock(), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.PACKAGE_NOT_FOUND);
        assertThat(result.getFailure().getPackageName()).isEqualTo("requests");
        assertThat(result.getFailure().getIndexUrl()).isEqualTo(server.url("/simple").toString());
    }

    @Test
    void malformedPipfileIsStructured() {
        Result result = PipenvLockEngine.regenerate("[packages\nrequests \"*\"\n", baseLock(), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.MALFORMED_MANIFEST);
    }

    @Test
    void authFailureIsStructured() {
        routes.put("/simple/requests/", () -> new MockResponse().setResponseCode(401));
        String pipfile = """
          [[source]]
          url = "https://pypi.org/simple"
          verify_ssl = true
          name = "pypi"

          [packages]
          requests = ">=2.32.0"

          [requires]
          python_version = "3.11"
          """;

        Result result = PipenvLockEngine.regenerate(pipfile, baseLock(), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.AUTH_FAILED);
        assertThat(result.getFailure().getIndexUrl()).isEqualTo(server.url("/simple").toString());
    }

    @Test
    void unreachableIndexIsStructured() {
        routes.put("/simple/requests/", () -> new MockResponse().setResponseCode(500));
        String pipfile = """
          [[source]]
          url = "https://pypi.org/simple"
          verify_ssl = true
          name = "pypi"

          [packages]
          requests = ">=2.32.0"

          [requires]
          python_version = "3.11"
          """;

        Result result = PipenvLockEngine.regenerate(pipfile, baseLock(), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.INDEX_UNREACHABLE);
    }

    @Test
    void anySingleFailureAbortsTheWholeRegeneration() {
        stubRequestsBaseline();
        listing("zzz-package", wheel("zzz_package-1.0.0-py3-none-any.whl", "5".repeat(64), ">=3.7"));
        String pipfile = """
          [[source]]
          url = "https://pypi.org/simple"
          verify_ssl = true
          name = "pypi"

          [packages]
          requests = ">=2.32.0"
          zzz-package = "==9.9.9"

          [requires]
          python_version = "3.11"
          """;
        String lock = baseLock().replace("""
                  "certifi": {
          """, """
                  "zzz-package": {
                      "hashes": [
                          "sha256:%s"
                      ],
                      "index": "pypi",
                      "version": "==1.0.0"
                  },
                  "certifi": {
          """.formatted("5".repeat(64)));

        Result result = PipenvLockEngine.regenerate(pipfile, lock, ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getLockFileContent()).isNull();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_CONFLICT);
        assertThat(result.getFailure().getPackageName()).isEqualTo("zzz-package");
    }

    /**
     * Recorded end-to-end cross-check: the before lock was produced by a real
     * {@code pipenv lock} (2026.6.2) of {@code six = "==1.16.0"}, the listing data and
     * PEP 658 sidecar mirror pypi.org's real responses, and the expected output is
     * byte-identical to what the same pipenv produced after editing the Pipfile to
     * {@code six = "==1.17.0"} ({@code pipenv verify} green).
     */
    @Test
    void agreesByteForByteWithRecordedRealPipenvUpgrade() {
        listing("six",
          wheel("six-1.16.0-py2.py3-none-any.whl",
            "8abb2f1d86890a2dfb989f9a77cfcfd3e47c2a354b01111771326f8aa26e0254", ">=2.7, !=3.0.*, !=3.1.*, !=3.2.*"),
          sdist("six-1.16.0.tar.gz",
            "1e61c37477a1626458e36f7b1d82aa5c9b094fa4802892072e49de9c60c4c926", ">=2.7, !=3.0.*, !=3.1.*, !=3.2.*"),
          wheel("six-1.17.0-py2.py3-none-any.whl",
            "4721f391ed90541fddacab5acf947aa0d3dc7d27b2e1e8eda2be8970586c3274", "!=3.0.*,!=3.1.*,!=3.2.*,>=2.7"),
          sdist("six-1.17.0.tar.gz",
            "ff70335d468e7eb6ec65b95b99d3a2836546063f63acc5171de367e834932a81", "!=3.0.*,!=3.1.*,!=3.2.*,>=2.7"));
        metadata("six-1.17.0-py2.py3-none-any.whl", """
          Metadata-Version: 2.1
          Name: six
          Version: 1.17.0
          Requires-Python: >=2.7, !=3.0.*, !=3.1.*, !=3.2.*
          """);
        String pipfile = """
          [[source]]
          url = "https://pypi.org/simple"
          verify_ssl = true
          name = "pypi"

          [packages]
          six = "==1.17.0"
          """;
        String oldLock = """
          {
              "_meta": {
                  "hash": {
                      "sha256": "f162d2e398108da068327e701ee9472102db75565b86c0415420968db20ef679"
                  },
                  "pipfile-spec": 6,
                  "requires": {},
                  "sources": [
                      {
                          "name": "pypi",
                          "url": "https://pypi.org/simple",
                          "verify_ssl": true
                      }
                  ]
              },
              "default": {
                  "six": {
                      "hashes": [
                          "sha256:1e61c37477a1626458e36f7b1d82aa5c9b094fa4802892072e49de9c60c4c926",
                          "sha256:8abb2f1d86890a2dfb989f9a77cfcfd3e47c2a354b01111771326f8aa26e0254"
                      ],
                      "index": "pypi",
                      "markers": "python_version >= '2.7' and python_version not in '3.0, 3.1, 3.2'",
                      "version": "==1.16.0"
                  }
              },
              "develop": {}
          }
          """;

        Result result = PipenvLockEngine.regenerate(pipfile, oldLock, ctx);

        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo("""
          {
              "_meta": {
                  "hash": {
                      "sha256": "84c2e21771457e7e60048cd7d81874d5f8210db4c93d03494250d6e942ed338c"
                  },
                  "pipfile-spec": 6,
                  "requires": {},
                  "sources": [
                      {
                          "name": "pypi",
                          "url": "https://pypi.org/simple",
                          "verify_ssl": true
                      }
                  ]
              },
              "default": {
                  "six": {
                      "hashes": [
                          "sha256:4721f391ed90541fddacab5acf947aa0d3dc7d27b2e1e8eda2be8970586c3274",
                          "sha256:ff70335d468e7eb6ec65b95b99d3a2836546063f63acc5171de367e834932a81"
                      ],
                      "index": "pypi",
                      "markers": "python_version >= '2.7' and python_version not in '3.0, 3.1, 3.2'",
                      "version": "==1.17.0"
                  }
              },
              "develop": {}
          }
          """);
    }

    @Test
    void removalKeepsEntryStillRequiredByRetainedTopLevel() {
        listing("requests",
          wheel("requests-2.31.0-py3-none-any.whl", WHEEL_2310, ">=3.7"),
          sdist("requests-2.31.0.tar.gz", SDIST_2310, ">=3.7"));
        metadata("requests-2.31.0-py3-none-any.whl", """
          Metadata-Version: 2.1
          Name: requests
          Version: 2.31.0
          Requires-Python: >=3.7
          Requires-Dist: certifi>=2017.4.17
          Requires-Dist: idna<4,>=2.5
          """);
        String pipfile = """
          [[source]]
          url = "https://pypi.org/simple"
          verify_ssl = true
          name = "pypi"

          [packages]
          requests = ">=2.31.0"

          [requires]
          python_version = "3.11"
          """;
        // certifi is top-level in the old lock but removed from the Pipfile
        String lock = baseLock().replace(
          "\"markers\": \"python_version >= '3.6'\"",
          "\"index\": \"pypi\",\n            \"markers\": \"python_version >= '3.6'\"");

        Result result = PipenvLockEngine.regenerate(pipfile, lock, ctx);

        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDetail()).contains("DEMOTED_TO_TRANSITIVE").contains("certifi").contains("requests");
        assertThat(result.getLockFileContent()).contains(
          "        \"certifi\": {\n" +
            "            \"hashes\": [\n" +
            "                \"sha256:" + CERTIFI + "\"\n" +
            "            ],\n" +
            "            \"markers\": \"python_version >= '3.6'\",\n" +
            "            \"version\": \"==2024.2.2\"\n" +
            "        },");
    }

    @Test
    void removalRetainsEntryWhenCheckerMetadataUnavailable() {
        // no routes at all: requests' requirements cannot be fetched to prove certifi unreferenced
        String pipfile = """
          [[source]]
          url = "https://pypi.org/simple"
          verify_ssl = true
          name = "pypi"

          [packages]
          requests = ">=2.31.0"

          [requires]
          python_version = "3.11"
          """;
        String lock = baseLock().replace(
          "\"markers\": \"python_version >= '3.6'\"",
          "\"index\": \"pypi\",\n            \"markers\": \"python_version >= '3.6'\"");

        Result result = PipenvLockEngine.regenerate(pipfile, lock, ctx);

        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDetail()).contains("could not be verified").contains("requests");
        assertThat(result.getLockFileContent()).contains(
          "        \"certifi\": {\n" +
            "            \"hashes\": [\n" +
            "                \"sha256:" + CERTIFI + "\"\n" +
            "            ],\n" +
            "            \"markers\": \"python_version >= '3.6'\",\n" +
            "            \"version\": \"==2024.2.2\"\n" +
            "        },");
    }

    @Test
    void attributeOnlyIndexRetargetRewritesEntryWithoutRefetch() {
        // no routes: index retargeting at an unchanged pinned version needs no network
        String pipfile = """
          [[source]]
          url = "https://pypi.org/simple"
          verify_ssl = true
          name = "pypi"

          [[source]]
          url = "https://corp.example.com/simple"
          verify_ssl = true
          name = "corp"

          [packages]
          requests = {version = ">=2.31.0", index = "corp"}

          [requires]
          python_version = "3.11"
          """;

        Result result = PipenvLockEngine.regenerate(pipfile, baseLock(), ctx);

        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getLockFileContent()).contains(
          "        \"requests\": {\n" +
            "            \"hashes\": [\n" +
            "                \"sha256:" + WHEEL_2310 + "\",\n" +
            "                \"sha256:" + SDIST_2310 + "\"\n" +
            "            ],\n" +
            "            \"index\": \"corp\",\n" +
            "            \"markers\": \"python_version >= '3.7'\",\n" +
            "            \"version\": \"==2.31.0\"\n" +
            "        }");
    }

    @Test
    void markersKeyChangeRewritesEntryKeepingPinnedVersion() {
        // offline: marker recomposition at an unchanged pinned version needs no network.
        // Composed form recorded from a real `pipenv lock` (2026.6.2): clauses sorted, no
        // Requires-Python marker, and no "index" for an inline table without an index key.
        String pipfile = """
          [[source]]
          url = "https://pypi.org/simple"
          verify_ssl = true
          name = "pypi"

          [packages]
          requests = {version = ">=2.31.0", markers = "python_version >= '3.8'", sys_platform = "== 'win32'"}

          [requires]
          python_version = "3.11"
          """;

        Result result = PipenvLockEngine.regenerate(pipfile, baseLock(), ctx);

        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getLockFileContent()).contains(
          "        \"requests\": {\n" +
            "            \"hashes\": [\n" +
            "                \"sha256:" + WHEEL_2310 + "\",\n" +
            "                \"sha256:" + SDIST_2310 + "\"\n" +
            "            ],\n" +
            "            \"markers\": \"python_version >= '3.8' and sys_platform == 'win32'\",\n" +
            "            \"version\": \"==2.31.0\"\n" +
            "        }");
    }

    @Test
    void unchangedInlineTableEntryRoundTripsByteForByte() {
        // Expected _meta.hash recorded from the python3 oracle for this Pipfile
        String pipfile = """
          [[source]]
          url = "https://pypi.org/simple"
          verify_ssl = true
          name = "pypi"

          [packages]
          requests = {version = ">=2.31.0", markers = "python_version >= '3.8'", sys_platform = "== 'win32'"}

          [requires]
          python_version = "3.11"
          """;
        String lock = baseLock()
          .replace("0".repeat(64), "c6b4651efeb5d989adda35c93693aa99e9d3eab324f457bcf0b8f3cfe902b788")
          .replace("            \"index\": \"pypi\",\n", "")
          .replace("\"markers\": \"python_version >= '3.7'\"",
            "\"markers\": \"python_version >= '3.8' and sys_platform == 'win32'\"");

        Result result = PipenvLockEngine.regenerate(pipfile, lock, ctx);

        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDetail()).isNull();
        assertThat(result.getLockFileContent()).isEqualTo(lock);
    }

    @Test
    void untouchedManifestAndLockRoundTripByteForByte() {
        // the recorded real-pipenv "after" state from agreesByteForByteWithRecordedRealPipenvUpgrade
        String pipfile = """
          [[source]]
          url = "https://pypi.org/simple"
          verify_ssl = true
          name = "pypi"

          [packages]
          six = "==1.17.0"
          """;
        String lock = """
          {
              "_meta": {
                  "hash": {
                      "sha256": "84c2e21771457e7e60048cd7d81874d5f8210db4c93d03494250d6e942ed338c"
                  },
                  "pipfile-spec": 6,
                  "requires": {},
                  "sources": [
                      {
                          "name": "pypi",
                          "url": "https://pypi.org/simple",
                          "verify_ssl": true
                      }
                  ]
              },
              "default": {
                  "six": {
                      "hashes": [
                          "sha256:4721f391ed90541fddacab5acf947aa0d3dc7d27b2e1e8eda2be8970586c3274",
                          "sha256:ff70335d468e7eb6ec65b95b99d3a2836546063f63acc5171de367e834932a81"
                      ],
                      "index": "pypi",
                      "markers": "python_version >= '2.7' and python_version not in '3.0, 3.1, 3.2'",
                      "version": "==1.17.0"
                  }
              },
              "develop": {}
          }
          """;

        Result result = PipenvLockEngine.regenerate(pipfile, lock, ctx);

        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(lock);
    }

    @Test
    void extrasAddedTriggersClosureProof() {
        listing("requests",
          wheel("requests-2.31.0-py3-none-any.whl", WHEEL_2310, ">=3.7"),
          sdist("requests-2.31.0.tar.gz", SDIST_2310, ">=3.7"));
        metadata("requests-2.31.0-py3-none-any.whl", """
          Metadata-Version: 2.1
          Name: requests
          Version: 2.31.0
          Requires-Python: >=3.7
          Requires-Dist: certifi>=2017.4.17
          Requires-Dist: idna<4,>=2.5
          Requires-Dist: pysocks!=1.5.7,>=1.5.6; extra == 'socks'
          """);
        String pipfile = """
          [[source]]
          url = "https://pypi.org/simple"
          verify_ssl = true
          name = "pypi"

          [packages]
          requests = {version = ">=2.31.0", extras = ["socks"]}

          [requires]
          python_version = "3.11"
          """;

        Result result = PipenvLockEngine.regenerate(pipfile, baseLock(), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getPackageName()).isEqualTo("requests");
        assertThat(result.getFailure().getDetail()).contains("pysocks");
    }

    @Test
    void extrasAddedWithSatisfiedClosureRewritesEntry() {
        listing("requests",
          wheel("requests-2.31.0-py3-none-any.whl", WHEEL_2310, ">=3.7"),
          sdist("requests-2.31.0.tar.gz", SDIST_2310, ">=3.7"));
        metadata("requests-2.31.0-py3-none-any.whl", """
          Metadata-Version: 2.1
          Name: requests
          Version: 2.31.0
          Requires-Python: >=3.7
          Requires-Dist: certifi>=2017.4.17
          Requires-Dist: idna<4; extra == 'socks'
          """);
        String pipfile = """
          [[source]]
          url = "https://pypi.org/simple"
          verify_ssl = true
          name = "pypi"

          [packages]
          requests = {version = ">=2.31.0", extras = ["socks"]}

          [requires]
          python_version = "3.11"
          """;

        Result result = PipenvLockEngine.regenerate(pipfile, baseLock(), ctx);

        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getLockFileContent()).contains(
          "        \"requests\": {\n" +
            "            \"extras\": [\n" +
            "                \"socks\"\n" +
            "            ],\n" +
            "            \"hashes\": [\n" +
            "                \"sha256:" + WHEEL_2310 + "\",\n" +
            "                \"sha256:" + SDIST_2310 + "\"\n" +
            "            ],\n" +
            "            \"markers\": \"python_version >= '3.7'\",\n" +
            "            \"version\": \"==2.31.0\"\n" +
            "        }");
    }

    @Test
    void versionChangeRetargetedToSecondSourceFetchesFromItAndRewritesIndex() {
        PythonExecutionContextView.view(ctx).setPackageIndexes(List.of(
          new PythonPackageIndex("pypi", server.url("/simple").toString(), true, null, null, false),
          new PythonPackageIndex("corp", server.url("/corp/simple").toString(), true, null, null, false)));
        // requests is only published on corp; using pypi would 404
        routes.put("/corp/simple/requests/", () -> new MockResponse()
          .setHeader("Content-Type", "application/vnd.pypi.simple.v1+json")
          .setBody("{\"files\": [" + String.join(", ",
            wheel("requests-2.31.0-py3-none-any.whl", WHEEL_2310, ">=3.7"),
            wheel("requests-2.32.4-py3-none-any.whl", WHEEL_2324, ">=3.8")) + "]}"));
        metadata("requests-2.32.4-py3-none-any.whl", """
          Metadata-Version: 2.1
          Name: requests
          Version: 2.32.4
          Requires-Python: >=3.8
          Requires-Dist: certifi>=2017.4.17
          Requires-Dist: idna<4,>=2.5
          """);
        String pipfile = """
          [[source]]
          url = "https://pypi.org/simple"
          verify_ssl = true
          name = "pypi"

          [[source]]
          url = "https://corp.example.com/simple"
          verify_ssl = true
          name = "corp"

          [packages]
          requests = {version = ">=2.32.0", index = "corp"}

          [requires]
          python_version = "3.11"
          """;

        Result result = PipenvLockEngine.regenerate(pipfile, baseLock(), ctx);

        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getLockFileContent()).contains("\"index\": \"corp\"");
        assertThat(result.getLockFileContent()).contains("\"version\": \"==2.32.4\"");
    }

    @Test
    void markerCompositionMatchesPipenvSortedClauses() {
        // golden: real `pipenv lock` (2026.6.2) sorts the normalized clause strings and
        // omits the Requires-Python-derived marker when the entry declares its own
        stubRequestsBaseline();
        String pipfile = """
          [[source]]
          url = "https://pypi.org/simple"
          verify_ssl = true
          name = "pypi"

          [packages]
          requests = {version = ">=2.32.0", markers = "sys_platform == 'win32'", platform_machine = "!= 'wasm32'"}

          [requires]
          python_version = "3.11"
          """;

        Result result = PipenvLockEngine.regenerate(pipfile, baseLock(), ctx);

        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getLockFileContent()).contains(
          "\"markers\": \"platform_machine != 'wasm32' and sys_platform == 'win32'\"");
        assertThat(result.getLockFileContent()).contains("\"version\": \"==2.32.4\"");
    }

    @Test
    void orMarkersComposeUnparenthesizedMatchingPipenv() {
        // golden: real `pipenv lock` (2026.6.2) joins an "or" marker without parentheses,
        // changing its precedence; the engine reproduces pipenv byte-for-byte
        stubRequestsBaseline();
        String pipfile = """
          [[source]]
          url = "https://pypi.org/simple"
          verify_ssl = true
          name = "pypi"

          [packages]
          requests = {version = ">=2.32.0", markers = "sys_platform == 'win32' or os_name == 'nt'", platform_machine = "!= 'wasm32'"}

          [requires]
          python_version = "3.11"
          """;

        Result result = PipenvLockEngine.regenerate(pipfile, baseLock(), ctx);

        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getLockFileContent()).contains(
          "\"markers\": \"platform_machine != 'wasm32' and sys_platform == 'win32' or os_name == 'nt'\"");
    }

    @Test
    void requiresBumpReportsMissingPinnedPackageStructured() {
        listing("requests",
          wheel("requests-2.31.0-py3-none-any.whl", WHEEL_2310, ">=3.7"),
          sdist("requests-2.31.0.tar.gz", SDIST_2310, ">=3.7"));
        listing("certifi", wheel("certifi-2024.2.2-py3-none-any.whl", CERTIFI, ">=3.6"));
        // idna deliberately unstubbed: its listing 404s during the requires bump
        String pipfile = """
          [[source]]
          url = "https://pypi.org/simple"
          verify_ssl = true
          name = "pypi"

          [packages]
          requests = ">=2.31.0"

          [requires]
          python_version = "3.12"
          """;

        Result result = PipenvLockEngine.regenerate(pipfile, baseLock(), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.PACKAGE_NOT_FOUND);
        assertThat(result.getFailure().getPackageName()).isEqualTo("idna");
        assertThat(result.getFailure().getIndexUrl()).isEqualTo(server.url("/simple").toString());
    }

    @Test
    void malformedLockIsStructured() {
        Result result = PipenvLockEngine.regenerate("[packages]\n", "{not json", ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.MALFORMED_LOCK);
    }

    @Test
    void missingLockRequiresFullResolution() {
        Result result = LockFileRegeneration.PIPENV.regenerate("[packages]\nrequests = \"*\"\n", null, ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
    }

    private static String sha256Hex(byte[] bytes) throws Exception {
        StringBuilder hex = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(bytes)) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    private static byte[] zip(String entryName, String content) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry(entryName));
            zip.write(content.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return out.toByteArray();
    }
}
