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
package org.openrewrite.javascript.tree;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.javascript.Assertions.javaScript;

@SuppressWarnings("ALL")
class ForLoopTest implements RewriteTest {

    @Test
    void forLoop() {
        rewriteRun(
          javaScript(
            """
              for ( let i = 0 ; i < 3 ; i++ ) {
              }
              """
          )
        );
    }

    @Test
    void multiDeclarationForLoop() {
        rewriteRun(
          javaScript(
            """
              for ( let i = 0 , j = 1 , k = 2 ; i < 10 ; i++ , j *= 2 , k += 2 ) {
              }
              """
          )
        );
    }

    @Test
    void forOfLoop() {
        rewriteRun(
          javaScript(
            """
              let arr = [ 10 , 20 , 30 , 40 ] ;
              for ( var val of arr ) {
              }
              """
          )
        );
    }

    @Test
    void forInLoop() {
        rewriteRun(
          javaScript(
            """
              let arr = [ 10 , 20 , 30 , 40 ] ;
              for ( var val in arr ) {
              }
              """
          )
        );
    }

    @ExpectedToFail("The const declaration name returns an ObjectBindingPattern.")
    @Test
    void destruct() {
        rewriteRun(
          javaScript(
            """
              for ( const { a , b } of [ { a : 1 , b : 2 } , { a : 3 , b : 4 } ] ) {
              }
              """
          )
        );
    }
}
