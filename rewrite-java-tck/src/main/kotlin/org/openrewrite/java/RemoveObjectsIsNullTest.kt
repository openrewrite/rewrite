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
package org.openrewrite.java

import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.Tree
import org.openrewrite.java.style.ImportLayoutStyle
import org.openrewrite.style.NamedStyles

interface RemoveObjectsIsNullTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = RemoveObjectsIsNull()


    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1547")
    fun transformCallToIsNull(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import static java.util.Objects.isNull;
            public class A {
                public void test() {
                    boolean a = true;
                    if (java.util.Objects.isNull(a)) {
                        System.out.println("a is null");
                    };
                }
            }        
        """,
        after = """
            import static java.util.Objects.isNull;
            public class A {
                public void test() {
                    boolean a = true;
                    if (a == null) {
                        System.out.println("a is null");
                    };
                }
            }        
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1547")
    fun transformCallToIsNullNeedsParentheses(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import static java.util.Objects.isNull;
            public class A {
                public void test() {
                    boolean a = true, b = false;
                    if (isNull(a || b)) {
                        System.out.println("a || b is null");
                    };
                }
            }        
        """,
        after = """
            import static java.util.Objects.isNull;
            public class A {
                public void test() {
                    boolean a = true, b = false;
                    if ((a || b) == null) {
                        System.out.println("a || b is null");
                    };
                }
            }        
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1547")
    fun transformCallToNonNull(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import static java.util.Objects.nonNull;
            public class A {
                public void test() {
                    boolean a = true;
                    if (java.util.Objects.nonNull(a)) {
                        System.out.println("a is non-null");
                    };
                }
            }        
        """,
        after = """
            import static java.util.Objects.nonNull;
            public class A {
                public void test() {
                    boolean a = true;
                    if (a != null) {
                        System.out.println("a is non-null");
                    };
                }
            }        
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1547")
    fun transformCallToNonNullNeedsParentheses(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import static java.util.Objects.nonNull;
            public class A {
                public void test() {
                    boolean a = true, b = false;
                    if (nonNull(a || b)) {
                        System.out.println("a || b is non-null");
                    };
                }
            }        
        """,
        after = """
            import static java.util.Objects.nonNull;
            public class A {
                public void test() {
                    boolean a = true, b = false;
                    if ((a || b) != null) {
                        System.out.println("a || b is non-null");
                    };
                }
            }        
        """
    )
}
