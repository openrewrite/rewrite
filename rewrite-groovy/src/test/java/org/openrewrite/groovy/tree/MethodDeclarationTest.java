/*
 * Copyright 2021 the original author or authors.
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
import org.openrewrite.Issue;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.RewriteTest;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.groovy.Assertions.groovy;

@SuppressWarnings({"GrUnnecessaryDefModifier", "GrMethodMayBeStatic"})
class MethodDeclarationTest implements RewriteTest {

    @Test
    void methodDeclarationDeclaringType() {
        rewriteRun(
          // Avoid type information in the usual groovy parser type cache leaking and affecting this test
          // In the real world you don't parse a bunch of classes all named "A" all at once
          spec -> spec.parser(GroovyParser.builder()),
          groovy(
            """
              class A {
                  void method() {}
              }
              """,
            spec -> spec.beforeRecipe(cu -> {
                var method = (J.MethodDeclaration) cu.getClasses().getFirst().getBody().getStatements().getFirst();
                JavaType.Method methodType = method.getMethodType();
                assertThat(methodType).isNotNull();
                assertThat(methodType.getName()).isEqualTo("method");
                var declaring = methodType.getDeclaringType();
                assertThat(declaring.getFullyQualifiedName()).isEqualTo("A");
                assertThat(declaring.getMethods().stream()
                  .filter(m -> m == methodType)
                  .findAny()).isPresent();
            })
          )
        );
    }

    @Test
    void methodDeclaration() {
        rewriteRun(
          groovy(
            """
              def accept(Map m) {
              }
              """
          )
        );
    }

    @Test
    void primitiveReturn() {
        rewriteRun(
          groovy(
            """
              static int accept(Map m) {
                  return 0
              }
              """
          )
        );
    }

    @Test
    void genericTypeParameterReturn() {
        rewriteRun(
          groovy(
            """
              interface Foo {
                  <T extends Task> T task(Class<T> type)
              }
              """
          )
        );
    }

    @Test
    void modifiersReturn() {
        rewriteRun(
          groovy(
            """
              public final accept(Map m) {
              }
              """
          )
        );
    }

    @Test
    void emptyArguments() {
        rewriteRun(
          groovy("def foo( ) {}")
        );
    }

    @Test
    void dynamicTypedArguments() {
        rewriteRun(
          groovy(
            """
              def foo(bar, baz) {
              }
              """
          )
        );
    }

    @Test
    void varargsArguments() {
        rewriteRun(
          groovy(
            """
              def foo(String... messages) {
                  println(messages[0])
              }
              """
          )
        );
    }

    @Test
    void defaultArgumentValues() {
        rewriteRun(
          groovy(
            """
              def confirmNextStepWithCredentials(String message /* = prefix */ = /* hello prefix */ "Hello" ) {
              }
              """
          )
        );
    }

    @Test
    void modifiersArguments() {
        rewriteRun(
          groovy(
            """
              def accept(final def Map m) {
              }
              """
          )
        );
    }

    @Test
    void methodThrows() {
        rewriteRun(
          groovy(
            """
              def foo(int a) throws Exception , RuntimeException {
              }
              """
          )
        );
    }

    @Test
    void returnNull() {
        rewriteRun(
          groovy(
            """
              static def foo() {
                  return null
              }
              """
          )
        );
    }
    @Test
    void arrayType() {
        rewriteRun(
          groovy(
            """
              def foo(String[][] s) {}
              """,
                spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                    @Override
                    public J.ArrayType visitArrayType(J.ArrayType arrayType, Object o) {
                        if (arrayType.getElementType() instanceof J.ArrayType) {
                            assertThat(Objects.requireNonNull(arrayType.getElementType().getType()).toString()).isEqualTo("java.lang.String[]");
                            assertThat(Objects.requireNonNull(arrayType.getType()).toString()).isEqualTo("java.lang.String[][]");
                        }
                        return super.visitArrayType(arrayType, o);
                    }
                }.visit(cu, 0))
          )
        );
    }


    @Issue("https://github.com/openrewrite/rewrite/issues/3559")
    @Test
    void escapedMethodName() {
        rewriteRun(
          groovy(
            """
              def 'default'() {}
              'default'()
              """
          )
        );
    }

    @Test
    void escapedMethodNameWithSpaces() {
        rewriteRun(
          groovy(
            """
              def 'some test scenario description'() {}
              'some test scenario description'()
              """
          )
        );
    }

    @Test
    void escapedMethodNameWithDollarSign() {
        rewriteRun(
          groovy(
            """
              def "xMethod\\${regularText}"() {}
              "xMethod\\${regularText}"()
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4705")
    @Test
    void functionWithDefAndExplicitReturnType() {
        rewriteRun(
          groovy(
            """
              class B {
                  def /*int*/ int one() { 1 }
                  @Foo def /*Object*/ Object two() { 2 }
              }
              """
          )
        );
    }

    @Test
    void parameterWithConflictingTypeName() {
        rewriteRun(
          groovy(
            """
              class variable {}
              def accept(final def variable m) {}
              """,
            spec -> spec.afterRecipe(cu -> {
                J.MethodDeclaration accept = (J.MethodDeclaration) cu.getStatements().get(1);
                J.VariableDeclarations m = (J.VariableDeclarations) accept.getParameters().getFirst();
                assertThat(m.getModifiers()).satisfiesExactly(
                  mod -> assertThat(mod.getType()).isEqualTo(J.Modifier.Type.Final),
                  mod -> assertThat(mod.getKeyword()).isEqualTo("def")
                );
            })
          )
        );
    }

    @Test
    void defIsNotReturnType() {
        rewriteRun(
          groovy(
            """
              final def accept(final def Object m) {
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                J.MethodDeclaration accept = (J.MethodDeclaration) cu.getStatements().getFirst();
                assertThat(accept.getReturnTypeExpression()).isNull();
            })
          )
        );
    }
}
