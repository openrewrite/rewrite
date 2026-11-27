/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.table.ClasspathReport;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.SourceSpecs.text;

class ListRuntimeClasspathTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ListRuntimeClasspath());
    }

    @DocumentExample
    @Test
    void listClasspathEntries() throws Exception {
        String testClassesUri = Path.of(ListRuntimeClasspathTest.class.getProtectionDomain().getCodeSource().getLocation().toURI())
          .toUri().toString();
        rewriteRun(
          spec -> spec.dataTable(ClasspathReport.Row.class, rows ->
            assertThat(rows).contains(new ClasspathReport.Row(testClassesUri, ""))),
          text("hello")
        );
    }
}
