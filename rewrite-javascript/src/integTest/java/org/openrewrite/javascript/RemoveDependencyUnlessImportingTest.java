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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.rpc.JavaScriptRewriteRpc;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.io.ByteArrayInputStream;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.openrewrite.javascript.Assertions.dependency;
import static org.openrewrite.javascript.Assertions.javascript;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;
import static org.openrewrite.javascript.Assertions.packageJson;
import static org.openrewrite.javascript.Assertions.typescript;

/**
 * The imports {@link RemoveDependency#getUnlessImporting()} reads from parsed sources.
 */
@Timeout(120)
class RemoveDependencyUnlessImportingTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        HttpSender http = request -> new HttpSender.Response(404, new ByteArrayInputStream(new byte[0]), () -> {
        });
        ExecutionContext ctx = new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });
        HttpSenderExecutionContextView.view(ctx).setHttpSender(http);
        NodeExecutionContextView.view(ctx).setRegistries(singletonList(
          new NodeRegistry(null, "https://registry.npmjs.org/", null, null, null, null, false, null, true, false)));
        spec.executionContext(ctx);
    }

    @AfterEach
    void after() {
        JavaScriptRewriteRpc.shutdownCurrent();
    }

    private static String manifest(String name) {
        return """
          {
            "name": "%s",
            "dependencies": {
              "lodash": "^4.17.21"
            }
          }
          """.formatted(name);
    }

    @Test
    void keepsTheDependencyOfEachProjectThatImportsIt() {
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("lodash", null, List.of("lodash"))),
          packageJson(manifest("es"), null, nodeResolutionResult(PackageManager.Npm,
            dependency("lodash", "^4.17.21")), s -> s.path("es/package.json")),
          typescript("import { chunk } from 'lodash';\nexport const c = chunk;\n", s -> s.path("es/src/a.ts")),
          packageJson(manifest("reexport"), null, nodeResolutionResult(PackageManager.Npm,
            dependency("lodash", "^4.17.21")), s -> s.path("reexport/package.json")),
          typescript("export { debounce } from 'lodash/debounce';\n", s -> s.path("reexport/src/a.ts")),
          packageJson(manifest("equals"), null, nodeResolutionResult(PackageManager.Npm,
            dependency("lodash", "^4.17.21")), s -> s.path("equals/package.json")),
          typescript("import _ = require('lodash');\nexport const c = _.chunk;\n", s -> s.path("equals/src/a.ts")),
          packageJson(manifest("dynamic"), null, nodeResolutionResult(PackageManager.Npm,
            dependency("lodash", "^4.17.21")), s -> s.path("dynamic/package.json")),
          typescript("export const load = () => import('lodash');\n", s -> s.path("dynamic/src/a.ts")),
          packageJson(manifest("type"), null, nodeResolutionResult(PackageManager.Npm,
            dependency("lodash", "^4.17.21")), s -> s.path("type/package.json")),
          typescript("export type Chunk = typeof import('lodash').chunk;\n", s -> s.path("type/src/a.ts")),
          packageJson(manifest("cjs"), null, nodeResolutionResult(PackageManager.Npm,
            dependency("lodash", "^4.17.21")), s -> s.path("cjs/package.json")),
          javascript("const { chunk } = require('lodash');\nmodule.exports = chunk;\n", s -> s.path("cjs/src/a.js"))
        );
    }

    @Test
    void removesTheDependencyOfAProjectThatOnlyMentionsIt() {
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("lodash", null, List.of("lodash"))),
          packageJson(manifest("app"),
            """
              {
                "name": "app"
              }
              """,
            nodeResolutionResult(PackageManager.Npm, dependency("lodash", "^4.17.21"))),
          typescript(
            """
              // import { chunk } from 'lodash';
              import { chunk } from './lodash';
              export const name = 'lodash';
              export const c = chunk;
              """,
            s -> s.path("src/a.ts"))
        );
    }
}
