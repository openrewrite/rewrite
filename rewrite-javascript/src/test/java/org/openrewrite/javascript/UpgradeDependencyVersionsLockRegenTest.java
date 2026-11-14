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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;
import org.openrewrite.marker.Markup;
import org.openrewrite.test.RewriteTest;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;
import static org.openrewrite.javascript.Assertions.packageJson;
import static org.openrewrite.javascript.Assertions.packageLock;

class UpgradeDependencyVersionsLockRegenTest implements RewriteTest {
    private static final String REGISTRY = "https://registry.npmjs.org/";
    private final Map<String, String> routes = new HashMap<>();
    private ExecutionContext ctx;

    @BeforeEach
    void registry() throws Exception {
        routes.clear();
        addPackage("compiler", Map.of("1.0.0", Map.of(), "2.0.0", Map.of()));
        addPackage("builder", Map.of(
          "1.0.0", Map.of("dependencies", Map.of("tools", "1.0.0"), "peerDependencies", Map.of("compiler", "1")),
          "2.0.0", Map.of("dependencies", Map.of("tools", "2.0.0"), "peerDependencies", Map.of("compiler", "2"))));
        addPackage("tools", Map.of(
          "1.0.0", Map.of("peerDependencies", Map.of("compiler", "1")),
          "2.0.0", Map.of("peerDependencies", Map.of("compiler", "2"))));
        addPackage("legacy-consumer", Map.of("1.0.0", Map.of("peerDependencies", Map.of("compiler", "1"))));
        ctx = new InMemoryExecutionContext(t -> { throw new RuntimeException(t); });
        HttpSenderExecutionContextView.view(ctx).setHttpSender(request -> {
            String body = routes.get(request.getUrl().toString());
            return new HttpSender.Response(body == null ? 404 : 200,
              new ByteArrayInputStream((body == null ? "" : body).getBytes(StandardCharsets.UTF_8)), () -> { });
        });
        NodeExecutionContextView.view(ctx).setRegistries(singletonList(
          new NodeRegistry(null, REGISTRY, null, null, null, null, false, null, true, false)));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = false)
    void strictDefaultRejectsStaleLockAndLeavesBothFilesUnchanged(@Nullable Boolean reconcile) {
        rejected(new UpgradeDependencyVersions(Map.of("builder", "2.0.0", "compiler", "2.0.0"), reconcile), false, "builder");
    }

    @Test
    void optedInBatchMovesAllAlignedDependenciesAndTheirTransitivesTogether() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersions(Map.of("builder", "2.0.0", "compiler", "2.0.0"), true))
            .executionContext(ctx)
            .afterRecipe(run -> assertThat(run.getDataTableRows(NodeLockRegenerationFailures.class)).isEmpty()),
          packageJson(manifest("1.0.0", false), manifest("2.0.0", false), nodeResolutionResult(PackageManager.Npm),
            spec -> spec.afterRecipe(doc -> {
                assertThat(doc.getMarkers().findFirst(Markup.Warn.class)).isEmpty();
                assertThat(doc.getMarkers().findFirst(NodeResolutionResult.class).orElseThrow().getResolvedDependencies())
                  .extracting(d -> d.getName() + "@" + d.getVersion())
                  .contains("builder@2.0.0", "compiler@2.0.0", "tools@2.0.0");
            })),
          packageLock(lock(false), null, spec -> spec.noTrim().after(actual -> {
              assertThat(actual).contains("\"version\": \"2.0.0\"").doesNotContain("\"version\": \"1.0.0\",\n      \"resolved\"");
              return actual;
          }).afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(Markup.Warn.class)).isEmpty()))
        );
    }

    @Test
    void recoveryDoesNotMakeAnIndividualIntermediateUpgradePeerCompatible() {
        rejected(new UpgradeDependencyVersions(Map.of("compiler", "2.0.0"), true), false, "builder");
    }

    @Test
    void realConflictRejectsTheWholeBatchAndNamesTheBlockingPackage() {
        rejected(new UpgradeDependencyVersions(Map.of("builder", "2.0.0", "compiler", "2.0.0"), true), true, "legacy-consumer");
    }

    private void rejected(UpgradeDependencyVersions recipe, boolean blocker, String expectedBlocker) {
        String manifest = manifest("1.0.0", blocker);
        String lock = lock(blocker);
        rewriteRun(
          spec -> spec.recipe(recipe).executionContext(ctx)
            .dataTable(NodeLockRegenerationFailures.Row.class, rows -> assertThat(rows).singleElement()
              .satisfies(row -> {
                  assertThat(row.getReason()).isEqualTo("PEER_CONFLICT");
                  assertThat(row.getDetail()).contains(expectedBlocker);
              })),
          packageJson(manifest, null, nodeResolutionResult(PackageManager.Npm), spec -> spec.after(actual -> {
              assertThat(actual).contains("lock regeneration failed").endsWith(manifest);
              return actual;
          })),
          packageLock(lock, null, spec -> spec.after(actual -> {
              assertThat(actual).contains("lock regeneration failed").endsWith(lock);
              return actual;
          }))
        );
    }

    private void addPackage(String name, Map<String, Map<String, Object>> versions) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> manifests = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : versions.entrySet()) {
            String version = entry.getKey();
            Map<String, Object> manifest = new LinkedHashMap<>(entry.getValue());
            manifest.put("name", name);
            manifest.put("version", version);
            manifest.put("dist", Map.of("tarball", REGISTRY + name + "/-/" + name + "-" + version + ".tgz"));
            manifests.put(version, manifest);
            routes.put(REGISTRY + name + "/" + version, mapper.writeValueAsString(manifest));
        }
        routes.put(REGISTRY + name, mapper.writeValueAsString(Map.of("name", name, "versions", manifests)));
    }

    private static String manifest(String version, boolean blocker) {
        return "{\"name\":\"root\",\"version\":\"1.0.0\",\"dependencies\":{" +
               "\"builder\":\"" + version + "\",\"compiler\":\"" + version + "\"" +
               (blocker ? ",\"legacy-consumer\":\"1.0.0\"" : "") + "}}";
    }

    private static String lock(boolean blocker) {
        return "{\"name\":\"root\",\"version\":\"1.0.0\",\"lockfileVersion\":3,\"requires\":true,\"packages\":{" +
               "\"\":" + manifest("1.0.0", blocker) + "," +
               "\"node_modules/builder\":{\"version\":\"1.0.0\",\"dependencies\":{\"tools\":\"1.0.0\"},\"peerDependencies\":{\"compiler\":\"1\"}}," +
               "\"node_modules/compiler\":{\"version\":\"1.0.0\"}," +
               "\"node_modules/tools\":{\"version\":\"1.0.0\",\"peerDependencies\":{\"compiler\":\"1\"}}" +
               (blocker ? ",\"node_modules/legacy-consumer\":{\"version\":\"1.0.0\",\"peerDependencies\":{\"compiler\":\"1\"}}" : "") + "}}";
    }
}
