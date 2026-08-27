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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.javascript.internal.PackageManagerExecutor;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Path;

import static org.openrewrite.javascript.Assertions.npm;
import static org.openrewrite.javascript.Assertions.packageJson;

/**
 * PM-gated parity cross-check: exercises the recipe against a real {@code npm} workspace. Native
 * PM-free regeneration is covered by {@code AddDependencyLockRegenTest} in {@code src/test}; this
 * suite skips when npm is not on the PATH.
 */
class AddDependencyTest implements RewriteTest {

    @BeforeEach
    void requirePackageManager() {
        Assumptions.assumeTrue(PackageManagerExecutor.NPM.find() != null, "npm not installed");
    }

    @Test
    void addsDependencyToPackageJson(@TempDir Path tempDir) {
        rewriteRun(
                spec -> spec.recipe(new AddDependency("lodash", "^4.17.21", "dependencies")),
                npm(tempDir,
                        packageJson(
                                "{\n" +
                                "  \"name\": \"x\",\n" +
                                "  \"dependencies\": {\n" +
                                "    \"uuid\": \"^9.0.0\"\n" +
                                "  }\n" +
                                "}\n",
                                "{\n" +
                                "  \"name\": \"x\",\n" +
                                "  \"dependencies\": {\n" +
                                "    \"uuid\": \"^9.0.0\",\n" +
                                "    \"lodash\": \"^4.17.21\"\n" +
                                "  }\n" +
                                "}\n")));
    }

    @Test
    void doesNotModifyWhenDependencyExists(@TempDir Path tempDir) {
        rewriteRun(
                spec -> spec.recipe(new AddDependency("uuid", "^10.0.0", "dependencies")),
                npm(tempDir,
                        packageJson(
                                "{\n" +
                                "  \"name\": \"x\",\n" +
                                "  \"dependencies\": {\n" +
                                "    \"uuid\": \"^9.0.0\"\n" +
                                "  }\n" +
                                "}\n")));
    }
}
