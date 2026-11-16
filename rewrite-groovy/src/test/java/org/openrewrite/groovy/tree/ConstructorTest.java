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
package org.openrewrite.groovy.tree;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.Issue;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.groovy.Assertions.groovy;

class ConstructorTest implements RewriteTest {

    @Test
    void inParens() {
        rewriteRun(
          groovy(
            """
              ( new String("foo") )
              """
          )
        );
    }

    @Test
    void withGenerics() {
        rewriteRun(
          groovy(
            """
              new ArrayList<String>()
              """
          )
        );
    }

    @Test
    void withDiamondOperator() {
        rewriteRun(
          groovy(
            """
              new ArrayList<>()
              """
          )
        );
    }

    @Test
    void declaration() {
        rewriteRun(
          groovy(
            """
              class Pair {
                  String first
                  String second
                  Pair(String first, String second) {
                      this.first = first
                      this.second = second
                  }
              }
              """
          )
        );
    }

    @Test
    void anonymousClassDeclarationClosedOverVariable() {
        rewriteRun(
          groovy(
            """
              int i = 1
              new Object() {
                  int one() {
                      return i
                  }
              }
              """
          )
        );
    }

    @Test
    void implicitPublic() {
        rewriteRun(
          groovy(
            """
              class T {
                  T(int a, int b, int c) {
                  }
              }
              """
          )
        );
    }

    @Test
    void superCall() {
        rewriteRun(
          groovy(
            """
              class T {
                  T() {
                      super ( )
                  }
              }
              """
          )
        );
    }

    @Test
    void thisCall() {
        rewriteRun(
          groovy(
            """
              class T {
                  T() {
                      this ( 3 )
                  }
                  T(int foo) {
                      super ( )
                  }
              }
              """
          )
        );
    }

    @Test
    void defaultConstructorArguments() {
        rewriteRun(
          groovy(
            """
              class T {
                  T(int a = 1) {
                  }
              }
              """
          )
        );
    }

    @Test
    void nonStaticInnerClassNamedArgs() {
        rewriteRun(
          groovy(
            """
              class T {
                  class Reader { }
                  def m() { new Reader(a: 1) }
              }
              """
          )
        );
    }

    @Test
    void innerClassConstructorWithOnlySuperCall() {
        rewriteRun(
          groovy(
            """
              class Outer {
                  class Inner extends Base {
                      Inner(int x, int y) {
                          super(x, y)
                      }
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9871")
    @Test
    void aNestedClassNameBrokenAfterTheDot() {
        rewriteRun(
          groovy(
            """
              def m() {
                foo(1, new Outer.
                  Inner(1))
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9871")
    @Test
    void aSpaceAfterTheDot() {
        rewriteRun(
          groovy(
            """
              def m() {
                foo(new Outer. Inner(1))
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9871")
    @Test
    void aSpaceBeforeTheDot() {
        rewriteRun(
          groovy(
            """
              def m() {
                foo(new Outer .Inner(1))
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9871")
    @Test
    void aPackageNameBrokenAfterADot() {
        rewriteRun(
          groovy(
            """
              def m() {
                foo(new a.b.
                  C(1))
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9871")
    @Test
    void aDeclaredTypeWithSpacedDotsAndACallOnAType() {
        rewriteRun(
          groovy(
            """
              def m() {
                def x = List
                  .of(1)
                def y = new Outer.Inner(1)
                java.util.List z = null
                java . util . Map w = null
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9871")
    @ParameterizedTest
    @ValueSource(strings = {
      "Outer. /* comment */ Inner",
      "Outer /* comment */ .Inner",
      "Outer. /* a.b */ Inner",
      "Outer. // a.b\n Inner",
      "Outer /* a.b */ . /* c.d */ Inner"
    })
    void commentsInQualifiedConstructorName(String typeName) {
        rewriteRun(
          groovy(
            "def m() { foo(new " + typeName + "(1)) }",
            spec -> spec.afterRecipe(cu -> new GroovyIsoVisitor<Integer>() {
                @Override
                public J.NewClass visitNewClass(J.NewClass newClass, Integer p) {
                    assertThat(newClass.getClazz()).isInstanceOfSatisfying(J.FieldAccess.class, clazz -> {
                        assertThat(clazz.getSimpleName()).isEqualTo("Inner");
                        assertThat(clazz.getTarget()).isInstanceOfSatisfying(J.Identifier.class,
                          target -> assertThat(target.getSimpleName()).isEqualTo("Outer"));
                    });
                    return super.visitNewClass(newClass, p);
                }
            }.visit(cu, 0))
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9871")
    @ParameterizedTest
    @ValueSource(strings = {
      "foo.String.String",
      "foo.String . String",
      "foo . String . String",
      "foo.String /* comment */ . String",
      "foo.String. /* comment */ String",
      "foo.String.\n String"
    })
    void repeatedSegmentInQualifiedConstructorName(String typeName) {
        rewriteRun(
          groovy(
            """
              package foo.String
              class String {}
              def m() { foo(new %s()) }
              """.formatted(typeName)
          )
        );
    }
}
