/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.properties.trait;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.trait.Reference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.properties.Assertions.properties;

class PropertiesReferenceTest implements RewriteTest {

    @Language("properties")
    private static final String PROPERTIES = """
      a.fqt=java.lang.String
      b.package=java.lang
      c.type=Integer
      """;

    @CsvSource({
      "application.properties",
      "application-test.properties",
      "/foo/bar/application-test.properties",
    })
    @ParameterizedTest
    void findJavaReferencesInApplicationProperties(String filename) {
        rewriteRun(
          properties(
            PROPERTIES,
            spec -> spec.path(filename).afterRecipe(doc ->
              assertThat(doc.getReferences().getReferences())
                .satisfiesExactlyInAnyOrder(
                  ref -> {
                      assertThat(ref.getKind()).isEqualTo(Reference.Kind.TYPE);
                      assertThat(ref.getValue()).isEqualTo("java.lang.String");
                  },
                  ref -> {
                      assertThat(ref.getKind()).isEqualTo(Reference.Kind.PACKAGE);
                      assertThat(ref.getValue()).isEqualTo("java.lang");
                  }
                )
            )
          )
        );
    }

    @CsvSource({
      "application-.properties",
      "application.test.properties",
      "other-application.properties",
      "other.properties",
      "/foo/bar/other.properties"
    })
    @ParameterizedTest
    void noReferencesInMismatchedFilenames(String filename) {
        rewriteRun(
          properties(
            PROPERTIES,
            spec -> spec.path(filename).afterRecipe(doc -> assertThat(doc.getReferences().getReferences()).isEmpty())
          )
        );
    }
}
