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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.python.Assertions.python;

@SuppressWarnings("PyUnresolvedReferences")
class MethodInvocationTest implements RewriteTest {

    @Disabled
//    @ParameterizedTest
    // language=py
    @ValueSource(strings = {
      "print", "print ",
      "print 42", "print 42 ", "print 1, 2, 3, 4",
      "print 1, 2, 3, 4 ", "print 1 , 2 , 3 , 4",
      "print 1, 2, 3, 4",
      """
        for x in range(1,11):
            print '{0:2d} {1:3d} {2:4d}'.format(x, x*x, x*x*x)
        """,
    })
    void python2Print(@Language("py") String print) {
        rewriteRun(python(print));
    }

    @ParameterizedTest
    //language=py
    @ValueSource(strings = {
      "print()",
      "print( )",
      "print(42)",
      "print( 42 )",
      "print(1, 2, 3, 4)",
      "print( 1, 2, 3, 4 )",
      "print(1 , 2 , 3 , 4)",
      "print(1, 2, 3, 4, sep='+')",
      "print(1, 2, a=1, b=2)",
      "print(1, 2, a =1, b =2)",
      "print(1, 2, a= 1, b= 2)",
    })
    void print(@Language("py") String print) {
        rewriteRun(python(print));
    }

    @ParameterizedTest
    //language=py
    @ValueSource(strings = {
      "int.bit_length(42)",
      "int .bit_length(42)",
      "int. bit_length(42)",
      "int.bit_length (42)",
      "int.bit_length( 42)",
      "int.bit_length(42 )",
    })
    void qualifiedTarget(@Language("py") String arg) {
        rewriteRun(python(arg));
    }

    @ParameterizedTest
    //language=py
    @ValueSource(strings = {
      "list().copy()",
      "list() .copy()",
      "list(). copy()",
      "list().copy ()",
      "list().copy( )",
    })
    void methodInvocationOnExpressionTarget(@Language("py") String arg) {
        rewriteRun(python(arg));
    }

    @ParameterizedTest
    //language=py
    @ValueSource(strings = {
      "(list().copy)()",
      "(list().copy) ()",
      "(list().copy)( )",
      "(list().count)(1)",
      "(list().count) (1)",
      "(list().count)( 1)",
      "(list().count)(1 )",
      "(list().count)(1,2)",
      "(list().count) (1,2)",
      "(list().count)( 1,2)",
      "(list().count)(1 ,2)",
      "(list().count)(1, 2)",
      "(list().count)(1,2 )",
    })
    void methodInvocationOnCallable(@Language("py") String arg) {
        rewriteRun(python(arg));
    }

    @ParameterizedTest
    //language=py
    @ValueSource(strings = {
      "print(*x)",
      "print(* x)",
      "print(**x)",
      "print(** x)",
    })
    void specialArg(@Language("py") String arg) {
        rewriteRun(python(arg));
    }

    @Issue("https://github.com/openrewrite/rewrite-python/issues/39")
    @Test
    void parameterPadding() {
        rewriteRun(
          python(
            """
              class Foo:
                  def foo ( ) :
                      pass
              """
          )
        );
    }
}
