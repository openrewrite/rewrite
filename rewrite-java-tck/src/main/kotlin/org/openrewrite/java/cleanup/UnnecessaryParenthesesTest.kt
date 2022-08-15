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
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.Tree.randomId
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.java.style.Checkstyle
import org.openrewrite.java.style.UnnecessaryParenthesesStyle
import org.openrewrite.style.NamedStyles

@Suppress(
    "UnnecessaryLocalVariable", "ConstantConditions", "UnusedAssignment", "PointlessBooleanExpression",
    "MismatchedStringCase", "SillyAssignment", "StatementWithEmptyBody"
)
interface UnnecessaryParenthesesTest : JavaRecipeTest {
    override val recipe: Recipe?
        get() = UnnecessaryParentheses()

    /**
     * Setting all options to false to enable individually toggling configuration flags.
     *
     * This is a little aggressive to have this large wall of "false, false, false...", I know.
     * The reason for having an "all false" style set is to individually test each style rule.
     * This allows each configuration flag to be tested on a more fine-grained level,
     * and it allows these tests to be more descriptive of what situation each flag is concerned with.
     * For example, did you know bsrAssign was "UNSIGNED RIGHT-SHIFT ASSIGNMENT", or where it was applicable?
     * I didn't either. Hopefully some of these tests, although sometimes redundant, can help clarify.
     */
    fun unnecessaryParentheses(with: UnnecessaryParenthesesStyle.() -> UnnecessaryParenthesesStyle = { this }) =
        listOf(
            NamedStyles(
                randomId(), "test", "test", "test", emptySet(), listOf(
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
                    randomId(), "test", "test", "test", emptySet(), listOf(
                        Checkstyle.unnecessaryParentheses()
                    )
                )
            )
        ).build(),
        before = """
            import java.util.*;

            class Test {
                int square(int a, int b) {
                    int square = (a * b);

                    int sumOfSquares = 0;
                    for (int i = (0); i < 10; i++) {
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

            class Test {
                int square(int a, int b) {
                    int square = a * b;

                    int sumOfSquares = 0;
                    for (int i = 0; i < 10; i++) {
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
    @Issue("https://github.com/openrewrite/rewrite/issues/798")
    fun unwrapExpr(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(unnecessaryParentheses {
            withExpr(true)
        }).build(),
        before = """
            class Test {
                void method(int x, int y, boolean a) {
                    if (a && ((x + y > 0))) {
                        int q = ((1 + 2) + 3);
                        int z = (q + q) * q;
                    }
                }
            }
        """,
        after = """
            class Test {
                void method(int x, int y, boolean a) {
                    if (a && (x + y > 0)) {
                        int q = (1 + 2) + 3;
                        int z = (q + q) * q;
                    }
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
            class Test {
                double doNothing() {
                    double num = (10.0);
                    return (num);
                }
            }
        """,
        after = """
            class Test {
                double doNothing() {
                    double num = (10.0);
                    return num;
                }
            }
        """
    )

    @Test
    fun unwrapNum(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(unnecessaryParentheses {
            withNumDouble(true)
                .withNumFloat(true)
                .withNumInt(true)
                .withNumLong(true)
        }).build(),
        before = """
            class Test {
                void doNothing() {
                    double a = (1000.0);
                    if ((1000.0) == a) {
                        a = (1000.0);
                    }
                    float b = (1000.0f);
                    int c = (1000);
                    long d = (1000L);
                }
            }
        """,
        after = """
            class Test {
                void doNothing() {
                    double a = 1000.0;
                    if (1000.0 == a) {
                        a = 1000.0;
                    }
                    float b = 1000.0f;
                    int c = 1000;
                    long d = 1000L;
                }
            }
        """
    )

    @Test
    fun unwrapLiteral(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(unnecessaryParentheses {
            withLiteralTrue(true)
                .withLiteralFalse(true)
                .withLiteralNull(true)
                .withStringLiteral(true)
        }).build(),
        before = """
            class Test {
                void doNothing() {
                    boolean a = (true);
                    boolean b = (false);
                    if (a == (true)) {
                        b = (false);
                    } else if (b == (false)) {
                        a = (true);
                    }

                    String s = ("literallyString");
                    String t = ("literallyString" + "stringLiteral");
                    if (s == (null)) {
                        s = (null);
                    } else if ((("someLiteral").toLowerCase()).equals(s)) {
                        s = null;
                    }
                }
            }
        """,
        after = """
            class Test {
                void doNothing() {
                    boolean a = true;
                    boolean b = false;
                    if (a == true) {
                        b = false;
                    } else if (b == false) {
                        a = true;
                    }

                    String s = "literallyString";
                    String t = ("literallyString" + "stringLiteral");
                    if (s == null) {
                        s = null;
                    } else if (("someLiteral".toLowerCase()).equals(s)) {
                        s = null;
                    }
                }
            }
        """
    )

    @Test
    fun unwrapAssignment(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(unnecessaryParentheses {
            withAssign(true)
        }).build(),
        before = """
            class Test {
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
            class Test {
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
            withBitAndAssign(true)
        }).build(),
        before = """
            class Test {
                int a = 5;
                int b = 7;

                void bitwiseAnd() {
                    int c = (a & b);
                    c &= (c);
                }
            }
        """,
        after = """
            class Test {
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
            withBitOrAssign(true)
        }).build(),
        before = """
            class Test {
                int a = 5;
                int b = 7;

                void bitwiseOr() {
                    int c = (a | b);
                    c |= (c);
                }
            }
        """,
        after = """
            class Test {
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
            withBitShiftRightAssign(true)
        }).build(),
        before = """
            class Test {
                int a = -1;

                void unsignedRightShiftAssignment() {
                    int b = a >>> 1;
                    b >>>= (b);
                }
            }
        """,
        after = """
            class Test {
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
            withBitXorAssign(true)
        }).build(),
        before = """
            class Test {
                boolean a = true;
                boolean b = false;

