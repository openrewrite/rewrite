/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.groovy;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

class GroovySlf4jTest implements RewriteTest {

    @Test
    void slf4jWithoutArguments() {
        rewriteRun(
          groovy(
            """
              import groovy.util.logging.Slf4j
              @Slf4j
              class A {
                void method() {
                     log.info("Hello World")
                }
              }
              """
          )
        );
    }

    @Test
    void slf4jWithCConcatenation() {
        rewriteRun(
          groovy(
            """
              import groovy.util.logging.Slf4j
              @Slf4j
              class A {
                void method() {
                     def a = "A"
                     log.info("Hello World" + A)
                }
              }
              """
          )
        );
    }

    @Test
    void slf4jWithStringTemplate() {
        rewriteRun(
          groovy(
            """
              import groovy.util.logging.Slf4j
              @Slf4j
              class A {
                void method() {
                     def a = "A"
                     log.info("Hello World ${A}")
                }
              }
              """
          )
        );
    }

    @Test
    void slf4jWithDifferentLogLevel() {
        rewriteRun(
          groovy(
            """
              import groovy.util.logging.Slf4j
              @Slf4j
              class A {
                void method() {
                     def msg = "World"
                     log.warn("Hello " + msg)
                }
              }
              """
          )
        );
    }

    @Test
    void slf4jWithMultipleCalls() {
        rewriteRun(
          groovy(
            """
              import groovy.util.logging.Slf4j
              @Slf4j
              class A {
                void method() {
                     def a = "A"
                     log.info("Hello " + a)
                     log.debug("World ${a}")
                }
              }
              """
          )
        );
    }

}
