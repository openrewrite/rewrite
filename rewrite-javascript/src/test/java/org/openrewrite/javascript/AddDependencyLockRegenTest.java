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
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.dependency;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;
import static org.openrewrite.javascript.Assertions.packageJson;
import static org.openrewrite.javascript.Assertions.packageLock;

/**
 * PM-free end-to-end tests for {@link AddDependency} lock regeneration: a scalar-only leaf add is
 * resolved and written into the lock byte-exactly (Phase B increment 1), while a closure-changing add
 * (a package with transitives) still fails loud — the lock is left untouched, both files carry a
 * {@link Markup.Warn}, and a structured {@link NodeLockRegenerationFailures} row records the reason.
 */
class AddDependencyLockRegenTest implements RewriteTest {

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

    @Test
    void leafAddRegeneratesLockByteExact() {
        routes.put("https://registry.npmjs.org/left-pad", resource("lock/npm/add-leaf/http/left-pad"));
        routes.put("https://registry.npmjs.org/left-pad/1.3.0", resource("lock/npm/add-leaf/http/left-pad-1.3.0"));

        rewriteRun(
                spec -> spec.recipe(new AddDependency("left-pad", "^1.3.0", "dependencies"))
                        .executionContext(ctx),
                packageJson(resource("lock/npm/add-leaf/pkg-before"), null,
                        nodeResolutionResult(PackageManager.Npm, dependency("is-number", "6.0.0")),
                        s -> s.after(actual -> {
                            assertThat(actual).contains("\"left-pad\": \"^1.3.0\"");
                            return actual;
                        })),
                packageLock(resource("lock/npm/add-leaf/before"), resource("lock/npm/add-leaf/after"),
                        s -> s.noTrim())
        );
    }

    @Test
    void metadataLeafAddRegeneratesLockByteExact() {
        // A leaf carrying object metadata (engines) is now written byte-exact end-to-end (I1-follow).
        routes.put("https://registry.npmjs.org/is-number", resource("lock/npm/add-meta-engines/http/is-number"));
        routes.put("https://registry.npmjs.org/is-number/7.0.0",
                resource("lock/npm/add-meta-engines/http/is-number-7.0.0"));

        rewriteRun(
                spec -> spec.recipe(new AddDependency("is-number", "^7.0.0", "dependencies"))
                        .executionContext(ctx),
                packageJson(resource("lock/npm/add-meta-engines/pkg-before"), null,
                        nodeResolutionResult(PackageManager.Npm, dependency("left-pad", "1.3.0")),
                        s -> s.after(actual -> {
                            assertThat(actual).contains("\"is-number\": \"^7.0.0\"");
                            return actual;
                        })),
                packageLock(resource("lock/npm/add-meta-engines/before"), resource("lock/npm/add-meta-engines/after"),
                        s -> s.noTrim())
        );
    }

    @Test
    void closureAddFailsLoudWarnsAndRecordsDataTableRow() {
        // A package whose resolved version pulls a transitive is a closure add (Phase B I2), not a leaf.
        routes.put("https://registry.npmjs.org/needs-transitive", "{\"versions\":{\"1.0.0\":{}}}");
        routes.put("https://registry.npmjs.org/needs-transitive/1.0.0",
                "{\"name\":\"needs-transitive\",\"version\":\"1.0.0\",\"dependencies\":{\"is-number\":\"^6.0.0\"}}");

        String pkgBefore = "{\n" +
                "  \"name\": \"npm-lock-v3\",\n" +
                "  \"version\": \"1.0.0\",\n" +
                "  \"dependencies\": {\n" +
                "    \"is-odd\": \"3.0.0\"\n" +
                "  }\n" +
                "}\n";
        String lock = resource("lock/npm/v3/before");

        rewriteRun(
                spec -> spec.recipe(new AddDependency("needs-transitive", "^1.0.0", "dependencies"))
                        .executionContext(ctx)
                        .dataTable(NodeLockRegenerationFailures.Row.class, rows -> {
                            assertThat(rows).hasSize(1);
                            assertThat(rows.get(0).getSourcePath()).isEqualTo("package.json");
                            assertThat(rows.get(0).getPackageName()).isEqualTo("needs-transitive");
                            assertThat(rows.get(0).getReason()).isEqualTo("RESOLUTION_REQUIRED");
                        }),
                packageJson(pkgBefore, null,
                        nodeResolutionResult(PackageManager.Npm, dependency("is-odd", "3.0.0")),
                        s -> s.after(actual -> {
                            assertThat(actual).contains("\"needs-transitive\": \"^1.0.0\"");
                            return actual;
                        }).afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(Markup.Warn.class))
                                .as("manifest carries the lock-regen-failure warning").isPresent())),
                packageLock(lock, null,
                        s -> s.after(actual -> {
                            assertThat(actual)
                                    .as("the add was not written into the lock — it is left untouched")
                                    .doesNotContain("node_modules/needs-transitive")
                                    .contains("\"is-odd\": \"3.0.0\"");
                            return actual;
                        }).afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(Markup.Warn.class))
                                .as("lock carries the lock-regen-failure warning").isPresent()))
        );
    }

    private static String resource(String path) {
        try (InputStream in = AddDependencyLockRegenTest.class.getClassLoader().getResourceAsStream(path)) {
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
