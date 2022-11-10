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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.test.RewriteTest

interface FindAnnotationsTest : JavaRecipeTest {
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
    fun matchMetaAnnotation(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.classpath(JavaParser.runtimeClasspath()).logCompilationWarningsAndErrors(true).build(),
        recipe = FindAnnotations("@javax.annotation.Nonnull", true),
        before = """
            import org.openrewrite.internal.lang.Nullable;
            public class Test {
                @Nullable String name;
            }
        """,
        after = """
            import org.openrewrite.internal.lang.Nullable;
            public class Test {
                /*~~>*/@Nullable String name;
            }
        """
    )

    @Suppress("NewClassNamingConvention")
    @Issue("https://github.com/openrewrite/rewrite/issues/357")
    @Test
    fun matchesClassArgument(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.classpath("junit-jupiter-api").build(),
        recipe = FindAnnotations("@org.junit.jupiter.api.extension.ExtendWith(org.openrewrite.MyExtension.class)", null),
        before = """
            import org.junit.jupiter.api.extension.ExtendWith;
            import org.openrewrite.MyExtension;
            @ExtendWith(MyExtension.class) public class A {}
            @ExtendWith({MyExtension.class}) class B {}
            @ExtendWith(value = MyExtension.class) class C {}
            @ExtendWith(value = {MyExtension.class}) class D {}
        """,
        after = """
            import org.junit.jupiter.api.extension.ExtendWith;
            import org.openrewrite.MyExtension;
            /*~~>*/@ExtendWith(MyExtension.class) public class A {}
            /*~~>*/@ExtendWith({MyExtension.class}) class B {}
            /*~~>*/@ExtendWith(value = MyExtension.class) class C {}
            /*~~>*/@ExtendWith(value = {MyExtension.class}) class D {}
        """,
        dependsOn = arrayOf(
            """
                package org.openrewrite;
                import org.junit.jupiter.api.extension.Extension;
                public class MyExtension implements Extension {}
            """
        )
    )

    @Test
    fun matchesSimpleFullyQualifiedAnnotation(jp: JavaParser) = assertChanged(
        jp,
        recipe = FindAnnotations("@java.lang.Deprecated", null),
        before = "@Deprecated public class A {}",
        after = "/*~~>*/@Deprecated public class A {}"
    )

    @Test
    fun matchesWildcard(jp: JavaParser) = assertChanged(
        jp,
        recipe = FindAnnotations("@java.lang.*", null),
        before = "@Deprecated public class A {}",
        after = "/*~~>*/@Deprecated public class A {}"
    )

    @Test
    fun matchesSubpackageWildcard(jp: JavaParser) = assertChanged(
        jp,
        recipe = FindAnnotations("@java..*", null),
        before = "@Deprecated public class A {}",
        after = "/*~~>*/@Deprecated public class A {}"
    )

    @Test
    fun matchesAnnotationOnMethod(jp: JavaParser) = assertChanged(
        jp,
        recipe = FindAnnotations("@java.lang.Deprecated", null),
        before = """
            public class A {
                @Deprecated
                public void foo() {}
            }
        """,
        after = """
            public class A {
                /*~~>*/@Deprecated
                public void foo() {}
            }
        """
    )

    @Test
    fun matchesAnnotationOnField(jp: JavaParser) = assertChanged(
        jp,
        recipe = FindAnnotations("@java.lang.Deprecated", null),
        before = """
            public class A {
                @Deprecated String s;
            }
        """,
        after = """
            public class A {
                /*~~>*/@Deprecated String s;
            }
        """
    )

    @Test
    fun doesNotMatchNotFullyQualifiedAnnotations(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = FindAnnotations("@Deprecated", null),
        before = "@Deprecated public class A {}"
    )

    @Test
    fun matchesSingleAnnotationParameter(jp: JavaParser) = assertChanged(
        jp,
        recipe = FindAnnotations("""@java.lang.SuppressWarnings("deprecation")""", null),
        before = "@SuppressWarnings(\"deprecation\") public class A {}",
        after = "/*~~>*/@SuppressWarnings(\"deprecation\") public class A {}"
    )

