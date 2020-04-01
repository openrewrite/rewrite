package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openrewrite.Tree
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaSourceVisitor
import org.openrewrite.java.asClass

open class AnnotationTest : JavaParser() {
    
    @Test
    fun annotation() {
        val a = parse("""
            @SuppressWarnings("ALL")
            public class A {}
        """)
        
        val ann = a.classes[0].annotations[0]
        
        assertEquals("java.lang.SuppressWarnings", ann.type.asClass()?.fullyQualifiedName)
        assertEquals("ALL", ann.args!!.args.filterIsInstance<J.Literal>().firstOrNull()?.value)
    }
    
    @Test
    fun formatImplicitDefaultArgument() {
        val a = parse("""
            @SuppressWarnings("ALL")
            public class A {}
        """)
        
        val ann = a.classes[0].annotations[0]
        
        assertEquals("@SuppressWarnings(\"ALL\")", ann.printTrimmed())
    }

    @Test
    fun preserveOptionalEmptyParentheses() {
        val a = parse("""
            @Deprecated ( )
            public class A {}
        """)

        val ann = a.classes[0].annotations[0]

        assertEquals("@Deprecated ( )", ann.printTrimmed())
    }

    @Test
    fun default() {
        val a = parse("""
            public @interface A {
                String foo() default "foo";
            }
        """)

        assertEquals("""String foo() default "foo"""", a.classes[0].methods[0].printTrimmed())
    }

    @Test
    fun newArrayArgument() {
        val a = parse("""
            import java.lang.annotation.Target;
            import static java.lang.annotation.ElementType.*;

            @Target({ FIELD, PARAMETER })
            public @interface Annotation {}
        """)

        assertEquals("@Target({ FIELD, PARAMETER })", a.classes[0].annotations[0].printTrimmed())
    }

    @Test
    fun annotationArgTypesVisited() {
        val a = parse("""
            import java.lang.annotation.Target;
            import static java.lang.annotation.ElementType.*;

            @Target({ FIELD, PARAMETER })
            public @interface Annotation {}
        """)

        assertEquals(true, object: JavaSourceVisitor<Boolean>() {
            override fun defaultTo(t: Tree?): Boolean = false

            override fun visitTypeName(name: NameTree): Boolean = name.type.asClass()?.fullyQualifiedName == "java.lang.annotation.ElementType"
        }.visit(a))
    }
}