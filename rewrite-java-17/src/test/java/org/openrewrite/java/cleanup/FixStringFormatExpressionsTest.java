package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("ALL")
public class FixStringFormatExpressionsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FixStringFormatExpressions());
    }

    @Test
    void newLineFormat() {
        rewriteRun(
          java(
            //language=java
            """
            class T {
                static {
                    String s = String.format("hello world\\n%s", "again");
                    String s2 = "hello world\\n%s".formatted("again");
                }
            }
            ""","""
            class T {
                static {
                    String s = String.format("hello world%n%s", "again");
                    String s2 = "hello world%n%s".formatted("again");
                }
            }
            """)
        );
    }

    @Test
    void trimUnusedArguments() {
        rewriteRun(
          //language=java
          java("""
            class T {
                static {
                    String s = String.format("count: %d, %d, %d, %d", 1, 3, 2, 4, 5);
                    String f = "count: %d, %d, %d, %d".formatted(1, 3, 2, 4, 5);
                }
            }
            """,
            """
            class T {
                static {
                    String s = String.format("count: %d, %d, %d, %d", 1, 3, 2, 4);
                    String f = "count: %d, %d, %d, %d".formatted(1, 3, 2, 4);
                }
            }
            """)
        );
    }

    @Test
    void allArgsAreUsed() {
        rewriteRun(
          //language=java
          java("""
            class T {
                static {
                    String s = String.format("count: %d, %d, %d, %d", 1, 3, 2, 4);
                    String f = "count: %d, %d, %d, %d".formatted(1, 3, 2, 4);
                }
            }
            """)
        );
    }


}
