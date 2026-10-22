/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.python.tree;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.python.Assertions.python;

class ClassDeclarationTest implements RewriteTest {

    @ParameterizedTest
    @ValueSource(strings = {
      "Foo:",
      "Foo :",
      "Foo(Bar):",
      "Foo (Bar):",
      "Foo(Bar) :",
      "Foo( Bar):",
      "Foo(Bar ):",
      "Foo():",
      "Foo( ):",
      "Foo ():",
      "Foo (Bar1, Bar2):",
    })
    void classDeclaration(String decl) {
        rewriteRun(
          python(
            """
              class %s
                  pass
              """.formatted(decl)
          )
        );
    }

    @Test
    void spaceBetweenClasses() {
        rewriteRun(
          python(
            """
              class Foo:
                pass
              
              
              class Bar:
                pass
              """
          )
        );
    }

    @Test
    void nestedClasses() {
        rewriteRun(
          python(
            """
              class A:
              
                class B:
                  pass
              """
          )
        );
    }

    @Test
    void decoratedClassIsTypedAsTheClassItDeclares() {
        rewriteRun(
          python(
            """
              try:
                  from unittest import mock
              except ImportError:
                  import mock


              class Request:
                  def send(self): ...


              @mock.patch.object(Request, "send")
              class TestBuildProxy:
                  pass


              def replace(c) -> int:
                  ...


              @replace
              class Replaced:
                  pass
              """,
            spec -> spec.afterRecipe(cu -> {
                List<J.ClassDeclaration> classes = cu.getStatements().stream()
                  .filter(J.ClassDeclaration.class::isInstance)
                  .map(J.ClassDeclaration.class::cast)
                  .collect(toList());
                assertThat(classes.get(1).getType()).isNotNull()
                  .satisfies(t -> assertThat(t.getClassName()).isEqualTo("TestBuildProxy"));
                assertThat(classes.get(2).getType()).isNotNull()
                  .satisfies(t -> {
                      assertThat(t.getClassName()).isEqualTo("Replaced");
                      assertThat(t.getAnnotations())
                        .extracting(JavaType.FullyQualified::getFullyQualifiedName)
                        .containsExactly("file.replace");
                  });
            })
          )
        );
    }
}
