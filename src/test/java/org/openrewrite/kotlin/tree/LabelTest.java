package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.tree.ParserAssertions.kotlin;

public class LabelTest implements RewriteTest {

    @Test
    void anonymousFunction() {
        rewriteRun(
          kotlin(
            """
                fun foo() {
                    run loop@ {
                        listOf ( 1 , 2 , 3 , 4 , 5 ) . forEach {
                            if ( it == 3 ) return@loop
                            println ( it )
                        }
                    }
                }
            """
          )
        );
    }
    @Test
    void breakFromLabeledWhileLoop() {
        rewriteRun(
          kotlin(
            """
                fun method ( ) {
                    labeled@ while ( true ) {
                        break@labeled
                    }
                }
              """
          )
        );
    }

    @Test
    void continueFromLabeledWhileLoop() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  fun test ( ) {
                      labeled@ while ( true ) continue@labeled
                  }
              }
              """
          )
        );
    }

    @Test
    void doWhileLoop() {
        rewriteRun(
          kotlin(
            """
                  fun test ( ) {
                      var i = 0
                      labeled@ do {
                          i++
                          break@labeled
                      } while ( i < 10 )
                  }
              """
          )
        );
    }

    @Test
    void forLoop() {
        rewriteRun(
          kotlin(
            """
                fun test ( ) {
                    labeled@ for (i in 1..10) {
                        break@labeled
                    }
                }
            """
          )
        );
    }
}
