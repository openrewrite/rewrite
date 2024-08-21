/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class BinaryTest implements RewriteTest {

    @SuppressWarnings("PointlessArithmeticExpression")
    @Test
    void arithmetic() {
        rewriteRun(
          java(
            """
              class Test {
                  void test() {
                  int n = 0 + 1;
                  }
              }
              """
          )
        );
    }

    /**
     * String folding needs to be disabled in the parser to preserve the binary expression in the AST!
     *
     * @see "com.sun.tools.javac.parser.JavacParser.allowStringFolding"
     */
    @Test
    void formatFoldableStrings() {
        rewriteRun(
          java(
            """
              class Test {
                  void test() {
                      String n = "a" + "b";
                  }
              }
              """
          )
        );
    }

    @Test
    void endOfLineBreaks() {
        rewriteRun(
          java(
            """
              import java.util.Objects;

              class Test {
                  void test() {
                      boolean b = Objects.equals(1, 2) //
                                  && Objects.equals(3, 4) //
                                  && Objects.equals(4, 5);
                  }
              }
              """
          )
        );
    }
}
