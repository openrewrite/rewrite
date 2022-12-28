package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.kotlin.tree.ParserAsserts.isFullyParsed;

public class LambdaTest implements RewriteTest {

    @Disabled("Requires function call and Convert PSI.")
    @Test
    void function() {
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
