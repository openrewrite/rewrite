/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.yaml;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.yaml.Assertions.yaml;

class CopyValueTest implements RewriteTest {

    @Test
    void copyValueAndItsFormatting() {
        rewriteRun(
          spec -> spec.recipe(new CopyValue(".source", ".destination", null)),
          yaml(
            """
              id: something
              source:   password
              destination: whatever
              """,
            """
              id: something
              source:   password
              destination:   password
              """
          )
        );
    }

    @Test
    void onlyCopiesScalars() {
        rewriteRun(
          spec -> spec.recipe(new CopyValue(".source", ".destination", null)),
          yaml(
            """
              id: something
              source:
                  complex:
                      structure: 1
              destination: whatever
              """
          )
        );
    }

    @Test
    void insertCopyValueAndRemoveSource() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml("$", "destination: TEMP", true, null, null)
            .doNext(new CopyValue(".source", ".destination", null))
            .doNext(new DeleteKey(".source", null))),
          yaml(
            """
              id: something
              source:   password
              """,
            """
              id: something
              destination:   password
              """
          )
        );
    }

    @Test
    void changeOnlyMatchingFile() {
        rewriteRun(
          spec -> spec.recipe(new CopyValue(".source", ".destination", "**/a.yml")),
          yaml(
            """
              source: password
              destination: whatever
              """,
            """
              source: password
              destination: password
              """,
            spec -> spec.path("a.yml")
          ),
          yaml(
            """
              source: password
              destination: whatever
              """,
            spec -> spec.path("b.yml")
          )
        );
    }
}
