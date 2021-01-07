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
package org.openrewrite.java.search

import org.junit.jupiter.api.Test
import org.openrewrite.RecipeTest
import org.openrewrite.TreePrinter
import org.openrewrite.java.JavaParser
import org.openrewrite.marker.SearchResult

interface FindAnnotationTest: RecipeTest {
    override val treePrinter: TreePrinter<*>?
        get() = SearchResult.PRINTER

    companion object {
        const val foo = """
            package com.netflix.foo;
            public @interface Foo {
                String bar();
                String baz();
            }
        """
    }

    @Test
    fun matchesSimpleFullyQualifiedAnnotation(jp: JavaParser) = assertChanged(
        jp,
        recipe = FindAnnotation().apply { setSignature("@java.lang.Deprecated") },
        before = "@Deprecated public class A {}",
        after = "~~>@Deprecated public class A {}"
    )

    @Test
    fun matchesAnnotationOnMethod(jp: JavaParser) = assertChanged(
        jp,
        recipe = FindAnnotation().apply { setSignature("@java.lang.Deprecated") },
        before = """
            public class A {
                @Deprecated
                public void foo() {}
            }
        """,
        after = """
            public class A {
                ~~>@Deprecated
                public void foo() {}
            }
        """
    )

    @Test
    fun matchesAnnotationOnField(jp: JavaParser) = assertChanged(
        jp,
        recipe = FindAnnotation().apply { setSignature("@java.lang.Deprecated") },
        before = """
            public class A {
                @Deprecated String s;
            }
        """,
        after = """
            public class A {
                ~~>@Deprecated String s;
            }
        """
    )

    @Test
    fun doesNotMatchNotFullyQualifiedAnnotations(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = FindAnnotation().apply { setSignature("@Deprecated") },
        before = "@Deprecated public class A {}"
    )

    @Test
    fun matchesSingleAnnotationParameter(jp: JavaParser) = assertChanged(
        jp,
        recipe = FindAnnotation().apply { setSignature("""@java.lang.SuppressWarnings("deprecation")""") },
        before = "@SuppressWarnings(\"deprecation\") public class A {}",
        after = "~~>@SuppressWarnings(\"deprecation\") public class A {}"
    )

    @Test
    fun doesNotMatchDifferentSingleAnnotationParameter(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = FindAnnotation().apply { setSignature("""@java.lang.SuppressWarnings("foo")""") },
        before = "@SuppressWarnings(\"deprecation\") public class A {}"
    )

    @Test
    fun matchesNamedParameters(jp: JavaParser) = assertChanged(
        jp,
        recipe = FindAnnotation().apply { setSignature("""@com.netflix.foo.Foo(bar="quux",baz="bar")""") },
        before = """
            import com.netflix.foo.Foo;
            @Foo(bar="quux", baz="bar")
            public class A {} 
        """,
        after = """
            import com.netflix.foo.Foo;
            ~~>@Foo(bar="quux", baz="bar")
            public class A {}
        """,
        dependsOn = arrayOf(foo)
    )

    @Test
    fun doesNotMatchDifferentNamedParameters(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = FindAnnotation().apply { setSignature("""@com.netflix.foo.Foo(bar="qux",baz="baz")""") },
        before = """
            import com.netflix.foo.Foo;
            @Foo(bar="quux", baz="bar")
            public class A {} 
        """,
        dependsOn = arrayOf(foo)
    )

    @Test
    fun matchesNamedParametersRegardlessOfOrder(jp: JavaParser) = assertChanged(
        jp,
        recipe = FindAnnotation().apply { setSignature("""@com.netflix.foo.Foo(baz="bar",bar="quux")""") },
        before = """
            import com.netflix.foo.Foo;
            @Foo(bar="quux", baz="bar")
            public class A {} 
        """,
        after = """
            import com.netflix.foo.Foo;
            ~~>@Foo(bar="quux", baz="bar")
            public class A {}
        """,
        dependsOn = arrayOf(foo)
    )
}
