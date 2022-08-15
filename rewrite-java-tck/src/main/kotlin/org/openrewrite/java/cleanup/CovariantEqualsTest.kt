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

import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

interface CovariantEqualsTest : JavaRecipeTest {
    override val recipe: Recipe?
        get() = CovariantEquals()

    @Test
    fun replaceWithNonCovariantEquals(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class Test {
                int n;

                public boolean equals(Test tee) {
                    return n == tee.n;
                }
            }
        """,
        after = """
            class Test {
                int n;

                @Override
                public boolean equals(Object obj) {
                    if (obj == this) return true;
                    if (obj == null || getClass() != obj.getClass()) return false;
                    Test tee = (Test) obj;
                    return n == tee.n;
                }
            }
        """
    )

    @Test
    fun replaceMultiStatementReturnBody(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class A {
                String id;

                public boolean equals(A other) {
                    boolean isEqual = id.equals(other.id);
                    return isEqual;
                }
            }
        """,
        after = """
            class A {
                String id;

                @Override
                public boolean equals(Object obj) {
                    if (obj == this) return true;
                    if (obj == null || getClass() != obj.getClass()) return false;
                    A other = (A) obj;
                    boolean isEqual = id.equals(other.id);
                    return isEqual;
                }
            }
        """
    )

    @Test
    fun replaceEqualsBasedOnTypeSignature(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class Test {
                int n;
                public void placeholder(Test test) {}
                public void placeholder(Object test) {}

                public boolean equals(Number test) {
                    return false;
                }

                public boolean equals(Test test) {
                    return n == test.n;
                }

                public boolean equals() {
                    return false;
                }

                public boolean equals(String test) {
                    return false;
                }
            }
        """,
        after = """
            class Test {
                int n;
                public void placeholder(Test test) {}
                public void placeholder(Object test) {}

                public boolean equals(Number test) {
                    return false;
                }

                @Override
                public boolean equals(Object obj) {
                    if (obj == this) return true;
                    if (obj == null || getClass() != obj.getClass()) return false;
                    Test test = (Test) obj;
                    return n == test.n;
                }

                public boolean equals() {
                    return false;
                }

                public boolean equals(String test) {
                    return false;
                }
            }
        """
    )

    @Test
    fun replaceEqualsMaintainsExistingAnnotations(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class A {
                String id;

                @SuppressWarnings("all")
                public boolean equals(A other) {
                    boolean isEqual = id.equals(other.id);
                    return isEqual;
                }
            }
        """,
        after = """
            class A {
                String id;

                @Override
                @SuppressWarnings("all")
                public boolean equals(Object obj) {
                    if (obj == this) return true;
                    if (obj == null || getClass() != obj.getClass()) return false;
                    A other = (A) obj;
                    boolean isEqual = id.equals(other.id);
                    return isEqual;
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/653")
    @Test
    fun replaceWithNonCovariantEqualsWhenNested(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class A {
                class B {
                    int n;

                    public boolean equals(B bee) {
                        return n == bee.n;
                    }
                }
            }
        """,
        after = """
            class A {
                class B {
                    int n;

                    @Override
                    public boolean equals(Object obj) {
                        if (obj == this) return true;
                        if (obj == null || getClass() != obj.getClass()) return false;
                        B bee = (B) obj;
                        return n == bee.n;
                    }
                }
            }
        """
    )

    @Test
    fun renameExistingParameterNameWhenParameterNameIsDefaultTemplateName(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class Test {
                int n;

                public boolean equals(Test obj) {
                    return n == obj.n;
                }
            }
        """,
        after = """
            class Test {
                int n;

                @Override
                public boolean equals(Object other) {
                    if (other == this) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    Test obj = (Test) other;
                    return n == obj.n;
                }
            }
        """
    )

    @Test
    fun ignoreIfAtLeastOneExistingNonCovariantEqualsMethod(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            class Test {
                public boolean equals(Test t) {
                    return false;
                }

                public boolean equals(Object i) {
                    return false;
                }
                
                public boolean equals(Object i, Test t) {
                    return false;
                }
            }
        """
    )

    @Test
    fun ignoreIfNoExistingEqualsMethod(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            class A {}
            
            class B {
                B() {}
                public void placeholder(B t) {}
                public void placeholder(Object t) {}
            }
        """
    )

}
