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

    fun unnecessaryParentheses(with: UnnecessaryParenthesesStyle.() -> UnnecessaryParenthesesStyle = { this }) =
        listOf(
            NamedStyles(
                "test", listOf(
                    UnnecessaryParenthesesStyle(
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false
                    ).run { with(this) }
                )
            )
        )

    @Test
    fun fullUnwrappingDefault(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(
            listOf(
                NamedStyles(
                    "test", listOf(
                        IntelliJ.unnecessaryParentheses()
                    )
                )
            )
        ).build(),
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
    @Disabled
    fun unwrapExpr(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(unnecessaryParentheses {
            withExpr(true)
        }).build(),
        before = """
                public class A {
                    void doNothing() {
                        double num = (1.0 + 1.0) + 2.0;
                    }
                }
            """,
        after = """
                public class A {
                    void doNothing() {
                        double num = 1.0 + 1.0 + 2.0;
                    }
                }
            """
    )

    @Test
    fun unwrapIdent(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(unnecessaryParentheses {
            withIdent(true)
        }).build(),
        before = """
                public class A {
                    double doNothing() {
                        double num = (10.0);
                        return (num);
                    }
                }
            """,
        after = """
                public class A {
                    double doNothing() {
                        double num = (10.0);
                        return num;
                    }
                }
            """
    )

    @Test
    @Disabled
    fun unwrapNum(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(unnecessaryParentheses {
            withNumDouble(true)
            withNumFloat(true)
            withNumInt(true)
            withNumLong(true)
        }).build(),
        before = """
                public class A {
                    void doNothing() {
                        double a = (1.0);
                        float b = (1.0);
                        int c = (1);
                        long d = (1.0L);
                    }
                }
            """,
        after = """
                public class A {
                    void doNothing() {
                        double a = 1.0;
                        float b = 1.0;
                        int c = 1;
                        long d = 1.0L;
                    }
                }
            """
    )

    @Test
    @Disabled
    fun unwrapLiteral(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(unnecessaryParentheses{
            withLiteralTrue(true)
            withLiteralFalse(true)
            withLiteralNull(true)
            withStringLiteral(true)
        }).build(),
        before = """
                public class A {
                    void doNothing() {
                        boolean a = (true);
                        boolean b = (false);
                        if (a == (true)) {
                          b = (false);
                        } else if (b == (false)) {
                          a = (true);
                        }
                        
                        String s = ("literallyString"); 
                        if (s == (null)) {
                            s = (null);
                        } else if (("someLiteral").equals(s)) {
                            s = (null);
                        }
                    }
                }
            """,
        after = """
                public class A {
                    void doNothing() {
                        boolean a = (true);
                        boolean b = (false);
                        if (a == (true)) {
                          b = (false);
                        } else if (b == (false)) {
                          a = (true);
                        }
                        
                        String s = ("literallyString"); 
                        if (s == (null)) {
                            s = (null);
                        } else if (("someLiteral").equals(s)) {
                            s = (null);
                        }
                    }
                }
            """
    )

    @Test
    @Disabled
    fun unwrapAssignment(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(unnecessaryParentheses {
            withAssign(true)
        }).build(),
        before = """
                public class A {
                    void doNothing() {
                        double a = (10.0);
                        a = (10.0);
                        double b = (a);
                        b = b; // identity assignment
                        b += (b);
                        double c = (a + (b));
                        c = (a + b);
                        c = a + b; // binary operation
                        c *= (c);

                        String d = ("example") + ("assignment");
                        d = ("example" + "assignment");
                        d += ("example") + ("assignment");
                        d = (("example") + ("assignment"));
                    }
                }
            """,
        after = """
                public class A {
                    void doNothing() {
                        double a = 10.0;
                        a = 10.0;
                        double b = a;
                        b = b; // identity assignment
                        b += (b);
                        double c = a + (b);
                        c = a + b;
                        c = a + b; // binary operation
                        c *= (c);

                        String d = ("example") + ("assignment");
                        d = "example" + "assignment";
                        d += ("example") + ("assignment");
                        d = ("example") + ("assignment");
                    }
                }
            """
    )

    @Test
    fun unwrapBandAssign(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(unnecessaryParentheses {
            withBandAssign(true)
        }).build(),
        before = """
                public class A {
                    int a = 5;
                    int b = 7;
                    void bitwiseAnd() {
                        int c = (a & b);
                        c &= (c);
                    }
                }
            """,
        after = """
                public class A {
                    int a = 5;
                    int b = 7;
                    void bitwiseAnd() {
                        int c = (a & b);
                        c &= c;
                    }
                }
            """
    )

    @Test
    fun unwrapBorAssign(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(unnecessaryParentheses {
            withBorAssign(true)
        }).build(),
        before = """
                public class A {
                    int a = 5;
                    int b = 7;
                    void bitwiseOr() {
                        int c = (a | b);
                        c |= (c);
                    }
                }
            """,
        after = """
                public class A {
                    int a = 5;
                    int b = 7;
                    void bitwiseOr() {
                        int c = (a | b);
                        c |= c;
                    }
                }
            """
    )

    @Test
    fun unwrapBsrAssign(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(unnecessaryParentheses {
            withBsrAssign(true)
        }).build(),
        before = """
                public class A {
                    int a = -1;
                    void unsignedRightShiftAssignment() {
                        int b = a >>> 1;
                        b >>>= (b);
                    }
                }
            """,
        after = """
                public class A {
                    int a = -1;
                    void unsignedRightShiftAssignment() {
                        int b = a >>> 1;
                        b >>>= b;
                    }
                }
            """
    )

    @Test
    fun unwrapBxorAssign(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(unnecessaryParentheses {
            withBxorAssign(true)
        }).build(),
        before = """
                public class A {
                    boolean a = true;
                    boolean b = false;
                    void bitwiseExclusiveOr() {
                        boolean c = (a ^ b);
                        c ^= (c);
                    }
                }
            """,
        after = """
                public class A {
                    boolean a = true;
                    boolean b = false;
                    void bitwiseExclusiveOr() {
                        boolean c = (a ^ b);
                        c ^= c;
                    }
                }
            """
    )

    @Test
    fun unwrapDivAssign(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(unnecessaryParentheses {
            withDivAssign(true)
        }).build(),
        before = """
                public class A {
                    int a = 10;
                    int b = 5;
                    void divisionAssignmentOperator() {
                        int c = (a / b);
                        c /= (c);
                    }
                }
            """,
        after = """
                public class A {
                    int a = 10;
                    int b = 5;
                    void divisionAssignmentOperator() {
                        int c = (a / b);
                        c /= c;
                    }
                }
            """
    )

    @Test
    fun unwrapMinusAssign(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(unnecessaryParentheses {
            withMinusAssign(true)
        }).build(),
        before = """
                public class A {
                    int a = 10;
                    int b = 5;
                    void minusAssignment() {
                        int c = (a - b);
                        c -= (c);
                    }
                }
            """,
        after = """
                public class A {
                    int a = 10;
                    int b = 5;
                    void minusAssignment() {
                        int c = (a - b);
                        c -= c;
                    }
                }
            """
    )

    @Test
    fun unwrapModAssign(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(unnecessaryParentheses {
            withModAssign(true)
        }).build(),
        before = """
                public class A {
                    int a = 5;
                    int b = 3;
                    void remainderAssignment() {
                        int c = a % b;
                        c %= (c);
                    }
                }
            """,
        after = """
                public class A {
                    int a = 5;
                    int b = 3;
                    void remainderAssignment() {
                        int c = a % b;
                        c %= c;
                    }
                }
            """
    )

    @Test
    fun unwrapPlusAssign(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(unnecessaryParentheses {
            withPlusAssign(true)
        }).build(),
        before = """
                public class A {
                    int a = 1;
                    int b = 1;
                    void plusAssignment() {
                        int c = a + b;
                        c += (c);
                    }
                }
            """,
        after = """
                public class A {
                    int a = 1;
                    int b = 1;
                    void plusAssignment() {
                        int c = a + b;
                        c += c;
                    }
                }
            """
    )

    @Test
    fun unwrapSlAssign(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(unnecessaryParentheses {
            withSlAssign(true)
        }).build(),
        before = """
                public class A {
                    int a = 1;
                    int b = 1;
                    void leftShiftAssignment() {
                        int c = a << b;
                        c <<= (c);
                    }
                }
            """,
        after = """
                public class A {
                    int a = 1;
                    int b = 1;
                    void leftShiftAssignment() {
                        int c = a << b;
                        c <<= c;
                    }
                }
            """
    )

    @Test
    fun unwrapSrAssign(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(unnecessaryParentheses {
            withSrAssign(true)
        }).build(),
        before = """
                public class A {
                    int a = 1;
                    int b = 1;
                    void signedRightShiftAssignment() {
                        int c = a >> b;
                        c >>= (c);
                    }
                }
            """,
        after = """
                public class A {
                    int a = 1;
                    int b = 1;
                    void signedRightShiftAssignment() {
                        int c = a >> b;
                        c >>= c;
                    }
                }
            """
    )

    @Test
    fun unwrapStarAssign(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(unnecessaryParentheses {
            withStarAssign(true)
        }).build(),
        before = """
                public class A {
                    int a = 1;
                    int b = 1;
                    void multiplicationAssignmentOperator() {
                        int c = a * b;
                        c *= (c);
                    }
                }
            """,
        after = """
                public class A {
                    int a = 1;
                    int b = 1;
                    void multiplicationAssignmentOperator() {
                        int c = a * b;
                        c *= c;
                    }
                }
            """
    )

    @Test
    fun unwrapLambda(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(unnecessaryParentheses {
            withLambda(true)
        }).build(),
        before = """
                import java.util.*;
                public class A {
                    void doNothing() {
                        List<String> list = Arrays.asList("a1", "b1", "c1");
                        list.stream()
                          .filter((s) -> s.startsWith("c"))
                          .forEach(System.out::println);
                    }
                }
            """,
        after = """
                import java.util.*;
                public class A {
                    void doNothing() {
                        List<String> list = Arrays.asList("a1", "b1", "c1");
                        list.stream()
                          .filter(s -> s.startsWith("c"))
                          .forEach(System.out::println);
                    }
                }
            """
    )
}
