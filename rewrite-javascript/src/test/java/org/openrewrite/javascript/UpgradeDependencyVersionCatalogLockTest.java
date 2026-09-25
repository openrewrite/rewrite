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
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;
import org.openrewrite.marker.Markup;
import org.openrewrite.test.RewriteTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.dependency;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;
import static org.openrewrite.javascript.Assertions.packageJson;
import static org.openrewrite.javascript.Assertions.pnpmLock;
import static org.openrewrite.yaml.Assertions.yaml;

/**
 * A run can carry two independent verdicts about one lock: the manifest edits regenerate cleanly, and
 * the catalog edit cannot be written at all. Reporting the second must not throw away the first.
 */
class UpgradeDependencyVersionCatalogLockTest implements RewriteTest {

    private ExecutionContext ctx;
    private final Map<String, String> routes = new HashMap<>();

    @BeforeEach
    void setUp() {
        routes.clear();
        HttpSender http = request -> {
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

    private static final String MANIFEST = "{\n" +
            "  \"name\": \"g1\",\n" +
            "  \"version\": \"1.0.0\",\n" +
            "  \"dependencies\": {\n" +
            "    \"ms\": \"%s\",\n" +
            "    \"ms-logger\": \"catalog:\"\n" +
            "  }\n" +
            "}\n";

    private static final String WORKSPACE_YAML = "packages:\n" +
            "  - '.'\n" +
            "catalog:\n" +
            "  ms-logger: '%s'\n";

    /**
     * The pattern matches a plain dependency and a catalog-backed one. The manifest bump regenerates the
     * lock cleanly; the catalog bump cannot be written to it at all. Reporting the second must not throw
     * away the first, or the plain dependency ends up worse off than if the catalog were never followed.
     */
    @Test
    void aStaleCatalogIsReportedWithoutDiscardingTheRegeneratedLock() {
        routes.put("https://registry.npmjs.org/ms", resource("lock/pnpm/v9/http/ms"));
        routes.put("https://registry.npmjs.org/ms/2.1.2", resource("lock/pnpm/v9/http/ms-2.1.2"));
        routes.put("https://registry.npmjs.org/ms/2.1.3", resource("lock/pnpm/v9/http/ms-2.1.3"));

        rewriteRun(
                spec -> spec.recipe(new UpgradeDependencyVersion(null, "ms*", "2.1.3")).executionContext(ctx)
                        .dataTable(NodeLockRegenerationFailures.Row.class, rows -> {
                            assertThat(rows).as("the catalog staleness is still reported").hasSize(1);
                            assertThat(rows.get(0).getPackageName()).isEqualTo("ms-logger");
                            assertThat(rows.get(0).getReason()).isEqualTo("UNSUPPORTED_ENTRY_TYPE");
                        }),
                packageJson(String.format(MANIFEST, "2.1.2"), String.format(MANIFEST, "2.1.3"),
                        nodeResolutionResult(PackageManager.Pnpm,
                                dependency("ms", "2.1.2"),
                                dependency("ms-logger", "catalog:"))),
                yaml(String.format(WORKSPACE_YAML, "~1.4.1"), String.format(WORKSPACE_YAML, "2.1.3"),
                        s -> s.path("pnpm-workspace.yaml")),
                pnpmLock(resource("lock/pnpm/v9/before"), resource("lock/pnpm/v9/after"),
                        s -> s.noTrim().afterRecipe(doc ->
                                assertThat(doc.getMarkers().findFirst(Markup.Warn.class))
                                        .as("the lock carries the stale-catalog warning").isPresent()))
        );
    }

    private static String resource(String path) {
        try (InputStream in = UpgradeDependencyVersionCatalogLockTest.class.getClassLoader().getResourceAsStream(path)) {
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
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
