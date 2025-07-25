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
package org.openrewrite.yaml.trait;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.trait.Reference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.yaml.Assertions.yaml;

class YamlReferenceTest implements RewriteTest {
    @Language("yml")
    private static final String YAML = """
      root:
          a: java.lang.String
          b: java.lang
          c: String
          recipelist:
            - org.openrewrite.java.DoSomething:
                option: 'org.foo.Bar'
      """;


    @CsvSource({
      "application.yaml",
      "application.yml",
      "application-test.yaml",
      "application-test.yml",
      "/foo/bar/application-test.yaml",
      "/foo/bar/application-test.yml",
    })
    @ParameterizedTest
    void findJavaReferencesInYamlProperties(String filename) {
        rewriteRun(
          yaml(
            YAML,
            spec -> spec.path(filename).afterRecipe(doc ->
              assertThat(doc.getReferences().getReferences()).satisfiesExactlyInAnyOrder(
                ref -> {
                    assertThat(ref.getKind()).isEqualTo(Reference.Kind.TYPE);
                    assertThat(ref.getValue()).isEqualTo("java.lang.String");
                },
                ref -> {
                    assertThat(ref.getKind()).isEqualTo(Reference.Kind.PACKAGE);
                    assertThat(ref.getValue()).isEqualTo("java.lang");
                },
                ref -> {
                    assertThat(ref.getKind()).isEqualTo(Reference.Kind.TYPE);
                    assertThat(ref.getValue()).isEqualTo("org.openrewrite.java.DoSomething");
                },
                ref -> {
                    assertThat(ref.getKind()).isEqualTo(Reference.Kind.TYPE);
                    assertThat(ref.getValue()).isEqualTo("org.foo.Bar");
                })))
        );
    }

    @CsvSource({
      "application-.yaml",
      "application-.yml",
      "application.test.yaml",
      "application.test.yml",
      "other-application.yaml",
      "other-application.yml",
      "other.yaml",
      "other.yml",
      "/foo/bar/other.yaml",
      "/foo/bar/other.yml"
    })
    @ParameterizedTest
    void noReferencesInMismatchedFilenames(String filename) {
        rewriteRun(
          yaml(
            YAML,
            spec -> spec.path(filename).afterRecipe(doc -> assertThat(doc.getReferences().getReferences()).isEmpty())
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4817")
    @Test
    void endsWithDot() {
        rewriteRun(
          yaml(
            """
              root:
                  recipelist:
                    - org.openrewrite.java.DoSomething:
                        option: 'org.foo.'
              """,
            spec -> spec
              .path("application.yml")
              .afterRecipe(doc -> assertThat(doc.getReferences().getReferences()).singleElement().satisfies(
                ref -> {
                    assertThat(ref.getKind()).isEqualTo(Reference.Kind.TYPE);
                    assertThat(ref.getValue()).isEqualTo("org.openrewrite.java.DoSomething");
                })
              )
          )
        );
    }
}
