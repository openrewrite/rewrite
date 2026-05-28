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
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Path;

import static org.openrewrite.javascript.Assertions.npm;
import static org.openrewrite.javascript.Assertions.packageJson;

class RemoveDependencyTest implements RewriteTest {

    @Test
    void removesDependencyFromPackageJson(@TempDir Path tempDir) {
        rewriteRun(
                spec -> spec.recipe(new RemoveDependency("lodash", null)),
                npm(tempDir,
                        packageJson(
                                "{\n" +
                                "  \"name\": \"x\",\n" +
                                "  \"dependencies\": {\n" +
                                "    \"lodash\": \"^4.17.21\",\n" +
                                "    \"uuid\": \"^9.0.0\"\n" +
                                "  }\n" +
                                "}\n",
                                "{\n" +
                                "  \"name\": \"x\",\n" +
                                "  \"dependencies\": {\n" +
                                "    \"uuid\": \"^9.0.0\"\n" +
                                "  }\n" +
                                "}\n")));
    }

    @Test
    void doesNotModifyWhenPackageAbsent(@TempDir Path tempDir) {
        rewriteRun(
                spec -> spec.recipe(new RemoveDependency("nonexistent", null)),
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
