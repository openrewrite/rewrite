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
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.test.RewriteTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static java.util.Collections.singletonList;
import static org.openrewrite.javascript.Assertions.dependency;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;
import static org.openrewrite.javascript.Assertions.packageJson;
import static org.openrewrite.javascript.Assertions.packageLock;

class RegistryLockRegenTest implements RewriteTest {

    private ExecutionContext ctx;
    private final Map<String, String> routes = new HashMap<>();

    private Function<HttpSender.Request, HttpSender.Response> responder = request -> null;

    @BeforeEach
    void setUp() {
        routes.clear();
        responder = request -> null;
        HttpSender http = request -> {
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
        NodeExecutionContextView.view(ctx).setRegistries(singletonList(
                new NodeRegistry(null, "https://registry.npmjs.org/", null, null, null, null, false, null, true, false)));
    }

    /**
     * An Artifactory-style npm proxy that returns {@code HTTP 406} for the abbreviated packument media type still
     * serves the full packument to a client that also accepts {@code application/json}, so the bump resolves and
     * the lock regenerates.
     */
    @Test
    void packumentServedOverJsonFallbackRegeneratesLock() {
        routes.put("https://registry.npmjs.org/is-odd", resource("lock/npm/v3/http/is-odd"));
        routes.put("https://registry.npmjs.org/is-odd/3.0.0", resource("lock/npm/v3/http/is-odd-3.0.0"));
        routes.put("https://registry.npmjs.org/is-odd/3.0.1", resource("lock/npm/v3/http/is-odd-3.0.1"));

        responder = request -> {
            String accept = request.getRequestHeaders().getOrDefault("Accept", "");
            if (request.getUrl().toString().endsWith("/is-odd") && !accept.contains("application/json")) {
                return new HttpSender.Response(406,
                        new ByteArrayInputStream("Not Acceptable".getBytes(StandardCharsets.UTF_8)), () -> {
                });
            }
            return null;
        };

        String pkgBefore = "{\n" +
                "  \"name\": \"npm-lock-v3\",\n" +
                "  \"version\": \"1.0.0\",\n" +
                "  \"dependencies\": {\n" +
                "    \"is-odd\": \"3.0.0\"\n" +
                "  }\n" +
                "}\n";
        String pkgAfter = pkgBefore.replace("\"is-odd\": \"3.0.0\"", "\"is-odd\": \"3.0.1\"");

        rewriteRun(
                spec -> spec.recipe(new UpgradeDependencyVersion("is-odd", null, "3.0.1")).executionContext(ctx),
                packageJson(pkgBefore, pkgAfter,
                        nodeResolutionResult(PackageManager.Npm, dependency("is-odd", "3.0.0"))),
                packageLock(resource("lock/npm/v3/before"), resource("lock/npm/v3/after"), s -> s.noTrim())
        );
    }

    private static String resource(String path) {
        try (InputStream in = RegistryLockRegenTest.class.getClassLoader().getResourceAsStream(path)) {
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
