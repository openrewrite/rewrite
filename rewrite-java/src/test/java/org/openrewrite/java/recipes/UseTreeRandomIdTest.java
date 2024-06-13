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
package org.openrewrite.java.recipes;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class UseTreeRandomIdTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseTreeRandomId())
          .parser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()));
    }

    @Test
    @DocumentExample
    void replacesUUIDRandomId() {
        rewriteRun(
          java(
            """
              import org.openrewrite.java.tree.J;
              import org.openrewrite.java.tree.JavaType;
              import org.openrewrite.marker.Markers;
              import org.openrewrite.java.tree.Space;
              
              import java.util.UUID;
              import static java.util.UUID.randomUUID;
              
              class Foo {
                  void bar() {
                      J.Literal literal1 = new J.Literal(UUID.randomUUID(), Space.EMPTY, Markers.EMPTY, null, null, null, JavaType.Primitive.Boolean);
                      J.Literal literal2 = new J.Literal(randomUUID(), Space.EMPTY, Markers.EMPTY, null, null, null, JavaType.Primitive.Boolean);
                  }
              }
              """,
            """
              import org.openrewrite.Tree;
              import org.openrewrite.java.tree.J;
              import org.openrewrite.java.tree.JavaType;
              import org.openrewrite.marker.Markers;
              import org.openrewrite.java.tree.Space;
              
              class Foo {
                  void bar() {
                      J.Literal literal1 = new J.Literal(Tree.randomId(), Space.EMPTY, Markers.EMPTY, null, null, null, JavaType.Primitive.Boolean);
                      J.Literal literal2 = new J.Literal(Tree.randomId(), Space.EMPTY, Markers.EMPTY, null, null, null, JavaType.Primitive.Boolean);
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotReplaceRegularUse() {
        rewriteRun(
          java(
            """
              import org.openrewrite.java.tree.J;
              import org.openrewrite.java.tree.JavaType;
              import org.openrewrite.marker.Markers;
              import org.openrewrite.java.tree.Space;
              
              import java.util.UUID;
              
              class Foo {
                  Foo(UUID uuid) {}
              
                  void bar() {
                      J.Literal literal1 = new J.Literal(UUID.randomUUID(), Space.EMPTY, Markers.EMPTY, null, null, null, JavaType.Primitive.Boolean);
                      Foo untouched = new Foo(UUID.randomUUID());
                  }
              }
              """,
            """
              import org.openrewrite.Tree;
              import org.openrewrite.java.tree.J;
              import org.openrewrite.java.tree.JavaType;
              import org.openrewrite.marker.Markers;
              import org.openrewrite.java.tree.Space;
              
              import java.util.UUID;
              
              class Foo {
                  Foo(UUID uuid) {}
              
                  void bar() {
                      J.Literal literal1 = new J.Literal(Tree.randomId(), Space.EMPTY, Markers.EMPTY, null, null, null, JavaType.Primitive.Boolean);
                      Foo untouched = new Foo(UUID.randomUUID());
                  }
              }
              """
          )
        );
    }
}
