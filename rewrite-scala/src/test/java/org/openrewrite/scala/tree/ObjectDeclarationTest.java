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
package org.openrewrite.scala.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.scala.Assertions.scala;

class ObjectDeclarationTest implements RewriteTest {

    @Test
    void singletonObject() {
        rewriteRun(
            scala(
                """
                object MySingleton
                """
            )
        );
    }

    @Test
    void objectWithBody() {
        rewriteRun(
            scala(
                """
                object Utils {
                  def helper(): String = "Hello"
                  val constant = 42
                }
                """
            )
        );
    }

    @Test
    void objectExtendingClass() {
        rewriteRun(
            scala(
                """
                object MyObject extends BaseClass
                """
            )
        );
    }

    @Test
    void objectWithTraits() {
        rewriteRun(
            scala(
                """
                object MyService extends Service with Logging with Monitoring
                """
            )
        );
    }

    @Test
    void privateObject() {
        rewriteRun(
            scala(
                """
                private object InternalUtils
                """
            )
        );
    }

    @Test
    void companionObject() {
        rewriteRun(
            scala(
                """
                class Person(name: String)
                
                object Person {
                  def apply(name: String): Person = new Person(name)
                }
                """
            )
        );
    }

    @Test
    void caseObject() {
        rewriteRun(
            scala(
                """
                case object EmptyList
                """
            )
        );
    }

    @Test
    void nestedObject() {
        rewriteRun(
            scala(
                """
                class Outer {
                  object Inner {
                    val x = 1
                  }
                }
                """
            )
        );
    }
}