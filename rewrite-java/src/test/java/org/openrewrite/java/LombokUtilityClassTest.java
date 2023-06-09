package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class LombokUtilityClassTest implements RewriteTest {

    @Test
    void happyPath1() {
        rewriteRun(
                recipeSpec -> recipeSpec
                        .recipe(new LombokUtilityClass()
                        ),
                java(
                        """
                                public class A {
                                   public static int add(final int x, final int y) {
                                      return x + y;
                                   }
                                }
                                """,
                        """
                                import lombok.experimental.UtilityClass;
                                                                
                                @UtilityClass
                                public class A {
                                   public int add(final int x, final int y) {
                                      return x + y;
                                   }
                                }
                                """
                )
        );
    }


    @Test
    void doNotUpgradeToUtilityClassIfNonStaticVariables() {
        rewriteRun(
                recipeSpec -> recipeSpec
                        .recipe(new LombokUtilityClass()),
                java(
                        """
                                public class A {
                                   private final int x = 0;
                                   public static int add(final int x, final int y) {
                                      return x + y;
                                   }
                                }
                                """
                )
        );
    }


    @Test
    void doNotUpgradeToUtilityClassIfNonStaticMethods() {
        rewriteRun(
                recipeSpec -> recipeSpec
                        .recipe(new LombokUtilityClass()),
                java(
                        """
                                public class A {
                                   public int add(final int x, final int y) {
                                      return x + y;
                                   }
                                }
                                """
                )
        );
    }
}