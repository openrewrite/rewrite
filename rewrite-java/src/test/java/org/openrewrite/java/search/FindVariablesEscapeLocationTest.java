/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.search;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class FindVariablesEscapeLocationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(RewriteTest.toRecipe(FindVariablesEscapeLocation::new));
    }

    @Nested
    class Escaping {
        @Test
        @DocumentExample
        void viaReturnValue() {
            rewriteRun(java(
              """
                package com.sample;
                public class Foo{
                    Object test() {
                        Object o = new Object();
                        return o;
                    }
                }
                """, """
                package com.sample;
                public class Foo{
                    Object test() {
                        Object o = new Object();
                        /*~~>*/return o;
                    }
                }
                """));
        }

        @Test
        void viaField() {
            rewriteRun(java(
              """
                package com.sample;
                public class Foo{
                    Object someField;
                    void test() {
                        Object o = new Object();
                        someField = o;
                    }
                }
                """, """
                package com.sample;
                public class Foo{
                    Object someField;
                    void test() {
                        Object o = new Object();
                        /*~~>*/someField = o;
                    }
                }
                """));
        }

        @Test
        void viaMethodCall() {
            rewriteRun(java(
              """
                package com.sample;
                public class Foo{
                    void test() {
                        Object o = new Object();
                        System.out.print(o);
                    }
                }
                """, """
                package com.sample;
                public class Foo{
                    void test() {
                        Object o = new Object();
                        /*~~>*/System.out.print(o);
                    }
                }
                """));
        }

        @Test
        void viaLambdaBody() {
            rewriteRun(java(
              """
                package com.sample;
                public class Foo{
                    Runnable test() {
                        Object o = new Object();
                        Runnable r = () -> System.out.print(o);
                    }
                }
                """, """
                package com.sample;
                public class Foo{
                    Runnable test() {
                        Object o = new Object();
                        Runnable r = () -> /*~~>*/System.out.print(o);
                    }
                }
                """));
        }

        @Test
        void viaNew() {
            rewriteRun(java(
              """
                package com.sample;
                public class Foo{
                    void test() {
                        StringBuilder sb = new StringBuilder();
                        StringBuilder other = new StringBuilder(sb);
                    }
                }
                """, """
                package com.sample;
                public class Foo{
                    void test() {
                        StringBuilder sb = new StringBuilder();
                        StringBuilder other = /*~~>*/new StringBuilder(sb);
                    }
                }
                """));
        }
    }

    @Nested
    class Secure {
        @Nested
        class OtherObject {
            @Test
            @DocumentExample
            void viaReturnValue() {
                rewriteRun(java(
                  """
                    package com.sample;
                    public class Foo{
                        Object test() {
                            Object o = new Object();
                            return new Object();
                        }
                    }
                    """));
            }

            @Test
            void viaField() {
                rewriteRun(java(
                  """
                    package com.sample;
                    public class Foo{
                        Object someField;
                        void test() {
                            Object o = new Object();
                            someField = new Object();
                        }
                    }
                    """));
            }

            @Test
            void viaMethodCall() {
                rewriteRun(java(
                  """
                    package com.sample;
                    public class Foo{
                        void test() {
                            Object o = new Object();
                            System.out.print(new Object());
                        }
                    }
                    """));
            }

            @Test
            void viaLambdaBody() {
                rewriteRun(java(
                  """
                    package com.sample;
                    public class Foo{
                        Runnable test() {
                            Object o = new Object();
                            Runnable r = () -> System.out.print(new Object());
                        }
                    }
                    """));
            }
        }

        @Nested
        class Primitives {
            @Test
            @DocumentExample
            void viaReturnValue() {
                rewriteRun(java(
                  """
                    package com.sample;
                    public class Foo{
                        int test() {
                            int o = 1;
                            return o;
                        }
                    }
                    """));
            }

            @Test
            void viaField() {
                rewriteRun(java(
                  """
                    package com.sample;
                    public class Foo{
                        int someField;
                        void test() {
                            int o = 1;
                            someField = o;
                        }
                    }
                    """));
            }

            @Test
            void viaMethodCall() {
                rewriteRun(java(
                  """
                    package com.sample;
                    public class Foo{
                        void test() {
                            int o = 1;
                            System.out.print(o);
                        }
                    }
                    """));
            }

            @Test
            void viaLambdaBody() {
                rewriteRun(java(
                  """
                    package com.sample;
                    public class Foo{
                        Runnable test() {
                            int o = 1;
                            Runnable r = () -> System.out.print(o);
                        }
                    }
                    """));
            }
        }

        @Nested
        class Parameter {
            @Test
            @DocumentExample
            void viaReturnValue() {
                rewriteRun(java(
                  """
                    package com.sample;
                    public class Foo{
                        Object test(Object other) {
                            Object o = new Object();
                            return other;
                        }
                    }
                    """));
            }

            @Test
            void viaField() {
                rewriteRun(java(
                  """
                    package com.sample;
                    public class Foo{
                        Object someField;
                        void test(Object other) {
                            Object o = new Object();
                            someField = other;
                        }
                    }
                    """));
            }

            @Test
            void viaMethodCall() {
                rewriteRun(java(
                  """
                    package com.sample;
                    public class Foo{
                        void test(Object other) {
                            Object o = new Object();
                            System.out.print(other);
                        }
                    }
                    """));
            }

            @Test
            void viaLambdaBody() {
                rewriteRun(java(
                  """
                    package com.sample;
                    public class Foo{
                        Runnable test(Object other) {
                            Object o = new Object();
                            Runnable r = () -> System.out.print(other);
                        }
                    }
                    """));
            }
        }
    }
}
