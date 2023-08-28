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

@SuppressWarnings({"ControlFlowWithEmptyBody", "RemoveForLoopIndices"})
class ForLoopTest implements RewriteTest {

    @Test
    void inList() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                  val l = listOf ( 1 , 2 , 3 )
                  for ( i in l ) {
                      println ( i )
                  }
              }
              """
          )
        );
    }

    @Test
    void inMap() {
        rewriteRun(
          kotlin(
            """
              fun method() {
                  val map = mapOf ( 1 to "one" , 2 to "two" , 3 to "three" )
                  for ( ( key , value ) in map ) {
                      print ( key )
                      print ( ", " )
                      println ( value )
                  }
              }
              """
          )
        );
    }

    @Test
    void inRange() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                  for ( i in 1 .. 42 ) {
                      println ( i )
                  }
              }
              """
          )
        );
    }

    @Test
    void rangeUntil() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                  for ( i in 1 ..< 42 ) {
                      println ( i )
                  }
              }
              """
          )
        );
    }

    @Test
    void arrayWithIndex() {
        rewriteRun(
          kotlin(
            """
              fun method ( array : Array < Int > ) {
                  for (  (   index    ,    value   )  in array . withIndex ( ) ) {
                  }
              }
              """
          )
        );
    }

    @Test
    void downToWithStep() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                  for ( i in 6 downTo 0 step 2 ) {
                  }
              }
              """
          )
        );
    }
}
