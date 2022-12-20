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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.kotlin.tree.ParserAsserts.isFullyParsed;

@SuppressWarnings("GrUnnecessaryPublicModifier")
class ClassDeclarationTest implements RewriteTest {

    @Test
    void multipleClassDeclarationsInOneCompilationUnit() {
        rewriteRun(
          kotlin(
            """
              class A {}
              class B {}
            """,
            isFullyParsed()
          )
        );
    }

    @Test
    void empty() {
        rewriteRun(
          kotlin(
            """
              class A
              class B
            """,
            isFullyParsed()
          )
        );
    }

    @Disabled("Fix type attribution on super types.")
    @Test
    void classImplements() {
        rewriteRun(
          kotlin(
            """
              interface A
              class C : A
            """,
            isFullyParsed()
          )
        );
    }

    @Disabled("Fix type attribution on super types.")
    @Test
    void classExtends() {
        rewriteRun(
          kotlin(
            """
              class A {}
              class B : A() {}
            """,
            isFullyParsed()
          )
        );
    }

    @Test
    void modifierOrdering() {
        rewriteRun(
          kotlin(
            """
              public abstract class A
            """,
            isFullyParsed()
          )
        );
    }

    @Disabled("Requires supporting Kotlin modifiers.")
    @Test
    void annotationClass() {
        rewriteRun(
          kotlin(
            """
              annotation class A
            """,
            isFullyParsed()
          )
        );
    }

    @Disabled("Requires supporting Kotlin modifiers.")
    @Test
    void enumClass() {
        rewriteRun(
          kotlin(
            """
              enum class A
            """,
            isFullyParsed()
          )
        );
    }
}
