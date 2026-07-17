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
package org.openrewrite.python;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.marker.Markup;
import org.openrewrite.python.table.PythonLockRegenerationFailures;
import org.openrewrite.test.RewriteTest;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.json.Assertions.json;
import static org.openrewrite.python.Assertions.pipfile;

/**
 * End-to-end recipe test of native Pipfile.lock regeneration against a stub package
 * index. The expected {@code _meta.hash} was recorded from the repo's python3 oracle
 * ({@code src/test/resources/pipfilelock/oracle.py hash --sources-json ...}).
 */
class UpgradeDependencyVersionPipfileLockTest implements RewriteTest {

    private static final String WHEEL_2324 = "a".repeat(64);
    private static final String SDIST_2324 = "b".repeat(64);
    private static final String WHEEL_2310 = "c".repeat(64);
    private static final String SDIST_2310 = "d".repeat(64);
    private static final String CERTIFI = "e".repeat(64);

    private static final String HASH_AFTER = "80cda6d1b04efef5989eb4f2d396bdcbbf070bac9c80c86c9ff134e34a0c2097";

    MockWebServer server;
    ExecutionContext ctx;
    final Map<String, MockResponse> routes = new HashMap<>();

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                MockResponse response = routes.get(request.getPath());
                return response != null ? response : new MockResponse().setResponseCode(404);
            }
        });
        server.start();
        ctx = new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });
        PythonExecutionContextView.view(ctx).setPackageIndexes(List.of(
          new PythonPackageIndex("pypi", server.url("/simple").toString(), true, null, null, false)));

        routes.put("/simple/requests/", new MockResponse()
          .setHeader("Content-Type", "application/vnd.pypi.simple.v1+json")
          .setBody(("{\"files\": [" +
            "{\"filename\": \"requests-2.31.0-py3-none-any.whl\", \"url\": \"/packages/requests-2.31.0-py3-none-any.whl\", " +
            "\"hashes\": {\"sha256\": \"%s\"}, \"requires-python\": \">=3.7\", \"core-metadata\": true}, " +
            "{\"filename\": \"requests-2.31.0.tar.gz\", \"url\": \"/packages/requests-2.31.0.tar.gz\", " +
            "\"hashes\": {\"sha256\": \"%s\"}, \"requires-python\": \">=3.7\"}, " +
            "{\"filename\": \"requests-2.32.4-py3-none-any.whl\", \"url\": \"/packages/requests-2.32.4-py3-none-any.whl\", " +
            "\"hashes\": {\"sha256\": \"%s\"}, \"requires-python\": \">=3.8\", \"core-metadata\": true}, " +
            "{\"filename\": \"requests-2.32.4.tar.gz\", \"url\": \"/packages/requests-2.32.4.tar.gz\", " +
            "\"hashes\": {\"sha256\": \"%s\"}, \"requires-python\": \">=3.8\"}" +
            "]}").formatted(WHEEL_2310, SDIST_2310, WHEEL_2324, SDIST_2324)));
        routes.put("/packages/requests-2.32.4-py3-none-any.whl.metadata", new MockResponse().setBody("""
          Metadata-Version: 2.1
          Name: requests
          Version: 2.32.4
          Requires-Python: >=3.8
          Requires-Dist: certifi>=2017.4.17
          """));
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
        routes.clear();
    }

    @Test
    @Timeout(120)
    void upgradesPipfileAndRegeneratesLockNatively() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("requests", ">=2.32.0", null, null))
            .executionContext(ctx),
          pipfile(
            """
              [packages]
              requests = ">=2.28.0"
              """,
            """
              [packages]
              requests = ">=2.32.0"
              """
          ),
          json(
            """
              {
                  "_meta": {
                      "hash": {
                          "sha256": "0000000000000000000000000000000000000000000000000000000000000000"
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
                      "certifi": {
                          "hashes": [
                              "sha256:%s"
                          ],
                          "markers": "python_version >= '3.6'",
                          "version": "==2024.2.2"
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
              """.formatted(CERTIFI, WHEEL_2310, SDIST_2310),
            """
              {
                  "_meta": {
                      "hash": {
                          "sha256": "%s"
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
                      "certifi": {
                          "hashes": [
                              "sha256:%s"
                          ],
                          "markers": "python_version >= '3.6'",
                          "version": "==2024.2.2"
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
              """.formatted(HASH_AFTER, CERTIFI, WHEEL_2324, SDIST_2324),
            spec -> spec.path("Pipfile.lock").noTrim()
          )
        );
    }

    @Test
    @Timeout(120)
    void recordsStructuredFailureInDataTableAndWarns() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("requests", "==9.9.9", null, null))
            .executionContext(ctx)
            .dataTable(PythonLockRegenerationFailures.Row.class, rows -> {
                assertThat(rows).hasSize(1);
                assertThat(rows.get(0).getSourcePath()).isEqualTo("Pipfile");
                assertThat(rows.get(0).getPackageName()).isEqualTo("requests");
                assertThat(rows.get(0).getReason()).isEqualTo("RESOLUTION_CONFLICT");
            }),
          pipfile(
            """
              [packages]
              requests = ">=2.28.0"
              """,
            s -> s.after(actual -> {
                assertThat(actual).contains("requests = \"==9.9.9\"");
                return actual;
            }).afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(Markup.Warn.class))
              .as("manifest should carry the lock-regeneration-failure warning")
              .isPresent())
          ),
          json(
            """
              {
                  "_meta": {
                      "hash": {
                          "sha256": "0000000000000000000000000000000000000000000000000000000000000000"
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
                      "requests": {
                          "hashes": [
                              "sha256:%s"
                          ],
                          "index": "pypi",
                          "version": "==2.31.0"
                      }
                  },
                  "develop": {}
              }
              """.formatted(WHEEL_2310),
            spec -> spec.path("Pipfile.lock")
              .after(actual -> actual)
              .afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(Markup.Warn.class))
                .as("lock file should carry the lock-regeneration-failure warning")
                .isPresent())
          )
        );
    }
}
