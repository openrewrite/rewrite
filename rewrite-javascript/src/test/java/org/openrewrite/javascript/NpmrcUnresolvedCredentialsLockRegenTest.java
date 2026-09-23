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
package org.openrewrite.javascript;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.Npmrc;
import org.openrewrite.javascript.marker.NodeResolutionResult.NpmrcScope;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;
import org.openrewrite.marker.Markup;
import org.openrewrite.test.RewriteTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.dependency;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;
import static org.openrewrite.javascript.Assertions.packageJson;
import static org.openrewrite.javascript.Assertions.packageLock;

/**
 * An LST built where {@code .npmrc} credentials came from environment variables carries the
 * {@code ${VAR}} placeholders, not the secrets. Where the recipe runs, those variables are usually
 * unset, so the credentials cannot be used: regeneration falls back to unauthenticated requests
 * and, when the registry rejects them, reports the placeholder that could not be resolved.
 */
class NpmrcUnresolvedCredentialsLockRegenTest implements RewriteTest {

    // Deliberately a variable no environment sets, so Environment.SYSTEM leaves it unresolved
    private static final String UNSET_TOKEN = "${REWRITE_TEST_UNSET_NPM_TOKEN}";

    private ExecutionContext ctx;
    private final Map<String, String> routes = new HashMap<>();
    private final List<HttpSender.Request> requests = new ArrayList<>();

    private Function<HttpSender.Request, HttpSender.Response> responder = request -> null;

    @BeforeEach
    void setUp() {
        routes.clear();
        requests.clear();
        responder = request -> null;
        HttpSender http = request -> {
            requests.add(request);
            HttpSender.Response custom = responder.apply(request);
            if (custom != null) {
                return custom;
            }
            String body = routes.get(request.getUrl().toString());
            return new HttpSender.Response(body == null ? 404 : 200,
                    new ByteArrayInputStream((body == null ? "" : body).getBytes(StandardCharsets.UTF_8)), () -> {
            });
        };
        ctx = new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });
        HttpSenderExecutionContextView.view(ctx).setHttpSender(http);
    }

    @Test
    void unresolvedCredentialFallsBackToUnauthenticatedRequests() {
        routes.put("https://registry.npmjs.org/is-odd", resource("lock/npm/v3/http/is-odd"));
        routes.put("https://registry.npmjs.org/is-odd/3.0.0", resource("lock/npm/v3/http/is-odd-3.0.0"));
        routes.put("https://registry.npmjs.org/is-odd/3.0.1", resource("lock/npm/v3/http/is-odd-3.0.1"));

        rewriteRun(
                spec -> spec.recipe(new UpgradeDependencyVersion("is-odd", null, "3.0.1")).executionContext(ctx)
                        .afterRecipe(run -> assertThat(requests)
                                .as("no request carries the unexpanded placeholder or any credential")
                                .isNotEmpty()
                                .allSatisfy(r -> assertThat(r.getRequestHeaders()).doesNotContainKey("Authorization"))),
                packageJson(PKG_BEFORE, PKG_BEFORE.replace("\"is-odd\": \"3.0.0\"", "\"is-odd\": \"3.0.1\""),
                        markerWithUnresolvedToken()),
                packageLock(resource("lock/npm/v3/before"), resource("lock/npm/v3/after"), s -> s.noTrim())
        );
    }

    @Test
    void rejectedFallbackRecordsUnresolvedPlaceholder() {
        responder = request -> new HttpSender.Response(401,
                new ByteArrayInputStream("Unauthorized".getBytes(StandardCharsets.UTF_8)), () -> {
        });
        String lock = resource("lock/npm/v3/before");

        rewriteRun(
                spec -> spec.recipe(new UpgradeDependencyVersion("is-odd", null, "3.0.1")).executionContext(ctx)
                        .dataTable(NodeLockRegenerationFailures.Row.class, rows -> {
                            assertThat(rows).hasSize(1);
                            NodeLockRegenerationFailures.Row row = rows.get(0);
                            assertThat(row.getSourcePath()).isEqualTo("package.json");
                            assertThat(row.getReason()).isEqualTo("AUTH_FAILED");
                            assertThat(row.getDetail())
                                    .contains("HTTP 401")
                                    .contains(UNSET_TOKEN);
                        }),
                packageJson(PKG_BEFORE, null, markerWithUnresolvedToken(),
                        s -> s.after(actual -> actual)
                                .afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(Markup.Warn.class))
                                        .as("manifest carries the lock-regen-failure warning").isPresent())),
                packageLock(lock, null, s -> s.after(actual -> {
                    assertThat(actual).as("the lock is left untouched").contains("\"is-odd\": \"3.0.0\"");
                    return actual;
                }))
        );
    }

    private static final String PKG_BEFORE = "{\n" +
            "  \"name\": \"npm-lock-v3\",\n" +
            "  \"version\": \"1.0.0\",\n" +
            "  \"dependencies\": {\n" +
            "    \"is-odd\": \"3.0.0\"\n" +
            "  }\n" +
            "}\n";

    private static NodeResolutionResult markerWithUnresolvedToken() {
        Map<String, String> userNpmrc = new HashMap<>();
        userNpmrc.put("registry", "https://registry.npmjs.org/");
        userNpmrc.put("//registry.npmjs.org/:_authToken", UNSET_TOKEN);
        return nodeResolutionResult(PackageManager.Npm, dependency("is-odd", "3.0.0"))
                .withNpmrcConfigs(singletonList(new Npmrc(NpmrcScope.User, userNpmrc)));
    }

    private static String resource(String path) {
        try (InputStream in = NpmrcUnresolvedCredentialsLockRegenTest.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("missing test resource " + path);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
