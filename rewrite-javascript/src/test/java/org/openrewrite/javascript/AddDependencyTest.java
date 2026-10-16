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
 * {@link AddDependency} saves the manifest as each package manager's add does, recorded under {@code lock/catalogs/}.
 */
class AddDependencyTest implements RewriteTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
      npm          | add-npm-sorts-every-field
      npm          | add-npm-beside-empty-field
      pnpm         | add-pnpm-sorts-every-field
      pnpm         | add-pnpm-beside-empty-field
      yarn-classic | add-yarn-classic-sorts-every-field
      yarn-classic | add-yarn-classic-beside-empty-field
      yarn-berry   | add-yarn-berry-sorts-every-field
      yarn-berry   | add-yarn-berry-beside-empty-field
      bun          | add-bun-sorts-every-field
      bun          | add-bun-beside-empty-field
      """)
    void savedAsThePackageManagersAddSavesIt(String family, String scenario) {
        CatalogRecording recording = new CatalogRecording(family, scenario);
        rewriteRun(
          spec -> spec.recipe(new AddDependency("is-number", "^7.0.0", null)).executionContext(recording.ctx()),
          recording.sources()
        );
        recording.assertEveryVisitOrder(new AddDependency("is-number", "^7.0.0", null));
    }
}
