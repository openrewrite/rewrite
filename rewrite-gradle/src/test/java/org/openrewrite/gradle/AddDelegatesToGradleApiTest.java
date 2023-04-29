/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.gradle;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class AddDelegatesToGradleApiTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion().classpath("groovy", "gradle-base-services"))
          .recipe(new AddDelegatesToGradleApi());
    }

    @DocumentExample
    @Test
    void simpleMethod() {
        rewriteRun(
          java(
            """
              package org.gradle.example;
                            
              import groovy.lang.Closure;
              import org.gradle.api.Action;
                            
              class A {
                  void someMethod(Action<String> action) { }
                  void someMethod(Closure action) { }
              }
              """,
            """
              package org.gradle.example;
                            
              import groovy.lang.Closure;
              import groovy.lang.DelegatesTo;
              import org.gradle.api.Action;
                            
              class A {
                  void someMethod(Action<String> action) { }
                  void someMethod(@DelegatesTo(String.class) Closure action) { }
              }
              """
          )
        );
    }

    @Test
    void methodWithBound() {
        rewriteRun(
          java(
            """
              package org.gradle.example;
                            
              import groovy.lang.Closure;
              import org.gradle.api.Action;
                            
              class A {
                  void someMethod(Action<? super String> action) { }
                  void someMethod(Closure action) { }
              }
              """,
            """
              package org.gradle.example;
                            
              import groovy.lang.Closure;
              import groovy.lang.DelegatesTo;
              import org.gradle.api.Action;
                            
              class A {
                  void someMethod(Action<? super String> action) { }
                  void someMethod(@DelegatesTo(String.class) Closure action) { }
              }
              """
          )
        );
    }

    @Test
    void methodWithGenericBound() {
        rewriteRun(
          java(
            """
              package org.gradle.example;
                            
              import groovy.lang.Closure;
              import org.gradle.api.Action;
                            
              class A {
                  <T extends String> void someMethod(Action<? super T> action) { }
                  void someMethod(Closure action) { }
              }
              """,
            """
              package org.gradle.example;
                            
              import groovy.lang.Closure;
              import groovy.lang.DelegatesTo;
              import org.gradle.api.Action;
                            
              class A {
                  <T extends String> void someMethod(Action<? super T> action) { }
                  void someMethod(@DelegatesTo(String.class) Closure action) { }
              }
              """
          )
        );
    }

    @Test
    void dontUnwrapTypesTooMuch() {
        rewriteRun(
          java(
            """
              package org.gradle.example;
                            
              import groovy.lang.Closure;
              import org.gradle.api.Action;
              import java.util.List;
                            
              class A {
                  <T extends String> void someMethod(Action<? super List<? extends T>> action) { }
                  void someMethod(Closure action) { }
              }
              """,
            """
              package org.gradle.example;
                            
              import groovy.lang.Closure;
              import groovy.lang.DelegatesTo;
              import org.gradle.api.Action;
              import java.util.List;
                            
              class A {
                  <T extends String> void someMethod(Action<? super List<? extends T>> action) { }
                  void someMethod(@DelegatesTo(List.class) Closure action) { }
              }
              """
          )
        );
    }

    @Test
    void dontBeConfusedByOtherOverloads() {
        rewriteRun(
          java(
            """
              package org.gradle.example;
                            
              import groovy.lang.Closure;
              import org.gradle.api.Action;
                            
              class A {
                  void someMethod(int i, Action<Integer> action) {}
                  void someMethod(Action<String> action) {}
                  void someMethod(String s, Action<Integer> action) {}
                  void someMethod(Closure action) {}
              }
              """,
            """
              package org.gradle.example;
                            
              import groovy.lang.Closure;
              import groovy.lang.DelegatesTo;
              import org.gradle.api.Action;
                            
              class A {
                  void someMethod(int i, Action<Integer> action) {}
                  void someMethod(Action<String> action) {}
                  void someMethod(String s, Action<Integer> action) {}
                  void someMethod(@DelegatesTo(String.class) Closure action) {}
              }
              """
          )
        );
    }

    @Test
    void leaveUnusableTypeInformationAlone() {
        rewriteRun(
          java(
            """
              package org.gradle.example;
                            
              import groovy.lang.Closure;
              import org.gradle.api.Action;
              import java.util.List;
                            
              class A {
                  <T> void someMethod(Action<T> action) { }
                  void someMethod(Closure action) { }
              }
              """
          )
        );
    }

    @Test
    void commentSaysNoDelegate() {
        rewriteRun(
          java(
            """
              package org.gradle.example;
                            
              import groovy.lang.Closure;
              import org.gradle.api.Action;
                            
              class A {
                  void someMethod(Action<String> action) { }
                  /**
                   * The {@link String} is passed to the closure as a parameter.
                   * @param action action
                   */
                  void someMethod(Closure action) { }

                  void anotherMethod(Action<String> action) { }

                  /**
                   * The {@link String} is the delegate and also passed to the closure as a parameter
                   * @param action action
                   */
                  void anotherMethod(Closure action) { }
              }
              """,
            """
              package org.gradle.example;
                            
              import groovy.lang.Closure;
              import groovy.lang.DelegatesTo;
              import org.gradle.api.Action;
                            
              class A {
                  void someMethod(Action<String> action) { }
                  /**
                   * The {@link String} is passed to the closure as a parameter.
                   * @param action action
                   */
                  void someMethod(Closure action) { }

                  void anotherMethod(Action<String> action) { }

                  /**
                   * The {@link String} is the delegate and also passed to the closure as a parameter
                   * @param action action
                   */
                  void anotherMethod(@DelegatesTo(String.class) Closure action) { }
              }
              """
          )
        );
    }
}
