package org.openrewrite.java;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

/**
 * Cases to test:
 * - Interfaces
 * - Empty classes & interfaces
 * - inheritance
 * - abstract classes
 * - instantiations of changed classes
 * - constructor
 */
class LombokUtilityClassTest implements RewriteTest {

    @Nested
    class ShouldApplyLombokUtility implements RewriteTest {

        @Test
        void givenOneStaticMethod() {
            rewriteRun(
                    recipeSpec -> recipeSpec.recipe(new LombokUtilityClass()),
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
        void givenOneStaticFinalMember() {
            rewriteRun(
                    recipeSpec -> recipeSpec.recipe(new LombokUtilityClass()),
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
        void givenMultipleStaticFinalMembers() {
            rewriteRun(
                    recipeSpec -> recipeSpec.recipe(new LombokUtilityClass()),
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
        void givenStaticInnerClassWithOneStaticMethod() {
            rewriteRun(
                    recipeSpec -> recipeSpec.recipe(new LombokUtilityClass()),
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
        void givenInnerClassWithOneStaticMethod() {
            rewriteRun(
                    recipeSpec -> recipeSpec.recipe(new LombokUtilityClass()),
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

        @Test
        void givenNotPublicClassWithOneStaticMethod() {
            rewriteRun(
                    recipeSpec -> recipeSpec.recipe(new LombokUtilityClass()),
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

    @Nested
    class ShouldNotApplyLombokUtility implements RewriteTest {
        @Test
        void givenStaticMemberIsNotFinal() {
            rewriteRun(
                    recipeSpec -> recipeSpec.recipe(new LombokUtilityClass()),
                    java(
                            """
                                    public class A {
                                       public static int C = 0;
                                    }
                                    """
                    )
            );
        }

        @Test
        void givenMethodIsNotStatic() {
            rewriteRun(
                    recipeSpec -> recipeSpec.recipe(new LombokUtilityClass()),
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
        void givenMain() {
            rewriteRun(
                    recipeSpec -> recipeSpec.recipe(new LombokUtilityClass()),
                    java(
                            """
                                    public class A {
                                       public static void main(String[] args) {
                                       }
                                    }
                                    """
                    )
            );
        }

    }

    @Test
    void shouldNotChangeClassWhenStaticMethodOfChangedClassIsCalled() {
        rewriteRun(
                recipeSpec -> recipeSpec.recipe(new LombokUtilityClass()),
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
    void shoulOnlyUpgradeRelevantToUtilityClass() {
        rewriteRun(
                recipeSpec -> recipeSpec.recipe(new LombokUtilityClass()),
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




}