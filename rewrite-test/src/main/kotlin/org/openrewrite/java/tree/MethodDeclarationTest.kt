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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaTreeTest
import org.openrewrite.java.JavaTreeTest.NestingLevel.Class
import org.openrewrite.java.JavaTreeTest.NestingLevel.CompilationUnit
import java.util.stream.Collectors

interface MethodDeclarationTest : JavaTreeTest {

    @Test
    fun default(jp: JavaParser) = assertParsePrintAndProcess(
            jp, CompilationUnit, """
            public @interface A {
                String foo() default "foo";
            }
        """
    )

    @Test
    fun constructor(jp: JavaParser) = assertParsePrintAndProcess(
            jp, CompilationUnit, """
            public class A {
                public A() { }
            }
        """
    )

    @Test
    fun typeArguments(jp: JavaParser) = assertParsePrintAndProcess(
            jp, Class, """
            public <P, R> R foo(P p, String s, String... args) {
                return null;
            }
        """
    )

    @Test
    fun interfaceMethodDecl(jp: JavaParser) = assertParsePrintAndProcess(
            jp, CompilationUnit, """
            public interface A {
                String getName() ;
            }
        """
    )

    @Test
    fun throws(jp: JavaParser) = assertParsePrintAndProcess(
            jp, Class, """
            public void foo()  throws Exception { }
        """
    )

    @Test
    fun nativeModifier(jp: JavaParser) = assertParsePrintAndProcess(
            jp, Class, """
            public native void foo();
        """
    )

    @Test
    fun methodWithSuffixMultiComment(jp: JavaParser) = assertParsePrintAndProcess(
            jp, Class, """
            public void foo() { }/*Comments*/
        """
    )

    @Test
    fun hasModifier(jp: JavaParser) {
        val a = jp.parse(
                """
            public class A {
                private static boolean foo() { return true; }
            }
        """
        )[0]

        val inv = a.classes[0].body.statements.filterIsInstance<J.MethodDeclaration>().first()
        assertThat(inv.modifiers).hasSize(2)
        assertTrue(inv.hasModifier(J.Modifier.Type.Private))
        assertTrue(inv.hasModifier(J.Modifier.Type.Static))
    }

    @Test
    fun hasThrownExceptionInType(jp: JavaParser) {
        val a = jp.parse(
            """
            import java.io.IOException;
            
            public class A {
                private static boolean foo(boolean flag) throws IOException, ArithmeticException, org.example.WhatTypeOfExceptionIsThis {
                    if (flag) {
                        throw new WhatTypeOfExceptionIsThis();
                    } else {
                        throw new IOException();
                    }
                } 
            }
        """
        )[0]

        val inv = a.classes[0].body.statements.filterIsInstance<J.MethodDeclaration>().first()
        assertThat(inv.methodType!!.thrownExceptions).hasSize(3)

        val fullyQualifiedNames = inv.methodType!!.thrownExceptions.stream()
            .map(JavaType.FullyQualified::getFullyQualifiedName)
            .collect(Collectors.toList())
        assertThat(fullyQualifiedNames).contains("java.io.IOException", "java.lang.ArithmeticException", "org.example.WhatTypeOfExceptionIsThis")
    }

}