                void bitwiseExclusiveOr() {
                    boolean c = (a ^ b);
                    c ^= (c);
                }
            }
        """,
        after = """
            class Test {
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
            class Test {
                int a = 10;
                int b = 5;

                void divisionAssignmentOperator() {
                    int c = (a / b);
                    c /= (c);
                }
            }
        """,
        after = """
            class Test {
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
            class Test {
                int a = 10;
                int b = 5;

                void minusAssignment() {
                    int c = (a - b);
                    c -= (c);
                }
            }
        """,
        after = """
            class Test {
                int a = 10;
                int b = 5;

                void minusAssignment() {
                    int c = (a - b);
                    c -= c;
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1486")
    @Test
    fun unwrapMinusReturnExpression(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(unnecessaryParentheses {
            withExpr(true)
        }).build(),
        before = """
            class T {
                int getInt() {
                    return (4 - 5);
                }
            }
        """,
        after = """
            class T {
                int getInt() {
                    return 4 - 5;
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
            class Test {
                int a = 5;
                int b = 3;

                void remainderAssignment() {
                    int c = a % b;
                    c %= (c);
                }
            }
        """,
        after = """
            class Test {
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
            class Test {
                int a = 1;
                int b = 1;

                void plusAssignment() {
                    int c = a + b;
                    c += (c);
                }
            }
        """,
        after = """
            class Test {
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
            withShiftLeftAssign(true)
        }).build(),
        before = """
            class Test {
                int a = 1;
                int b = 1;

                void leftShiftAssignment() {
                    int c = a << b;
                    c <<= (c);
                }
            }
        """,
        after = """
            class Test {
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
            withShiftRightAssign(true)
        }).build(),
        before = """
            class Test {
                int a = 1;
                int b = 1;

                void signedRightShiftAssignment() {
                    int c = a >> b;
                    c >>= (c);
                }
            }
        """,
        after = """
            class Test {
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
            class Test {
                int a = 1;
                int b = 1;

                void multiplicationAssignmentOperator() {
                    int c = a * b;
                    c *= (c);
                }
            }
        """,
        after = """
            class Test {
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

            class Test {
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

            class Test {
                void doNothing() {
                    List<String> list = Arrays.asList("a1", "b1", "c1");
                    list.stream()
                            .filter(s -> s.startsWith("c"))
                            .forEach(System.out::println);
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/798")
    @Test
    fun unwrapDoubleParens(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(unnecessaryParentheses()).build(),
        before = """
            class Test {
                void test() {
                    int sum = 1 + ((2 + 3));
                }
            }
        """,
        after = """
            class Test {
                void test() {
                    int sum = 1 + (2 + 3);
                }
            }
        """
    )
}
