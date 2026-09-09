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
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.groovy.Assertions.groovy;

@SuppressWarnings({"GroovyEmptyStatementBody", "GroovyInfiniteLoopStatement"})
class WhileLoopTest implements RewriteTest {

    @Test
    void whileLoop() {
        rewriteRun(
          groovy(
            """
              while ( true ) { }
              """
          )
        );
    }

    @Test
    void statementTerminatorForSingleLineWhileLoops() {
        rewriteRun(
          groovy(
            """
              while(true) test()
              """
          )
        );
    }

    @Test
    void emptyBodyWhileLoop() {
        rewriteRun(
          groovy(
            """
              int i = 0
              while (i++ < 10);
              """,
            spec -> spec.afterRecipe(cu -> new GroovyIsoVisitor<Integer>() {
                @Override
                public J.WhileLoop visitWhileLoop(J.WhileLoop whileLoop, Integer p) {
                    assertThat(whileLoop.getBody()).isInstanceOf(J.Empty.class);
                    return super.visitWhileLoop(whileLoop, p);
                }
            }.visit(cu, 0))
          )
        );
    }

    @Test
    void emptyBodyDoWhileLoop() {
        rewriteRun(
          groovy(
            """
              int i = 0
              do; while (i++ < 10)
              """,
            spec -> spec.afterRecipe(cu -> new GroovyIsoVisitor<Integer>() {
                @Override
                public J.DoWhileLoop visitDoWhileLoop(J.DoWhileLoop doWhileLoop, Integer p) {
                    assertThat(doWhileLoop.getBody()).isInstanceOf(J.Empty.class);
                    return super.visitDoWhileLoop(doWhileLoop, p);
                }
            }.visit(cu, 0))
          )
        );
    }
}
