/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

/**
 * Cases to test:
 * - inheritance
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

        @Test
        void givenAbstractClass() {
            rewriteRun(
                    recipeSpec -> recipeSpec.recipe(new LombokUtilityClass()),
                    java(
                            """
                                    public abstract class A {
                                       public static void doSmth() {
                                       };
                                    }
                                    """
                    )
            );
        }

        @Test
        void givenInterface() {
            rewriteRun(
                    recipeSpec -> recipeSpec.recipe(new LombokUtilityClass()),
                    java(
                            """
                                    public interface A {                                    
                                       int CONST = 1;
                                       static void doSmth() {
                                       }
                                    }
                                    """
                    )
            );
        }

        // FIXME: use messaging on getCursor() to notify class of existing methods or fields?
        @Test
        void givenEmptyClass() {
            rewriteRun(
                    recipeSpec -> recipeSpec.recipe(new LombokUtilityClass()),
                    java(
                            """
                                    public class A {                                    
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