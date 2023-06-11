package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class LombokUtilityClassTest implements RewriteTest {

    @Test
    void happyPathSimpleMethod() {
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
    void happyPathSimpleField() {
        rewriteRun(
                recipeSpec -> recipeSpec
                        .recipe(new LombokUtilityClass()
                        ),
                java(
                        """
                                public class A {
                                   public static final int C = 0;
                                }
                                """,
                        """
                                import lombok.experimental.UtilityClass;
                                                                
                                @UtilityClass
                                public class A {
                                   public final int c = 0;
                                }
                                """
                )
        );
    }

    @Test
    void happyPathMultiVariableField() {
        rewriteRun(
                recipeSpec -> recipeSpec
                        .recipe(new LombokUtilityClass()
                        ),
                java(
                        """
                                public class A {
                                   public static final int A, B, C = 0;
                                }
                                """,
                        """
                                import lombok.experimental.UtilityClass;
                                                                
                                @UtilityClass
                                public class A {
                                   public final int a, b, c = 0;
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

    @Test
    void onlyUpgradeRelevantToUtilityClass() {
        rewriteRun(
                recipeSpec -> recipeSpec
                        .recipe(new LombokUtilityClass()),
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
                ),
                java("""
                                public class B {
                                   public int add(final int x, final int y) {
                                      return x + y;
                                   }
                                }
                                """
                )
        );
    }

    @Test
    void doNotChangeReferenced() {
        rewriteRun(
                recipeSpec -> recipeSpec
                        .recipe(new LombokUtilityClass()),
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
                ),
                java("""
                                public class B {
                                   public int add(final int x, final int y) {
                                      return A.add(x, y);
                                   }
                                }
                                """
                )
        );
    }

    @Test
    void happyPathInner() {
        rewriteRun(
                recipeSpec -> recipeSpec
                        .recipe(new LombokUtilityClass()),
                java(
                        """
                                public class A {
                                    public int add(final int x, final int y) {
                                        return x + y;
                                    }
                                    
                                    private class B {
                                        private static int substract(final int x, final int y) {
                                            return x - y;
                                        }
                                    }
                                }
                                """,
                        """
                                import lombok.experimental.UtilityClass;
                                              
                                public class A {
                                    public int add(final int x, final int y) {
                                        return x + y;
                                    }
                                    
                                    @UtilityClass
                                    private class B {
                                        private int substract(final int x, final int y) {
                                            return x - y;
                                        }
                                    }
                                }
                                """
                )
        );
    }

    /**
     * Nested ~ inner static
     */
    @Test
    void happyPathNested() {
        rewriteRun(
                recipeSpec -> recipeSpec
                        .recipe(new LombokUtilityClass()),
                java(
                        """
                                public class A {
                                    public int add(final int x, final int y) {
                                        return x + y;
                                    }
                                    
                                    private static class B {
                                        private static int substract(final int x, final int y) {
                                            return x - y;
                                        }
                                    }
                                }
                                """,
                        """
                                import lombok.experimental.UtilityClass;
                                
                                public class A {
                                    public int add(final int x, final int y) {
                                        return x + y;
                                    }
                                    
                                    @UtilityClass
                                    private static class B {
                                        private int substract(final int x, final int y) {
                                            return x - y;
                                        }
                                    }
                                }
                                """
                )
        );
    }

    @Test
    void happyPathNonPublic() {
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
                                class B {
                                    public static int substract(final int x, final int y) {
                                        return x - y;
                                    }
                                }
                                """,
                        """
                                import lombok.experimental.UtilityClass;
                                              
                                public class A {
                                   public int add(final int x, final int y) {
                                      return x + y;
                                   }
                                }
                                
                                @UtilityClass
                                class B {
                                    public int substract(final int x, final int y) {
                                        return x - y;
                                    }
                                }
                                """
                )
        );
    }
}