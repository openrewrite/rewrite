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

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

@Suppress("Convert2Lambda")
interface UseLambdaForFunctionalInterfaceTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = UseLambdaForFunctionalInterface()

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
                            Object inner = new Consumer<Integer>() {
                                @Override
                                public void accept(Integer i2) {
                                }
                            };
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
                        Object inner = i2 -> {
                        };
                    });
                }
            }
        """
    )
}
