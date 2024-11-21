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

import org.junit.jupiter.api.Test;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.trait.Reference;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.properties.Assertions.properties;

class PropertiesReferenceTest implements RewriteTest {
    @Test
    void findJavaReferences() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new PropertiesReference.Matcher()
            .asVisitor(ref -> SearchResult.found(ref.getTree(), ref.getValue())))),
          properties(
            """
              a.fqt=java.lang.String
              b.package=java.lang
              c.type=Integer
              """,
            """
              ~~(java.lang.String)~~>a.fqt=java.lang.String
              ~~(java.lang)~~>b.package=java.lang
              c.type=Integer
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
                  }
                );
            })
          ));
    }
}
