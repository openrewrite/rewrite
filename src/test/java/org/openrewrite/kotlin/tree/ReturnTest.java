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
package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

@SuppressWarnings({"RedundantUnitReturnType", "CatchMayIgnoreException", "ConstantConditionIf"})
class ReturnTest implements RewriteTest {

    @Test
    void returnValue() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) : String {
                  return "42"
              }
              """
          )
        );
    }

    @Test
    void implicitReturn() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) : String {
                  "42"
              }
              """
          )
        );
    }

    @Test
    void returnUnit() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) : Unit {
                  return
              }
              """
          )
        );
    }

    @Test
    void whenExpression() {
        rewriteRun(
          kotlin(
            """
              fun method ( i : Int ) : String {
                  return when {
                      i . mod ( 2 ) == 0 -> "even"
                      else -> "odd"
                  }
              }
              """
          )
        );
    }

    @Test
    void returnStatement() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) : Unit {
                  return try {
                  } catch ( e : Exception ) {
                  }
              }
              """
          )
        );
    }

    @Test
    void conditionalReturnedValue() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) : String {
                  return if ( true ) "42" else "24"
              }
              """
          )
        );
    }

    @Test
    void returnLabel() {
        rewriteRun(
          kotlin(
            """
              fun foo(ints: List<Int>) {
                  ints.forEach {
                      if (it == 0) return@forEach
                      print(it)
                  }
              }
              """
          )
        );
    }

    @Test
    void returnLabel_2() {
        rewriteRun(
          kotlin(
            """
              val x = arrayOf(1)
                  .single {
                      return@single false
                  }
              """
          )
        );
    }

    @Test
    void trailingSemiColon() {
        rewriteRun(
          kotlin(
            """
              fun id(): Int {
                  return 42   ;
              }
              """
          )
        );
    }
}
