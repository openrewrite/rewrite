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
package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

@SuppressWarnings("UnusedReceiverParameter")
class ImportTest implements RewriteTest {

    @Test
    void jdkImport() {
        rewriteRun(
          kotlin("import java.util.ArrayList")
        );
    }

    @Test
    void kotlinImport() {
        rewriteRun(
          kotlin("import kotlin.collections.List")
        );
    }

    @Test
    void wildCard() {
        rewriteRun(
          kotlin("import kotlin.collections.*")
        );
    }

    @Test
    void inlineImport() {
        rewriteRun(
          kotlin(
            """
              package a.b
              class Target {
                  inline fun method ( ) { }
              }
              """
          ),
          kotlin(
            """
              import a.b.method
              
              class A {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/158")
    @Test
    void methodName() {
        rewriteRun(
          kotlin("fun <T : Any> Class<T>.createInstance() {}"),
          kotlin("import createInstance")
        );
    }

    @Test
    void alias() {
        rewriteRun(
          kotlin(
            """
              import kotlin.collections.List as L
              import kotlin.collections.Set as S
              
              class T
              """)
        );
    }

    @Test
    void aliasFieldAccess() {
        rewriteRun(
          kotlin(
            """
              import java.lang.Integer as Number
              var max = Number.MAX_VALUE
              """
          )
        );
    }
}