    @Test
    fun doesNotMatchDifferentSingleAnnotationParameter(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = FindAnnotations("""@java.lang.SuppressWarnings("foo")""", null),
        before = "@SuppressWarnings(\"deprecation\") public class A {}"
    )

    @Test
    fun matchesNamedParameters(jp: JavaParser) = assertChanged(
        jp,
        recipe = FindAnnotations("""@com.netflix.foo.Foo(bar="quux",baz="bar")""", null),
        before = """
            import com.netflix.foo.Foo;
            @Foo(bar="quux", baz="bar")
            public class A {}
        """,
        after = """
            import com.netflix.foo.Foo;
            /*~~>*/@Foo(bar="quux", baz="bar")
            public class A {}
        """,
        dependsOn = arrayOf(foo)
    )

    @Test
    fun doesNotMatchDifferentNamedParameters(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = FindAnnotations("""@com.netflix.foo.Foo(bar="qux",baz="baz")""", null),
        before = """
            import com.netflix.foo.Foo;
            @Foo(bar="quux", baz="bar")
            public class A {} 
        """,
        dependsOn = arrayOf(foo)
    )

    @Test
    fun matchesPartialNamedParameters(jp: JavaParser) = assertChanged(
        jp,
        recipe = FindAnnotations("""@com.netflix.foo.Foo(baz="bar")""", null),
        before = """
            import com.netflix.foo.Foo;
            @Foo(bar="quux", baz="bar")
            public class A {}
        """,
        after = """
            import com.netflix.foo.Foo;
            /*~~>*/@Foo(bar="quux", baz="bar")
            public class A {}
        """,
        dependsOn = arrayOf(foo)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/358")
    @Test
    fun matchesNamedParametersRegardlessOfOrder(jp: JavaParser) = assertChanged(
        jp,
        recipe = FindAnnotations("""@com.netflix.foo.Foo(baz="bar",bar="quux")""", null),
        before = """
            import com.netflix.foo.Foo;
            @Foo(bar="quux", baz="bar")
            public class A {}
        """,
        after = """
            import com.netflix.foo.Foo;
            /*~~>*/@Foo(bar="quux", baz="bar")
            public class A {}
        """,
        dependsOn = arrayOf(foo)
    )

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = FindAnnotations(null, null)
        var valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("annotationPattern")

        recipe = FindAnnotations("@com.netflix.foo.Foo(baz=\"bar\",bar=\"quux\")", null)
        valid = recipe.validate()
        assertThat(valid.isValid).isTrue()
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/394")
    @Test
    fun findAnnotationWithClassTypeArgument(jp: JavaParser) {
        val fooClass = jp.parse("""
            package com.foo;
            
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Inherited;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            
            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.TYPE})
            @Inherited
            public @interface Example { 
                Class<?> value();
            }
        """,
        """
            package com.foo;
            
            @Example(Foo.class)
            public class Foo {}
        """).find { it.classes.first().simpleName == "Foo" }!!

        val maybeExample = FindAnnotations.find(fooClass, "@com.foo.Example(com.foo.Foo.class)")
        assertThat(maybeExample).hasSize(1)
    }

    @Test
    fun enumArgument(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.classpath("jackson-annotations").build(),
        recipe = FindAnnotations("@com.fasterxml.jackson.annotation.JsonTypeInfo(use=com.fasterxml.jackson.annotation.JsonTypeInfo${'$'}Id.CLASS)", null),
        before = """
            import com.fasterxml.jackson.annotation.JsonTypeInfo;
            import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
            
            class PenetrationTesting {
                @JsonTypeInfo(use = Id.CLASS)
                Object name;
            }
        """,
        after = """
            import com.fasterxml.jackson.annotation.JsonTypeInfo;
            import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
            
            class PenetrationTesting {
                /*~~>*/@JsonTypeInfo(use = Id.CLASS)
                Object name;
            }
        """
    )
}
