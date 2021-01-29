/*
 * Copyright 2020 the original author or authors.
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
import org.openrewrite.Recipe
import org.openrewrite.RecipeTest
import org.openrewrite.java.JavaParser
import org.openrewrite.java.style.IntelliJ
import org.openrewrite.java.style.UnnecessaryParenthesesStyle
import org.openrewrite.style.NamedStyles

interface UnnecessaryParenthesesTest : RecipeTest {
    override val recipe: Recipe?
        get() = UnnecessaryParentheses()

    fun unnecessaryParentheses(with: UnnecessaryParenthesesStyle.() -> UnnecessaryParenthesesStyle = { this }) = listOf(
            NamedStyles(
                    "test", listOf(
                    IntelliJ.unnecessaryParentheses().run { with(this) })
            )
    )

    @Test
    fun fullUnwrapping(jp: JavaParser.Builder<*, *>) = assertChanged(
            jp.styles(unnecessaryParentheses()).build(),
            before = """
                import java.util.*;
                public class A {
                    int square(int a, int b) {
                        int square = (a * b);
    
                        int sumOfSquares = 0;
                        for(int i = (0); i < 10; i++) {
                          sumOfSquares += (square(i * i, i));
                        }
                        double num = (10.0);
    
                        List<String> list = Arrays.asList("a1", "b1", "c1");
                        list.stream()
                          .filter((s) -> s.startsWith("c"))
                          .forEach(System.out::println);
    
                        return (square);
                    }
                }
            """,
            after = """
                import java.util.*;
                public class A {
                    int square(int a, int b) {
                        int square = a * b;
    
                        int sumOfSquares = 0;
                        for(int i = 0; i < 10; i++) {
                          sumOfSquares += square(i * i, i);
                        }
                        double num = 10.0;
    
                        List<String> list = Arrays.asList("a1", "b1", "c1");
                        list.stream()
                          .filter(s -> s.startsWith("c"))
                          .forEach(System.out::println);
    
                        return square;
                    }
                }
            """
    )

    @Test
    fun unwrapAssignment(jp: JavaParser.Builder<*, *>) = assertUnchanged(
            // a bit peculiar to have this test "inverted", but since default is 'on',
            // leaving this here as both a test and an example of having assign 'off'
            jp.styles(unnecessaryParentheses {
                withAssign(false)
            }).build(),
            before = """
                public class A {
                    void doNothing() {
                        double num = (10.0);
                    }
                }
            """
    )

    @Test
    @Disabled
    fun unwrapLiteralPrimitive(jp: JavaParser.Builder<*, *>) = assertChanged(
            jp.styles(unnecessaryParentheses()).build(),
            before = """
                public class A {
                    int literalPrimitive() {
                        return (5);
                    }
                }
            """,
            after = """
                public class A {
                    int literalPrimitive() {
                        return 5;
                    }
                }
            """
    )

}
