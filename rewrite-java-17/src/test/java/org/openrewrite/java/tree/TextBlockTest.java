package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class TextBlockTest implements RewriteTest {

    @Test
    void textBlock() {
        rewriteRun(
          java(
            """
                public class Test {
                    String s = \"""
                        Hello
                        World
                        \""";
                }
              """
          )
        );
    }
}
