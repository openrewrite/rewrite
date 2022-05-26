/*
 * Copyright 2021 the original author or authors.
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

package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

@Suppress("Convert2Lambda", "Anonymous2MethodRef", "CodeBlock2Expr", "WriteOnlyObject")
interface UseLambdaForFunctionalInterfaceTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = UseLambdaForFunctionalInterface()

    @Disabled("The recipe currently avoids simplifying anonymous classes that use the this keyword.")
    @Test
    fun useLambdaThenSimplifyFurther(jp: JavaParser) = assertChanged(
        jp,
        recipe = UseLambdaForFunctionalInterface().doNext(ReplaceLambdaWithMethodReference()),
        before = """
        class Test {
            Runnable r = new Runnable() {
                @Override public void run() {
                    Test.this.execute();
                }
            };
            
            void execute() {}
        }
    """,
        after = """
        class Test {
            Runnable r = Test.this::execute;
            
            void execute() {}
        }
    """
    )

    @Test
    fun useLambda(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.util.function.Function;
            class Test {
                Function<Integer, Integer> f = new Function<Integer, Integer>() {
                    @Override 
                    public Integer apply(Integer n) {
                        return n + 1;
                    }
                };
            }
        """,
        after = """
            import java.util.function.Function;
            class Test {
                Function<Integer, Integer> f = n -> n + 1;
            }
        """
    )

    @Test
    fun useLambdaNoParameters(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.util.function.Supplier;
            class Test {
                Supplier<Integer> s = new Supplier<Integer>() {
                    @Override 
                    public Integer get() {
                        return 1;
                    }
                };
            }
        """,
        after = """
            import java.util.function.Supplier;
            class Test {
                Supplier<Integer> s = () -> 1;
            }
        """
    )

    @Suppress("UnusedAssignment")
    @Test
    fun emptyLambda(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.util.function.Consumer;
            
            class Test {
                void foo() {
                    Consumer<Integer> s;
                    s = new Consumer<Integer>() {
                        @Override
                        public void accept(Integer i) {
                        }
                    };
                }
            }
        """,
        after = """
            import java.util.function.Consumer;
            
            class Test {
                void foo() {
                    Consumer<Integer> s;
                    s = i -> {
                    };
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1828")
    @Test
    fun nestedLambdaInMethodArgument(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.util.function.Consumer;
            
            class Test {
                void bar(Consumer<Integer> c) {
                }
                void foo() {
                    bar(new Consumer<Integer>() {
                        @Override
                        public void accept(Integer i) {
                            bar(new Consumer<Integer>() {
                                @Override
                                public void accept(Integer i2) {
                                }
                            });
                        }
                    });
                }
            }
        """,
        after = """
            import java.util.function.Consumer;
            
            class Test {
                void bar(Consumer<Integer> c) {
                }
                void foo() {
                    bar(i -> {
                        bar(i2 -> {
                        });
                    });
                }
            }
        """
    )

    @Test
    fun dontUseLambdaWhenThis(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.util.function.Function;
            class Test {
                int n;
                Function<Integer, Integer> f = new Function<Integer, Integer>() {
                    @Override 
                    public Integer apply(Integer n) {
                        return this.n;
                    }
                };
            }
        """
    )

    @Suppress("UnnecessaryLocalVariable")
    @Test
    fun dontUseLambdaWhenShadowsLocalVariable(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.util.function.Supplier;
            class Test {
                void test() {
                    int n = 1;
                    Supplier<Integer> f = new Supplier<Integer>() {
                        @Override
                        public Integer get() {
                            int n = 0;
                            return n;
                        }
                    };
                }
            }
        """
    )

    @Test
    fun finalParameters(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.util.function.Function;
            class Test {
                Function<Integer, Integer> f = new Function<Integer, Integer>() {
                    @Override 
                    public Integer apply(final Integer n) {
                        return n + 1;
                    }
                };
            }
        """,
        after = """
            import java.util.function.Function;
            class Test {
                Function<Integer, Integer> f = n -> n + 1;
            }
        """
    )

    @Test
    fun useLambdaThenRemoveUnusedImports(jp: JavaParser) = assertChanged(
        jp,
        before = """
        import java.util.HashMap;
        import java.util.function.Function;

        public class Temp {
            public static void foo(){
                new HashMap<Integer, String>().computeIfAbsent(3, new Function<Integer, String>() {
                    @Override
                    public String apply(Integer integer) {
                        return String.valueOf(integer + 1);
                    }
                });
            }
        }
        """,
        after = """
        import java.util.HashMap;

        public class Temp {
            public static void foo(){
                new HashMap<Integer, String>().computeIfAbsent(3, integer -> String.valueOf(integer + 1));
            }
        }
        """
    )
}
