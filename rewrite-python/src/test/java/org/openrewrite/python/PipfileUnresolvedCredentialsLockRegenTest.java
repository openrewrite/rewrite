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
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.json.Assertions.json;
import static org.openrewrite.python.Assertions.pipfile;

/**
 * A Pipfile {@code [[source]]} whose URL carries credentials as {@code ${VAR}} placeholders is
 * usually locked where the variables are unset (e.g. a hosted recipe run). As pipenv would,
 * regeneration still sends the credentials as written, so a proxy that authenticates on its own
 * can replace them. When the index rejects them, the failure names the placeholder that could not
 * be resolved.
 */
class PipfileUnresolvedCredentialsLockRegenTest implements RewriteTest {

    // Deliberately a variable no environment sets, so Environment.SYSTEM leaves it unresolved
    private static final String UNSET_TOKEN = "${REWRITE_TEST_UNSET_INDEX_TOKEN}";

    private static final String WHEEL_2324 = "a".repeat(64);
    private static final String SDIST_2324 = "b".repeat(64);
    private static final String WHEEL_2310 = "c".repeat(64);
    private static final String SDIST_2310 = "d".repeat(64);
    private static final String CERTIFI = "e".repeat(64);

    MockWebServer server;
    ExecutionContext ctx;
    final Map<String, MockResponse> routes = new HashMap<>();
    final List<RecordedRequest> requests = new CopyOnWriteArrayList<>();
    boolean rejectAll;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                requests.add(request);
                if (rejectAll) {
                    return new MockResponse().setResponseCode(401);
                }
                MockResponse response = routes.get(request.getPath());
                return response != null ? response : new MockResponse().setResponseCode(404);
            }
        });
        server.start();
        ctx = new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });

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
        requests.clear();
    }

    @Test
    @Timeout(120)
    void unresolvedCredentialsAreSentAsWritten() {
        String basic = "Basic " + Base64.getEncoder().encodeToString((UNSET_TOKEN + ":").getBytes(StandardCharsets.UTF_8));
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("requests", ">=2.32.0", null, null))
            .executionContext(ctx)
            .afterRecipe(run -> assertThat(requests)
              .filteredOn(r -> r.getPath() != null && r.getPath().startsWith("/simple/"))
              .as("every index request carries the credentials as written, as pip would send them")
              .isNotEmpty()
              .allSatisfy(r -> assertThat(r.getHeader("Authorization")).isEqualTo(basic))),
          pipfile(pipfileWith(">=2.28.0"), pipfileWith(">=2.32.0")),
          json(lockBefore(),
            spec -> spec.path("Pipfile.lock").noTrim().after(actual -> {
                assertThat(actual).contains("\"version\": \"==2.32.4\"");
                return actual;
            }))
        );
    }

    @Test
    @Timeout(120)
    void rejectedRequestRecordsUnresolvedPlaceholder() {
        rejectAll = true;
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("requests", ">=2.32.0", null, null))
            .executionContext(ctx)
            .dataTable(PythonLockRegenerationFailures.Row.class, rows -> {
                assertThat(rows).hasSize(1);
                assertThat(rows.get(0).getSourcePath()).isEqualTo("Pipfile");
                assertThat(rows.get(0).getReason()).isEqualTo("AUTH_FAILED");
                assertThat(rows.get(0).getDetail())
                  .contains("HTTP 401")
                  .contains(UNSET_TOKEN);
            }),
          pipfile(pipfileWith(">=2.28.0"),
            s -> s.after(actual -> actual)
              .afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(Markup.Warn.class))
                .as("manifest should carry the lock-regeneration-failure warning")
                .isPresent())),
          json(lockBefore(),
            spec -> spec.path("Pipfile.lock")
              .after(actual -> {
                  assertThat(actual).as("the lock is left untouched").contains("\"version\": \"==2.31.0\"");
                  return actual;
              }))
        );
    }

    private String indexUrl() {
        return "http://" + UNSET_TOKEN + "@" + server.getHostName() + ":" + server.getPort() + "/simple";
    }

    private String pipfileWith(String requestsConstraint) {
        return """
          [[source]]
          name = "corp"
          url = "%s"
          verify_ssl = true

          [packages]
          requests = "%s"
          """.formatted(indexUrl(), requestsConstraint);
    }

    private String lockBefore() {
        return """
          {
              "_meta": {
                  "hash": {
                      "sha256": "0000000000000000000000000000000000000000000000000000000000000000"
                  },
                  "pipfile-spec": 6,
                  "requires": {},
                  "sources": [
                      {
                          "name": "corp",
                          "url": "%s",
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
                      "index": "corp",
                      "markers": "python_version >= '3.7'",
                      "version": "==2.31.0"
                  }
              },
              "develop": {}
          }
          """.formatted(indexUrl(), CERTIFI, WHEEL_2310, SDIST_2310);
    }
}
