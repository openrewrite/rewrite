/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python.tree;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.python.Assertions.python;

class AnnotationTest implements RewriteTest {

    @Test
    void decoratorOnFunction() {
        rewriteRun(python(
          """
            @dec
            def f():
                pass
            """
        ));
    }

    @Test
    void decoratorOnClass() {
        rewriteRun(python(
          """
            @dec
            class C:
                pass
            """
        ));
    }

    @Test
    void staticMethodDecoratorOnFunction() {
        rewriteRun(python(
          """
            class C:
                @staticmethod
                def f():
                    pass
            """
        ));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                    "", "()", "( )",
                    "(42)", "(42 )", "( 42 )",
                    "(1, 2)", "(1,2)", "(1, 2 )", "( 1, 2 )", "( 1, 2)"
            }
    )
    void decoratorArguments(String args) {
        rewriteRun(python(
          """
            @dec%s
            def f():
                pass
            """.formatted(args)
        ));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                    "class C", "def m()"
            }
    )
    void multipleDecorators(String args) {
        rewriteRun(python(
          """
            @decA
            @decB()
            @decC()
            %s:
                pass
            """.formatted(args)
        ));
    }

    @ParameterizedTest
    @ValueSource(
      strings = {
        "class C", "def m()"
      }
    )
    void qualifiedDecoratorName(String args) {
        rewriteRun(python(
          """
            @a.qualified.name
            %s:
                pass
            """.formatted(args)
        ));
    }

}
