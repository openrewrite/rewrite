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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

class MethodInvocationTest implements RewriteTest {
    @Test
    void gradle() {
        rewriteRun(
          groovy(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'org.hibernate:hibernate-core:3.6.7.Final'
                  api 'com.google.guava:guava:23.0'
                  testImplementation 'junit:junit:4.+'
              }
              """
          )
        );
    }

    @Test
    void emptyArgsWithParens() {
        rewriteRun(
          groovy("mavenCentral()")
        );
    }

    @Test
    @SuppressWarnings("GroovyVariableNotAssigned")
    void nullSafeDereference() {
        rewriteRun(
          groovy(
            """
              Map m
              m?.clear()
              """
          )
        );
    }

    @Test
    void mapLiteralFirstArgument() {
        rewriteRun(
          groovy(
            """
              foo(["foo" : "bar"])
              """
          )
        );
    }

    @Test
    void namedArgumentsInDeclaredOrder() {
        rewriteRun(
          groovy(
            """
              def acceptsNamedArguments (Map a, int i) { }
              acceptsNamedArguments(foo: "bar", 1)
              """
          )
        );
    }

    @Test
    void namedArgumentsAfterPositionalArguments() {
        rewriteRun(
          groovy(
            """
              def acceptsNamedArguments (Map a, int i) { }
              acceptsNamedArguments(1, foo: "bar")
              """
          )
        );
    }

    @Test
    void namedArgumentBeforeClosure() {
        rewriteRun(
          groovy(
            """
              def acceptsNamedArguments(Map a, int i, Closure c) {}
              acceptsNamedArguments(1, foo: "bar") { }
              """
          )
        );
    }

    @Test
    void namedArgumentsBeforeClosure() {
        rewriteRun(
          groovy(
            """
              def acceptsNamedArguments(Map a, Closure c) {}
              acceptsNamedArguments(foo: "bar", bar: "baz") { }
              """
          )
        );
    }

    @Test
    void namedArgumentsBetweenPositionalArguments() {
        rewriteRun(
          groovy(
            """
              def acceptsNamedArguments(Map a, int n, int m) { }
              acceptsNamedArguments(1, foo: "bar", 2)
              """
          )
        );
    }

    @Test
    void namedArgumentsAllOverThePlace() {
        rewriteRun(
          groovy(
            """
              def acceptsNamedArguments(Map a, int n, int m) { }
              acceptsNamedArguments(1, foo: "bar", 2, bar: "baz")
              """
          )
        );
    }

    @Test
    @SuppressWarnings("GroovyAssignabilityCheck")
    void closureWithImplicitParameter() {
        rewriteRun(
          groovy(
            """
              def acceptsClosure(Closure cl) {}
              acceptsClosure {
                  println(it)
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1236")
    @Test
    @SuppressWarnings("GroovyAssignabilityCheck")
    void closureWithNamedParameter() {
        rewriteRun(
          groovy(
            """
              def acceptsClosure(Closure cl) {}
              acceptsClosure { foo ->
                  println(foo)
              }
              """
          )
        );
    }

    @Test
    void closureWithNamedParameterAndType() {
        rewriteRun(
          groovy(
            """
              def acceptsClosure(Closure cl) {}
              acceptsClosure { String foo ->
                  println(foo)
              }
              """
          )
        );
    }

    @Test
    void closureArgumentInParens() {
        rewriteRun(
          groovy(
            """
              def acceptsClosure(Closure cl) {}
              acceptsClosure({})
              """
          )
        );
    }

    @Test
    void closureArgumentAfterEmptyParens() {
        rewriteRun(
          groovy(
            """
              def acceptsClosure(Closure cl) {}
              acceptsClosure ( /* () */ ) { /* {} */ }
              """
          )
        );
    }

    @Test
    void closureReturn() {
        rewriteRun(
          groovy(
            """
              foo {
                  return
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2552")
    @Test
    void closureInvocation() {
        rewriteRun(
          groovy(
            """
              def closure = {}
              closure()
              closure.call()
              """
          )
        );
    }

    @Test
    void staticMethodInvocation() {
        rewriteRun(
          groovy(
            """
              class StringUtils {
                static boolean isEmpty(String value) {
                  return value == null || value.isEmpty()
                }

                static void main(String[] args) {
                  isEmpty("")
                }
              }
              """
          )
        );
    }
}
