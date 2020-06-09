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
package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openrewrite.Tree
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaSourceVisitor
import org.openrewrite.java.asClass

interface AnnotationTest {
    
    @Test
    fun annotation(jp: JavaParser) {
        val a = jp.parse("""
            @SuppressWarnings("ALL")
            public class A {}
        """)
        
        val ann = a.classes[0].annotations[0]
        
        assertEquals("java.lang.SuppressWarnings", ann.type.asClass()?.fullyQualifiedName)
        assertEquals("ALL", ann.args!!.args.filterIsInstance<J.Literal>().firstOrNull()?.value)
    }
    
    @Test
    fun formatImplicitDefaultArgument(jp: JavaParser) {
        val a = jp.parse("""
            @SuppressWarnings("ALL")
            public class A {}
        """)
        
        val ann = a.classes[0].annotations[0]
        
        assertEquals("@SuppressWarnings(\"ALL\")", ann.printTrimmed())
    }

    @Test
    fun preserveOptionalEmptyParentheses(jp: JavaParser) {
        val a = jp.parse("""
            @Deprecated ( )
            public class A {}
        """)

        val ann = a.classes[0].annotations[0]

        assertEquals("@Deprecated ( )", ann.printTrimmed())
    }

    @Test
    fun default(jp: JavaParser) {
        val a = jp.parse("""
            public @interface A {
                String foo() default "foo";
            }
        """)

        assertEquals("""String foo() default "foo"""", a.classes[0].methods[0].printTrimmed())
    }

    @Test
    fun newArrayArgument(jp: JavaParser) {
        val a = jp.parse("""
            import java.lang.annotation.Target;
            import static java.lang.annotation.ElementType.*;

            @Target({ FIELD, PARAMETER })
            public @interface Annotation {}
        """)

        assertEquals("@Target({ FIELD, PARAMETER })", a.classes[0].annotations[0].printTrimmed())
    }

    @Test
    fun annotationArgTypesVisited(jp: JavaParser) {
        val a = jp.parse("""
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