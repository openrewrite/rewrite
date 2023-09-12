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
package org.openrewrite.groovy.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

@SuppressWarnings("GroovyUnusedAssignment")
class LambdaTest implements RewriteTest {

    @Test
    void lambdaExpressionNoParens() {
        rewriteRun(
          groovy("""
            def lambda = a -> a
            """)
        );
    }

    @Test
    void lambdaExpressionNoArguments() {
        rewriteRun(
          groovy("""
            ( ) -> arg
            """)
        );
    }
    @Test
    void lambdaExpressionWithArgument() {
        rewriteRun(
          groovy("""
            ( String arg ) -> arg
            """)
        );
    }

    @Test
    void closureReturningLambda() {
        rewriteRun(
          groovy("""
            def foo(Closure cl) {}
            foo { String a ->
                ( _ ) -> a
            }
            """)
        );
    }

    @Test
    void closureParameterWithType() {
        rewriteRun(
          groovy("""
            class A {}
            def foo(Closure cl) {}
            foo { A a ->
                a
                a
            }
            """)
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2168")
    @Test
    void closureNoArguments() {
        rewriteRun(
          groovy(
            """
              def f1 = { -> 1 }
              def f2 = { 1 }
              def f3 = { -> }
              """
          )
        );
    }
}
