/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.MinimumJava11;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class TypeCastTest implements RewriteTest {

    @SuppressWarnings({"RedundantCast", "unchecked"})
    @Test
    void cast() {
        rewriteRun(
          java(
            """
              class Test {
                  Object o = (Class<String>) Class.forName("java.lang.String");
              }
              """
          )
        );
    }

    @Test
    void intersectionCast() {
        rewriteRun(
          java(
            """
              import java.io.Serializable;
              import java.util.function.BiFunction;
                            
              class Test {
                  Serializable s = (Serializable & BiFunction<Integer, Integer, Integer>) Integer::sum;
              }
              """
          )
        );
    }

    @MinimumJava11
    @Test
    void intersectionCastAssignedToVar() {
        rewriteRun(
          java(
            """
              import java.io.Serializable;
              import java.util.function.BiFunction;
                            
              class Test {
                  void m() {
                      var s = (Serializable & BiFunction<Integer, Integer, Integer>) Integer::sum;
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                J.MethodDeclaration m = (J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0);
                J.VariableDeclarations s = (J.VariableDeclarations) m.getBody().getStatements().get(0);
                assertThat(s.getType()).isInstanceOf(JavaType.Intersection.class);
                JavaType.Intersection intersection = (JavaType.Intersection) s.getType();
                assertThat(intersection.getBounds()).satisfiesExactly(
                  b1 -> assertThat(b1).satisfies(
                    t -> assertThat(t).isInstanceOf(JavaType.Class.class),
                    t -> assertThat(((JavaType.Class) t).getFullyQualifiedName()).isEqualTo("java.io.Serializable")
                  ),
                  b2 -> assertThat(b2).satisfies(
                    t -> assertThat(t).isInstanceOf(JavaType.Parameterized.class),
                    t -> assertThat(((JavaType.Parameterized) t).getFullyQualifiedName()).isEqualTo("java.util.function.BiFunction"),
                    t -> assertThat(((JavaType.Parameterized) t).getTypeParameters()).hasSize(3),
                    t -> assertThat(((JavaType.Parameterized) t).getTypeParameters()).allSatisfy(
                      p -> assertThat(((JavaType.Class) p).getFullyQualifiedName()).isEqualTo("java.lang.Integer")
                    )
                  )
                );
            })
          )
        );
    }
}
