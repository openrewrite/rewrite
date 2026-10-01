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
import org.openrewrite.Issue;
import org.openrewrite.ParseExceptionResult;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.MinimumJava11;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.tree.ParseError;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

@SuppressWarnings({"RedundantOperationOnEmptyContainer", "ResultOfMethodCallIgnored", "Convert2MethodRef"})
class LambdaTest implements RewriteTest {

    @Test
    void lambda() {
        rewriteRun(
          java(
            """
              import java.util.function.Function;
              class Test {
                  void test() {
                      Function<String, String> func = (String s) -> "";
                  }
              }
              """
          )
        );
    }

    @Test
    void untypedLambdaParameter() {
        rewriteRun(
          java(
            """
              import java.util.*;
              class Test {
                  void test() {
                      List<String> list = new ArrayList<>();
                      list.stream().filter(s -> s.isEmpty());
                  }
              }
              """
          )
        );
    }

    @Test
    void optionalSingleParameterParentheses() {
        rewriteRun(
          java(
            """
              import java.util.*;
              class Test {
                  void test() {
                      List<String> list = new ArrayList<>();
                      list.stream().filter((s) -> s.isEmpty());
                  }
              }
              """
          )
        );
    }

    @Test
    void rightSideBlock() {
        rewriteRun(
          java(
            """
              public class A {
                  Action a = ( ) -> { };
              }

              interface Action {
                  void call();
              }
              """
          )
        );
    }

