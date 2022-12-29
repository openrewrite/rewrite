package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.kotlin.tree.ParserAsserts.isFullyParsed;

public class LambdaTest implements RewriteTest {

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

    @Disabled("Implement lambda type references.")
    @Test
    void invokedLambda() {
        rewriteRun(
          kotlin("""
                fun invokeLambda(lambda: (Double) -> Boolean): Boolean {
                    return lambda(1.0)
                }
              """,
            isFullyParsed()
          )
        );
    }
}
