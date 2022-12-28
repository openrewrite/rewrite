package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.kotlin.tree.ParserAsserts.isFullyParsed;

public class LambdaTest implements RewriteTest {

    @Disabled("Requires fix for whitespace at end of body.")
    @Test
    void binaryExpressionAsBody() {
        rewriteRun(
          kotlin("""
                fun method() {
                    val square = { number: Int -> number * number }
                }
              """,
            isFullyParsed()
          )
        );
    }
}
