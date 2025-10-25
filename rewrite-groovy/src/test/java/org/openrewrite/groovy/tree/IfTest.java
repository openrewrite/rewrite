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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

@SuppressWarnings({"GroovyIfStatementWithIdenticalBranches", "GroovyConstantIfStatement", "GrUnnecessarySemicolon", "GroovyUnusedIncOrDec", "GroovyEmptyStatementBody"})
class IfTest implements RewriteTest {

    @Test
    void ifElse() {
        rewriteRun(
          groovy(
            """
              int n = 0
              if(n == 0) {
              }
              else if(n == 1) {
              }
              else {
              }
              """
          )
        );
    }

    @Test
    void noElse() {
        rewriteRun(
          groovy(
            """
              int n = 0;
              if (n == 0) {
              }
              """
          )
        );
    }

    @Test
    void singleLineIfElseStatements() {
        rewriteRun(
          groovy(
            """
              int n = 0;
              if (n == 0) n++;
              else if (n == 1) n++;
              else n++;
              """
          )
        );
    }

    @SuppressWarnings("TrailingWhitespacesInTextBlock")
    @Test
    void elseWithTrailingSpace() {
        rewriteRun(
          groovy(
            """
              if (true) {
              }
              else{
              }
              """
          )
        );
    }
}
