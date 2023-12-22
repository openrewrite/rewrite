package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class UnknownJavadocTagTest implements RewriteTest {
    @Test
    void javadocUnknownTagWithOpeningAndClosingBrace() {
        rewriteRun(
          java(
            """
              interface Test {
              	/**
              	 * {@return 42}
              	 */
              	int foo();
              }
              """,
            """
              interface Test {
              	/**
              	 * {@return 42}
              	 */
              	int foo();
              }
              """
          )
        );
    }

    @Test
    void javadocUnknownTagWithOpeningBraceOnly() {
        rewriteRun(
          java(
            """
              interface Test {
              	/**
              	 * {@return 42
              	 */
              	int foo();
              }
              """,
            """
              interface Test {
              	/**
              	 * {@return 42
              	 */
              	int foo();
              }
              """
          )
        );
    }
}
