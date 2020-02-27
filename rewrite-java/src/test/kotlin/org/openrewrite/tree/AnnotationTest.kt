/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.tree

import org.junit.Assert.assertEquals
import org.junit.Test
import org.openrewrite.Parser
import org.openrewrite.asClass
import org.openrewrite.visitor.AstVisitor

open class AnnotationTest : Parser() {
    
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

        assertEquals(true, object: AstVisitor<Boolean>() {
            override fun defaultTo(t: Tree?): Boolean = false

            override fun visitTypeName(name: NameTree): Boolean = name.type.asClass()?.fullyQualifiedName == "java.lang.annotation.ElementType"
        }.visit(a))
    }
}