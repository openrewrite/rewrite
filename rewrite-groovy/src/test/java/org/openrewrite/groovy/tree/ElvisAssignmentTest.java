/*
 * Copyright 2025 the original author or authors.
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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

class ElvisAssignmentTest implements RewriteTest {

    @Test
    void simpleElvisAssignment() {
        rewriteRun(
          groovy(
            """
              def x = null
              x ?= 42

              def y = 44
              y ?= 46
              """
          )
        );
    }

    @Test
    void elvisAssignmentWithMethodCall() {
        rewriteRun(
          groovy(
            """
              def value = null
              value ?= computeDefault()

              def computeDefault() {
                  return "Groovy"
              }
              """
          )
        );
    }

    @Test
    void elvisAssignmentWithBinaryExpression() {
        rewriteRun(
          groovy(
            """
              def count = null
              count ?= 10 + 5
              """
          )
        );
    }

    @Test
    void elvisAssignmentToFieldInClass() {
        rewriteRun(
          groovy(
            """
              class A {
                  def name

                  void init() {
                      name ?= "default"
                  }
              }
              """
          )
        );
    }

    @Test
    void elvisAssignmentWithArrayParam() {
        rewriteRun(
          groovy(
            """
              import java.util.HashSet
              import java.util.Set
              import java.util.regex.Pattern

              class A {
                  Set excludes

                  void setExcludes(String... excludes) {
                      excludes ?= new String[0]
                      this.excludes = new HashSet<>(excludes.length)
                      excludes.each { this.excludes.add(Pattern.compile(it)) }
                  }
              }
              """
          )
        );
    }
}
