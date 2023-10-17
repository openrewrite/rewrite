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

class NewClassTest implements RewriteTest {

    @Test
    void multipleParameters() {
        rewriteRun(
          kotlin("class Test ( val a : Int , val b : Int )"),
          kotlin("val t = Test ( 1 , 2 )")
        );
    }

    @Test
    void anonymousClass() {
        rewriteRun(
          kotlin(
            """
              open class Test ( val a: Int, val b: Int ) {
                  open fun base() : Boolean {
                      return false
                  }
              }
              """
          ),
          kotlin(
            """
              val t = object : Test ( 1 , 2 ) {
                  override fun base ( ) : Boolean {
                      return true
                  }
              }
              """
          )
        );
    }

    @Test
    void fullyQualified() {
        rewriteRun(
          kotlin(
            """
              val type : java . util . ArrayList<String> = java . util . ArrayList<String> ( )
              """
          )
        );
    }

    @Test
    void innerClass() {
        rewriteRun(
          kotlin(
            """
              val type : java . util . AbstractMap . SimpleEntry<String, String> = java . util . AbstractMap . SimpleEntry<String, String> ( "", "" )
              """
          )
        );
    }

    @Test
    void conditionalConstructorArg() {
        rewriteRun(
          kotlin("class Test ( val a : Int )"),
          kotlin("val t = Test ( if ( true ) 4 else 2 )")
        );
    }
}
