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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.*;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;
import org.openrewrite.json.JsonParser;
import org.openrewrite.json.tree.Json;
import org.openrewrite.text.PlainText;
import org.openrewrite.yaml.YamlParser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.dependency;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;

class NodeDependencyEditsAtomicityTest {

    @ParameterizedTest(name = "{0}, lock first: {5}")
    @CsvSource(delimiter = '|', textBlock = """
      Npm         | npm/v3                              | is-odd   | 3.0.1 | package-lock.json | false
      Npm         | npm/v3                              | is-odd   | 3.0.1 | package-lock.json | true
      Pnpm        | pnpm/v9                             | ms       | 2.1.3 | pnpm-lock.yaml    | false
      Pnpm        | pnpm/v9                             | ms       | 2.1.3 | pnpm-lock.yaml    | true
      YarnClassic | yarn-classic/fixture-inplace-npmjs   | left-pad | 1.3.0 | yarn.lock         | false
      YarnClassic | yarn-classic/fixture-inplace-npmjs   | left-pad | 1.3.0 | yarn.lock         | true
      YarnBerry   | yarn-berry/merged-rehead             | ms       | 3.0.0 | yarn.lock         | false
      YarnBerry   | yarn-berry/merged-rehead             | ms       | 3.0.0 | yarn.lock         | true
      Bun         | bun/bump-root-exact                 | ms       | 2.1.3 | bun.lock          | false
      Bun         | bun/bump-root-exact                 | ms       | 2.1.3 | bun.lock          | true
      Pnpm | upgrade-transitive/pnpm/override-recipe-global | is-number | 7.0.0 | pnpm-lock.yaml | false
      Pnpm | upgrade-transitive/pnpm/override-recipe-global | is-number | 7.0.0 | pnpm-lock.yaml | true
      """)
    void failedRegenerationLeavesManifestAndLockTogether(PackageManager pm, String fixture, String name,
                                                        String version, String lockPath, boolean lockFirst) throws Exception {
        String manifest = fixture.equals("npm/v3") ? """
          {
            "name": "npm-lock-v3",
            "version": "1.0.0",
            "dependencies": {
              "is-odd": "3.0.0"
            }
          }
          """ : resource(fixture + "/pkg-before");
        String lock = resource(fixture + "/before");
        JsonNode pkg = new ObjectMapper().readTree(manifest);
        ExecutionContext ctx = new InMemoryExecutionContext(t -> { throw new RuntimeException(t); });
        HttpSenderExecutionContextView.view(ctx).setHttpSender(request -> new HttpSender.Response(404,
          new ByteArrayInputStream(new byte[0]), () -> {}));
        Json.Document doc = (Json.Document) JsonParser.builder().build().parse(ctx, manifest).findFirst().orElseThrow();
        doc = doc.withSourcePath(Paths.get("package.json")).withMarkers(doc.getMarkers().add(
          nodeResolutionResult(pm, dependency(name, pkg.path("dependencies").path(name).asText("*")))));
        SourceFile parsedLock = lockPath.endsWith(".json") ?
          JsonParser.builder().build().parse(ctx, lock).findFirst().orElseThrow() :
          lockPath.endsWith(".yaml") ? YamlParser.builder().build().parse(ctx, lock).findFirst().orElseThrow() :
          PlainText.builder().text(lock).build();
        parsedLock = parsedLock.withSourcePath(Paths.get(lockPath));
        List<SourceFile> sources = lockFirst ? Arrays.asList(parsedLock, doc) : Arrays.asList(doc, parsedLock);
        Recipe recipe = fixture.startsWith("upgrade-transitive/") ?
          new UpgradeTransitiveDependencyVersion(name, version, null) : new UpgradeDependencyVersion(name, null, version);
        RecipeRun run = recipe.run(new InMemoryLargeSourceSet(sources), ctx);
        Map<String, String> written = new HashMap<>();
        written.put("package.json", manifest);
        written.put(lockPath, lock);
        for (Result result : run.getChangeset().getAllResults()) {
            SourceFile after = Objects.requireNonNull(result.getAfter());
            written.put(after.getSourcePath().toString(), after.printAll(
              new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.SANITIZED)));
        }
        assertThat(run.getDataTableRows(NodeLockRegenerationFailures.class)).hasSize(1);
        assertThat(written.keySet()).containsExactlyInAnyOrder("package.json", lockPath);
        assertThat(written.get("package.json")).as("a failed lock update must not leave an edited manifest")
          .isEqualTo(manifest);
        assertThat(written.get(lockPath)).isEqualTo(lock);
    }

    private static String resource(String name) throws Exception {
        try (InputStream in = NodeDependencyEditsAtomicityTest.class.getResourceAsStream("/lock/" + name)) {
            assertThat(in).as(name).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
