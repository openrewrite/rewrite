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

import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RewriteTest;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class MethodDeclarationTest implements RewriteTest {

    @Test
    void defaultValue() {
        rewriteRun(
          java(
            """
              public @interface A {
                  String foo() default "foo";
              }
              """
          )
        );
    }

    @Test
    void constructor() {
        rewriteRun(
          java(
            """
              public class A {
                  public A() { }
              }
              """
          )
        );
    }

    @Test
    void typeArguments() {
        rewriteRun(
          java(
            """
              class Test {
                  public <P, R> R foo(P p, String s, String... args) {
                      return null;
                  }
              }
              """
          )
        );
    }

    @Test
    void parameterAnnotations() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().classpath("jspecify")),
          java(
            """
              import org.jspecify.annotations.Nullable;
              
              class Test {
                  public @Nullable Object foo(@Nullable String s) {
                      return s;
                  }
              }
              """,
            sourceSpecs -> sourceSpecs.afterRecipe(cu -> {
                J.MethodDeclaration foo = (J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0);
                assertTypeIsAnnotatedAs((JavaType.Class) foo.getReturnTypeExpression().getType(), "org.jspecify.annotations.Nullable");
                assertTypeIsAnnotatedAs(((JavaType.Class) foo.getMethodType().getReturnType()), "org.jspecify.annotations.Nullable");
                assertTypeIsAnnotatedAs((JavaType.Class) ((J.VariableDeclarations) foo.getParameters().get(0)).getVariables().get(0).getType(), "org.jspecify.annotations.Nullable");
                assertTypeIsAnnotatedAs(((JavaType.Class) foo.getMethodType().getParameterTypes().get(0)), "org.jspecify.annotations.Nullable");
            })
          )
        );
    }

    private static void assertTypeIsAnnotatedAs(JavaType.Class type, String... annotations) {
        List<ThrowingConsumer<JavaType.FullyQualified>> array = Stream.of(annotations)
          .<ThrowingConsumer<JavaType.FullyQualified>>map(ann -> a -> assertThat(a.getFullyQualifiedName()).isEqualTo(ann))
          .toList();
        //noinspection unchecked
        assertThat(type.getAnnotations()).satisfiesExactly(array.toArray(new ThrowingConsumer[0]));
    }

    @Test
    void interfaceMethodDecl() {
        rewriteRun(
          java(
            """
              public interface A {
                  String getName() ;
              }
              """
          )
        );
    }

    @SuppressWarnings("RedundantThrows")
    @Test
    void methodThrows() {
        rewriteRun(
          java(
            """
              class Test {
                  public void foo()  throws Exception { }
              }
              """
          )
        );
    }

    @Test
    void nativeModifier() {
        rewriteRun(
          java(
            """
              class Test {
                  public native void foo();
              }
              """
          )
        );
    }

    @Test
    void methodWithSuffixMultiComment() {
        rewriteRun(
          java(
            """
              class Test {
                  public void foo() { }/*Comments*/
              }
              """
          )
        );
    }

}
