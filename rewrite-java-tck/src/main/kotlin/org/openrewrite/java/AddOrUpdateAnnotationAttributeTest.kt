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
package org.openrewrite.java

import org.junit.jupiter.api.Test
import org.openrewrite.java.Assertions.java
import org.openrewrite.test.RewriteTest


interface AddOrUpdateAnnotationAttributeTest: JavaRecipeTest, RewriteTest {

    @Test
    fun addValueAttribute(jp: JavaParser) = assertChanged(
        jp,
        recipe = AddOrUpdateAnnotationAttribute("org.example.Foo", null, "hello", null),
        dependsOn = arrayOf("""
            package org.example;
            public @interface Foo {
                String value() default "";
            }
        """),
        before = """
            import org.example.Foo;
            
            @Foo
            public class A {
            }
        """,
        after = """
            import org.example.Foo;
            
            @Foo("hello")
            public class A {
            }
        """
    )

    @Test
    fun updateValueAttribute(jp: JavaParser) = assertChanged(
        jp,
        recipe = AddOrUpdateAnnotationAttribute("org.example.Foo", null, "hello", null),
        dependsOn = arrayOf("""
            package org.example;
            public @interface Foo {
                String value() default "";
            }
        """),
        before = """
            import org.example.Foo;
            
            @Foo("goodbye")
            public class A {
            }
        """,
        after = """
            import org.example.Foo;
            
            @Foo("hello")
            public class A {
            }
        """
    )

    @Test
    fun addNamedAttribute(jp: JavaParser) = assertChanged(
        jp,
        recipe = AddOrUpdateAnnotationAttribute("org.junit.Test", "timeout", "500", null),
        dependsOn = arrayOf("""
            package org.junit;
            public @interface Test {
                long timeout() default 0L;
            }
        """),
        before = """
            import org.junit.Test;
            
            class SomeTestClass {
                
                @Test
                void foo() {
                }
            }
        """,
        after = """
            import org.junit.Test;
            
            class SomeTestClass {
                
                @Test(timeout = 500)
                void foo() {
                }
            }
        """
    )

    @Test
    fun replaceAttribute(jp: JavaParser) = assertChanged(
        jp,
        recipe = AddOrUpdateAnnotationAttribute("org.junit.Test", "timeout", "500", null),
        dependsOn = arrayOf("""
            package org.junit;
            public @interface Test {
                long timeout() default 0L;
            }
        """),
        before = """
            import org.junit.Test;
            
            class SomeTestClass {
                
                @Test(timeout = 1)
                void foo() {
                }
            }
        """,
        after = """
            import org.junit.Test;
            
            class SomeTestClass {
                
                @Test(timeout = 500)
                void foo() {
                }
            }
        """
    )

    @Test
    fun preserveExistingAttributes(jp: JavaParser) = assertChanged(
        jp,
        recipe = AddOrUpdateAnnotationAttribute("org.junit.Test", "timeout", "500", null),
        dependsOn = arrayOf("""
            package org.junit;
            public @interface Test {
                long timeout() default 0L;
                long foo() default "";
            }
        """),
        before = """
            import org.junit.Test;
            
            class SomeTestClass {
                
                @Test(foo = "")
                void foo() {
                }
            }
        """,
        after = """
            import org.junit.Test;
            
            class SomeTestClass {
                
                @Test(timeout = 500, foo = "")
                void foo() {
                }
            }
        """
    )

    @Test
    fun implicitValueToExplicitValue(jp: JavaParser)  = assertChanged(
        jp,
        recipe = AddOrUpdateAnnotationAttribute("org.junit.Test", "other", "1", null),
        cycles = 3,
        dependsOn = arrayOf("""
            package org.junit;
            public @interface Test {
                long other() default 0L;
                long value() default "";
            }
        """),
        before = """
            import org.junit.Test;
            
            class SomeTestClass {
                
                @Test("foo")
                void foo() {
                }
            }
        """,
        after = """
            import org.junit.Test;
            
            class SomeTestClass {
                
                @Test(other = 1, value = "foo")
                void foo() {
                }
            }
        """
    )

    @Test
    fun dontChangeWhenSetToAddOnly(jp: JavaParser) = rewriteRun(
        { spec -> spec.parser(jp)
            .recipe(AddOrUpdateAnnotationAttribute("org.junit.Test", "other", "1", true))
        },
        java("""
            package org.junit;
            public @interface Test {
                long other() default 0L;
                long value() default "";
            }
        """),
        java("""
            import org.junit.Test;
            
            class SomeTestClass {
                @Test(other = 0)
                void foo() {
                }
            }
        """)
    )
}
