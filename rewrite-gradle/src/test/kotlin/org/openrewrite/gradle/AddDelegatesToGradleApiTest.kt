package org.openrewrite.gradle

import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

class AddDelegatesToGradleApiTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.parser(
            JavaParser.fromJavaVersion()
                .classpath("groovy", "gradle-base-services")
                .build())
            .recipe(AddDelegatesToGradleApi())
    }

    @Test
    fun simpleMethod() = rewriteRun(
        java("""
            package org.gradle.example;
            
            import groovy.lang.Closure;
            import org.gradle.api.Action;
            
            class A {
                void someMethod(Action<String> action) { }
                void someMethod(Closure action) { }
            }
        """,
            """
            package org.gradle.example;
            
            import groovy.lang.Closure;
            import groovy.lang.DelegatesTo;
            import org.gradle.api.Action;
            
            class A {
                void someMethod(Action<String> action) { }
                void someMethod(@DelegatesTo(String.class) Closure action) { }
            }
        """)
    );

    @Test
    fun methodWithBound() = rewriteRun(
        java("""
            package org.gradle.example;
            
            import groovy.lang.Closure;
            import org.gradle.api.Action;
            
            class A {
                void someMethod(Action<? super String> action) { }
                void someMethod(Closure action) { }
            }
        """,
            """
            package org.gradle.example;
            
            import groovy.lang.Closure;
            import groovy.lang.DelegatesTo;
            import org.gradle.api.Action;
            
            class A {
                void someMethod(Action<? super String> action) { }
                void someMethod(@DelegatesTo(String.class) Closure action) { }
            }
        """)
    )

    @Test
    fun methodWithGenericBound() = rewriteRun(
        java("""
            package org.gradle.example;
            
            import groovy.lang.Closure;
            import org.gradle.api.Action;
            
            class A {
                <T extends String> void someMethod(Action<? super T> action) { }
                void someMethod(Closure action) { }
            }
        """,
            """
            package org.gradle.example;
            
            import groovy.lang.Closure;
            import groovy.lang.DelegatesTo;
            import org.gradle.api.Action;
            
            class A {
                <T extends String> void someMethod(Action<? super T> action) { }
                void someMethod(@DelegatesTo(String.class) Closure action) { }
            }
        """)
    )
}
