package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.tree.ParserAssertions.kotlin;

public class ThrowTest implements RewriteTest {

    @Test
    void returnValue() {
        rewriteRun(
          kotlin(
            """
            fun method ( ) {
                throw IllegalArgumentException ( "42" )
            }
            """
          )
        );
    }
}
