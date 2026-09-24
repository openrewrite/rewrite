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

import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.javascript.Assertions.dependency;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;
import static org.openrewrite.javascript.Assertions.packageJson;

/**
 * An override that is already in place is a no-op for npm ({@code overrides}) and Yarn
 * ({@code resolutions}), which both compare the existing entry and return the document untouched.
 * The pnpm dialect ({@code pnpm.overrides}) has no such comparison: every pass re-serializes the
 * manifest through Jackson and reparses it, so the recipe hands back a fresh tree on every cycle and
 * never reaches a fixed point. The reparse is also why the manifest comes back reprinted in Jackson's
 * pretty-printer style rather than edited in place.
 */
class UpgradeTransitiveDependencyVersionPnpmTest implements RewriteTest {

    @Test
    void pnpmOverrideConvergesInOneCycle() {
        String before = "{\n" +
                "  \"name\": \"consumer\",\n" +
                "  \"version\": \"1.0.0\",\n" +
                "  \"dependencies\": {\n" +
                "    \"acme-logger\": \"~1.4.1\"\n" +
                "  }\n" +
                "}\n";

        String after = "{\n" +
                "  \"name\": \"consumer\",\n" +
                "  \"version\": \"1.0.0\",\n" +
                "  \"dependencies\": {\n" +
                "    \"acme-logger\": \"~1.4.1\"\n" +
                "  },\n" +
                "  \"pnpm\": {\n" +
                "    \"overrides\": {\n" +
                "      \"acme-transitive\": \"~2.0.0\"\n" +
                "    }\n" +
                "  }\n" +
                "}\n";

        rewriteRun(
                spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("acme-transitive", "~2.0.0", null)),
                packageJson(before, after,
                        nodeResolutionResult(PackageManager.Pnpm, dependency("acme-logger", "~1.4.1")))
        );
    }
}
