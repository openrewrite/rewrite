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

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaTreeTest
import org.openrewrite.java.JavaTreeTest.NestingLevel.Class
import org.openrewrite.java.JavaTreeTest.NestingLevel.CompilationUnit

interface MethodDeclTest : JavaTreeTest {

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
    fun hasModifier(jp: JavaParser) {
        val a = jp.parse(
            """
            public class A {
                private static boolean foo() { return true; };
            }
        """
        )[0]

        val inv = a.classes[0].body.statements.filterIsInstance<J.MethodDecl>().first()
        assertTrue(inv.hasModifier(J.Modifier.Type.Private))
        assertTrue(inv.hasModifier(J.Modifier.Type.Static))
        assertFalse(inv.hasModifier(J.Modifier.Type.Default))
    }

    @Test
    fun methodWithSuffixMultiComment(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Class, """
            public void foo() { }/*Comments*/
        """
    )

}
