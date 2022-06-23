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
@file:Suppress("StatementWithEmptyBody", "PointlessBooleanExpression", "ConstantConditions",
    "ClassInitializerMayBeStatic", "ConditionCoveredByFurtherCondition", "DuplicateCondition", "DoubleNegation",
    "UnnecessaryLocalVariable", "StringOperationCanBeSimplified"
)

package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

interface SimplifyBooleanExpressionTest : JavaRecipeTest {
    override val recipe: Recipe?
        get() = SimplifyBooleanExpression()

    @Test
    fun simplifyEqualsLiteralTrueIf(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                boolean a;
                {
                    if(true == a) {
                    }
                }
            }
        """,
        after = """
            public class A {
                boolean a;
                {
                    if(a) {
                    }
                }
            }
        """
    )

    @Test
    fun simplifyBooleanExpressionComprehensive(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                {
                    boolean a = !false;
                    boolean b = (a == true);
                    boolean c = b || true;
                    boolean d = c || c;
                    boolean e = d && d;
                    boolean f = (e == true) || e;
                    boolean g = f && false;
                    boolean h = !!g;
                    boolean i = (a != false);
                }
            }
        """,
        after = """
            public class A {
                {
                    boolean a = true;
                    boolean b = a;
                    boolean c = true;
                    boolean d = c;
                    boolean e = d;
                    boolean f = e;
                    boolean g = false;
                    boolean h = g;
                    boolean i = a;
                }
            }
        """
    )


    @Test
    fun simplifyInvertedBooleanLiteral(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                {
                    boolean a = !false;
                    boolean b = !true;
                }
            }
        """,
        after = """
            public class A {
                {
                    boolean a = true;
                    boolean b = false;
                }
            }
        """
    )

    @Test
    fun simplifyEqualsLiteralTrue(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                {
                    boolean a = true;
                    boolean b = (a == true);
                }
            }
        """,
        after = """
            public class A {
                {
                    boolean a = true;
                    boolean b = a;
                }
            }
        """
    )

    @Test
    fun simplifyOrLiteralTrue(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                {
                    boolean b = true;
                    boolean c = b || true;
                }
            }
        """,
        after = """
            public class A {
                {
                    boolean b = true;
                    boolean c = true;
                }
            }
        """
    )

    @Test
    fun simplifyOrAlwaysTrue(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                {
                    boolean c = true;
                    boolean d = c || c;
                }
            }
        """,
        after = """
            public class A {
                {
                    boolean c = true;
                    boolean d = c;
                }
            }
        """
    )

    @Test
    fun simplifyAndAlwaysTrue(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                {
                    boolean d = true;
                    boolean e = d && d;
                }
            }
        """,
        after = """
            public class A {
                {
                    boolean d = true;
                    boolean e = d;
                }
            }
        """
    )

    @Test
    fun simplifyEqualsLiteralTrueAlwaysTrue(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                {
                    boolean e = true;
                    boolean f = (e == true) || e;
                }
            }
        """,
        after = """
            public class A {
                {
                    boolean e = true;
                    boolean f = e;
                }
            }
        """
    )

    @Test
    fun simplifyLiteralFalseAlwaysFalse(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                {
                    boolean f = true;
                    boolean g = f && false;
                }
            }
        """,
        after = """
            public class A {
                {
                    boolean f = true;
                    boolean g = false;
                }
            }
        """
    )

    @Test
    fun simplifyDoubleNegation(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                public void doubleNegation(boolean g) {
                    boolean h = !!g;
                }
            }
        """,
        after = """
            public class A {
                public void doubleNegation(boolean g) {
                    boolean h = g;
                }
            }
        """
    )

    @Test
    fun simplifyNotEqualsFalse(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                {
                    boolean a = true;
                    boolean i = (a != false);
                }
            }
        """,
        after = """
            public class A {
                {
                    boolean a = true;
                    boolean i = a;
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/502")
    @Test
    fun autoFormatIsConditionallyApplied(jp: JavaParser) = assertUnchanged(
        before = """
            public class A {
                {
                    boolean a=true;
                    boolean i=(a!=true);
                }
            }
        """,

    )
    @Test
    fun binaryOrBothFalse(jp: JavaParser) = assertChanged(
        before = """
            public class A {
                {
                    if (!true || !true) {
                        System.out.println("");
                    }
                }
            }
        """,
        after = """
            public class A {
                {
                    if (false) {
                        System.out.println("");
                    }
                }
            }
        """
    )

}
