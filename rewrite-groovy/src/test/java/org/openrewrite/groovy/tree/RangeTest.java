/*
 * Copyright 2023 the original author or authors.
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

class RangeTest implements RewriteTest {

    @Test
    void rangeExpression() {
        rewriteRun(
          groovy(
            """
              def a = []
              println(a[0..-2])
              """
          )
        );
    }

    @Test
    void parenthesized() {
        rewriteRun(
          groovy(
            """
              ( 8..19 ).each { majorVersion ->
                if (majorVersion == 9) return
              }
              """
          )
        );
    }

    @Test
    void parenthesizedAndInvokeMethodWithParentheses() {
        rewriteRun(
          groovy(
            """
              (((( 8..19 ))).each { majorVersion ->
                if (majorVersion == 9) return
              })
              """
          )
        );
    }

    @Test
    void exclusiveRightRange() {
        rewriteRun(
          groovy(
            """
              (1..<10).each { }
              def a = 0..<10
              def b = 0 ..< 10
              def c = (-5)..<5
              def d = a[0..<a.size()]
              for (int i = 0; i < (1..<5).size(); i++) { }
              """
          )
        );
    }

    @Test
    void allRangeKinds() {
        rewriteRun(
          groovy(
            """
              def inclusive = 1..10
              def inclusiveSpaces = 1 .. 10
              def inclusiveNegative = -3..3
              def inclusiveReverse = 10..1
              def inclusiveChars = 'a'..'z'
              def exclusiveEnd = 1..<10
              def exclusiveEndSpaces = 1 ..< 10
              def exclusiveEndNegative = (-5)..<5
              def list = [10, 20, 30, 40, 50]
              def slice1 = list[0..2]
              def slice2 = list[0..<list.size()]
              def slice3 = list[1..-1]
              (0..5).each { }
              (0..<5).each { }
              for (i in 1..3) { }
              for (j in 1..<3) { }
              """
          )
        );
    }
}