    @Test
    void multipleParameters() {
        rewriteRun(
          java(
            """
              import java.util.function.BiConsumer;
              class Test {
                  void test() {
                      BiConsumer<String, String> a = (s1, s2) -> { };
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/8712")
    @Test
    void untypedLambdaParameterInSuperWithUnresolvedSupertype() {
        J.CompilationUnit cu = parseRoundTrip(unresolvedSuper("processBuilder -> {}"));
        assertInferredLambdaParameter(cu, "processBuilder");
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/8712")
    @Test
    void untypedLambdaParameterVariantsWithUnresolvedSupertype() {
        String[][] cases = {
          {"processBuilder", "(processBuilder) -> {}"},
          {"processBuilder", "/*c*/ processBuilder /*d*/ -> {}"},
          {"processBuilder", "( /*c*/ processBuilder /*d*/ ) -> {}"},
          {"processBuilder", "/* var */ processBuilder -> {}"},
          {"var", "var -> {}"},
          {"var", "(var) -> {}"},
          {"var", "  var  -> {}"},
          {"var", "(  var  ) -> {}"},
          {"vari", "vari -> {}"},
          {"vari", "(vari) -> {}"},
          {"varprocessBuilder", "varprocessBuilder -> {}"},
          {"varprocessBuilder", "(varprocessBuilder) -> {}"}
        };
        for (String[] c : cases) {
            J.CompilationUnit cu = parseRoundTrip(unresolvedSuper(c[1]));
            assertInferredLambdaParameter(cu, c[0]);
        }
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/8712")
    @Test
    void untypedLambdaInSuperWhenSupertypeIsDeclared() {
        String source = """
          package com.example.foo.bar;

          interface BazFn {
              void go(Object o);
          }

          class Foo {
              Foo(BazFn fn) {}
          }

          class Baz {
              static class Bar extends Foo {
                  Bar() {
                      super(processBuilder -> {});
                  }
              }
          }
          """;
        J.CompilationUnit cu = parseRoundTrip(source);
        J.VariableDeclarations parameter = assertInferredLambdaParameter(cu, "processBuilder");
        assertThat(parameter.getVariables().get(0).getVariableType()).isNotNull();
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/8712")
    @Test
    void explicitFunctionalInterfaceCastWithUnresolvedSupertype() {
        J.CompilationUnit cu = parseRoundTrip(unresolvedSuper("(BazFn) (processBuilder -> {})"));
        assertInferredLambdaParameter(cu, "processBuilder");
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/8712")
    @Test
    void explicitlyTypedLambdaParameterInSuperWithUnresolvedSupertype() {
        J.CompilationUnit cu = parseRoundTrip(unresolvedSuper("(Object processBuilder) -> {}"));
        J.VariableDeclarations parameter = singleLambdaParameter(cu);
        assertThat(parameter.getTypeExpression()).isInstanceOf(J.Identifier.class);
        assertThat(((J.Identifier) parameter.getTypeExpression()).getSimpleName()).isEqualTo("Object");
        assertThat(parameter.getTypeExpression().getMarkers().findFirst(JavaVarKeyword.class)).isEmpty();
        assertThat(parameter.getVariables().get(0).getSimpleName()).isEqualTo("processBuilder");
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/8712")
    @MinimumJava11
    @Test
    void explicitVarLambdaParameterInSuperWithUnresolvedSupertype() {
        String[] lambdas = {
          "(var processBuilder) -> {}",
          "(var /*c*/ processBuilder) -> {}",
          "(  var   processBuilder  ) -> {}"
        };
        for (String lambda : lambdas) {
            J.CompilationUnit cu = parseRoundTrip(unresolvedSuper(lambda));
            J.VariableDeclarations parameter = singleLambdaParameter(cu);
            assertThat(parameter.getTypeExpression()).isInstanceOf(J.Identifier.class);
            J.Identifier typeExpression = (J.Identifier) parameter.getTypeExpression();
            assertThat(typeExpression.getSimpleName()).isEqualTo("var");
            assertThat(typeExpression.getMarkers().findFirst(JavaVarKeyword.class)).isPresent();
            assertThat(parameter.getVariables().get(0).getSimpleName()).isEqualTo("processBuilder");
        }
    }

    private static String unresolvedSuper(String lambda) {
        return String.format("""
          package com.example.foo.bar;

          class Baz {
              static class Bar extends Foo {
                  Bar() {
                      super(%s);
                  }
              }
          }
          """, lambda);
    }

    private static J.CompilationUnit parseRoundTrip(String source) {
        SourceFile parsed = JavaParser.fromJavaVersion()
          .build()
          .parse(source)
          .findFirst()
          .orElseThrow();
        assertThat(parsed)
          .as(parseFailureDescription(parsed))
          .isInstanceOf(J.CompilationUnit.class);
        assertThat(parsed.printAll()).isEqualTo(source);
        return (J.CompilationUnit) parsed;
    }

    private static String parseFailureDescription(SourceFile parsed) {
        if (!(parsed instanceof ParseError)) {
            return parsed.getClass().getName();
        }
        ParseError parseError = (ParseError) parsed;
        String message = parseError.getMarkers().findFirst(ParseExceptionResult.class)
          .map(ParseExceptionResult::getMessage)
          .orElse("");
        SourceFile erroneous = parseError.getErroneous();
        return message + "\n" + (erroneous == null ? "<no erroneous tree>" : erroneous.printAll());
    }

    private static J.VariableDeclarations assertInferredLambdaParameter(J.CompilationUnit cu, String simpleName) {
        J.VariableDeclarations parameter = singleLambdaParameter(cu);
        assertThat(parameter.getTypeExpression()).isNull();
        assertThat(parameter.getVariables().get(0).getSimpleName()).isEqualTo(simpleName);
        return parameter;
    }

    private static J.VariableDeclarations singleLambdaParameter(J.CompilationUnit cu) {
        List<J.VariableDeclarations> parameters = new ArrayList<>();
        new JavaIsoVisitor<Integer>() {
            @Override
            public J.Lambda visitLambda(J.Lambda lambda, Integer p) {
                for (J parameter : lambda.getParameters().getParameters()) {
                    if (parameter instanceof J.VariableDeclarations) {
                        parameters.add((J.VariableDeclarations) parameter);
                    }
                }
                return super.visitLambda(lambda, p);
            }
        }.visit(cu, 0);
        assertThat(parameters).hasSize(1);
        return parameters.get(0);
    }
}
