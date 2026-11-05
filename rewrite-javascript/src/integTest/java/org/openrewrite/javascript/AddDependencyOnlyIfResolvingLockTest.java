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

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.javascript.internal.PackageManagerExecutor;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.nodePackageManager;
import static org.openrewrite.javascript.Assertions.packageJson;

/**
 * {@code is-odd} declared as {@code >=1.0.0 <4} admits versions outside {@code ^3.0.0}, so only the version the
 * package manager's lock resolves, 3.0.1, lets the dependency be added.
 */
class AddDependencyOnlyIfResolvingLockTest implements RewriteTest {

    @ParameterizedTest
    @EnumSource(value = PackageManager.class, names = {"Npm", "YarnBerry", "Pnpm"})
    void addsByTheVersionTheLockResolves(PackageManager pm, @TempDir Path tempDir) {
        PackageManagerExecutor executor = pm == PackageManager.Npm ? PackageManagerExecutor.NPM :
          pm == PackageManager.Pnpm ? PackageManagerExecutor.PNPM : PackageManagerExecutor.YARN;
        Assumptions.assumeTrue(executor.find() != null, pm + " not installed");
        rewriteRun(
          spec -> spec.recipe(new AddDependency("is-number", "7.0.0", "dependencies", null,
            List.of("is-odd@^3.0.0"))),
          nodePackageManager(tempDir, pm,
            packageJson(
              """
                {
                  "name": "x",%s
                  "dependencies": {
                    "is-odd": ">=1.0.0 <4"
                  }
                }
                """.formatted(pm == PackageManager.YarnBerry ? "\n  \"packageManager\": \"yarn@4.0.2\"," : ""),
              spec -> spec.after(actual -> {
                  assertThat(actual).contains("\"is-number\": \"7.0.0\"");
                  return actual;
              })
            )
          )
        );
    }
}
