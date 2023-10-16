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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.test.RewriteTest;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class JavaTemplateBugsTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/3623")
    @Test
    void innerClass$Named() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations vd, ExecutionContext ctx) {
                  J.Identifier $type = new J.Identifier(Tree.randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    emptyList(),
                    ((JavaType.FullyQualified) Objects.requireNonNull(vd.getType())).getClassName(),
                    vd.getType(),
                    null);
                  Expression initializer = new J.MethodInvocation(
                    UUID.randomUUID(),
                    vd.getPrefix(),
                    vd.getMarkers(),
                    null, null,
                    $type,
                    JContainer.empty(),
                    new JavaType.Method(
     null,
                      0L,
                      (JavaType.FullyQualified)vd.getType(),
                      "of",
                      vd.getType(),
                      (List<String>) null,
                      null,
                      null,
                      null,
                      null)
                  );
                  return JavaTemplate.builder("$ test = #{any()}")
                    .contextSensitive()
                    .doBeforeParseTemplate(System.out::println)
                    .build()
                    .apply(getCursor(), vd.getCoordinates().replace(), initializer);
              }
          })),
          java(
            """
              class A {
                  void foo() {
                      $ test = new $();
                  }
                  class $ {
                      $ of() {
                          return new $();
                       }
                  }
              }
              """,
            """
              class A {
                  void foo() {
                      $ test = new $.of();
                  }
                  class $ {
                      $ of() {
                          return new $();
                       }
                  }
              }
              """
          )
        );
    }
}
