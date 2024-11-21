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

import org.junit.jupiter.api.Test;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.trait.Reference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.yaml.Assertions.yaml;

class YamlReferenceTest implements RewriteTest {

    @Test
    void findJavaReferences() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new YamlReference.Matcher()
            .asVisitor(yamlJavaReference -> SearchResult.found(yamlJavaReference.getTree(), yamlJavaReference.getValue())))),
          yaml(
            """
              root:
                  a: java.lang.String
                  b: java.lang
                  c: String
                  recipelist:
                    - org.openrewrite.java.DoSomething:
                        option: 'org.foo.Bar'
              """, """
              root:
                  a: ~~(java.lang.String)~~>java.lang.String
                  b: ~~(java.lang)~~>java.lang
                  c: String
                  recipelist:
                    - ~~(org.openrewrite.java.DoSomething)~~>org.openrewrite.java.DoSomething:
                        option: ~~(org.foo.Bar)~~>'org.foo.Bar'
              """,
            spec -> spec.afterRecipe(doc -> {
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
                  });
            }))
        );
    }
}
