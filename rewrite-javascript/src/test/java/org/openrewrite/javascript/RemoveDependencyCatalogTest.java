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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.test.RewriteTest;

/**
 * A removal in a pnpm workspace with catalogs, as {@code pnpm remove} wrote it in the recordings under
 * {@code catalogs/pnpm}: the catalog loses the entries no project references only under {@code cleanupUnusedCatalogs}.
 */
class RemoveDependencyCatalogTest implements RewriteTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
      catalog-remove-manual                | wrappy
      catalog-remove-manual-cleanup        | wrappy
      catalog-remove-prefer                | wrappy
      catalog-remove-prefer-cleanup        | wrappy
      catalog-remove-strict                | wrappy
      catalog-remove-strict-cleanup        | wrappy
      catalog-remove-named-cleanup         | is-number
      """)
    void removedAsPnpmRemovedIt(String scenario, String name) {
        CatalogRecording recording = new CatalogRecording("pnpm", scenario);
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency(name, null)).executionContext(recording.ctx()),
          recording.sources()
        );
    }
}
