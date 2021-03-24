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

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType

interface MethodMatcherTest {
    fun typeRegex(signature: String) = MethodMatcher(signature).targetTypePattern.toRegex()
    fun nameRegex(signature: String) = MethodMatcher(signature).methodNamePattern.toRegex()
    fun argRegex(signature: String) = MethodMatcher(signature).argumentPattern.toRegex()

    @Test
    fun matchesSuperclassType(jp: JavaParser) {
        assertTrue(MethodMatcher("Object equals(Object)").matchesTargetType(JavaType.Class.build("java.lang.String")))
        assertFalse(MethodMatcher("String equals(String)").matchesTargetType(JavaType.Class.build("java.lang.Object")))
    }

    @Test
    fun matchesMethodTargetType(jp: JavaParser) {
        assertTrue(typeRegex("*..MyClass foo()").matches("com.bar.MyClass"))
        assertTrue(typeRegex("MyClass foo()").matches("MyClass"))
        assertTrue(typeRegex("com.bar.MyClass foo()").matches("com.bar.MyClass"))
        assertTrue(typeRegex("com.*.MyClass foo()").matches("com.bar.MyClass"))
    }


    @Test
    fun matchesMethodNameWithDotSeparator(jp: JavaParser) {
        assertTrue(nameRegex("A.foo()").matches("foo"))
        assertTrue(nameRegex("A.*()").matches("foo"))
        assertTrue(nameRegex("A.fo*()").matches("foo"))
    }

    @Test
    fun matchesMethodNameWithPoundSeparator(jp: JavaParser) {
        assertTrue(nameRegex("A#foo()").matches("foo"))
        assertTrue(nameRegex("A#*()").matches("foo"))
        assertTrue(nameRegex("A#fo*()").matches("foo"))
        assertTrue(nameRegex("A#*oo()").matches("foo"))
    }

    @Test
    fun matchesMethodName(jp: JavaParser) {
        assertTrue(nameRegex("A foo()").matches("foo"))
        assertTrue(nameRegex("A *()").matches("foo"))
        assertTrue(nameRegex("A fo*()").matches("foo"))
        assertTrue(nameRegex("A *oo()").matches("foo"))
    }

    @Test
    fun matchesArguments(jp: JavaParser) {
        assertTrue(argRegex("A foo()").matches(""))
        assertTrue(argRegex("A foo(int)").matches("int"))
        assertTrue(argRegex("A foo(java.util.Map)").matches("java.util.Map"))
    }

    @Test
    fun matchesUnqualifiedJavaLangArguments(jp: JavaParser) {
        assertTrue(argRegex("A foo(String)").matches("java.lang.String"))
    }

    @Test
    fun matchesArgumentsWithWildcards(jp: JavaParser) {
        assertTrue(argRegex("A foo(java.util.*)").matches("java.util.Map"))
        assertTrue(argRegex("A foo(java..*)").matches("java.util.Map"))
    }

    @Test
    fun matchesArgumentsWithDotDot(jp: JavaParser) {
        assertTrue(argRegex("A foo(.., int)").matches("int"))
        assertTrue(argRegex("A foo(.., int)").matches("int,int"))

        assertTrue(argRegex("A foo(int, ..)").matches("int"))
        assertTrue(argRegex("A foo(int, ..)").matches("int,int"))

        assertTrue(argRegex("A foo(..)").matches(""))
        assertTrue(argRegex("A foo(..)").matches("int"))
        assertTrue(argRegex("A foo(..)").matches("int,int"))
    }

    @Test
    fun matchesSuperclassArgumentTypes(jp: JavaParser) {
        assertTrue(MethodMatcher("Object equals(Object)").matchesTargetType(JavaType.Class.build("java.lang.String")))
        assertFalse(MethodMatcher("String equals(String)").matchesTargetType(JavaType.Class.build("java.lang.Object")))
    }

    @Test
    fun matchesMethodSymbolsWithVarargs(jp: JavaParser) {
        argRegex("A foo(String, Object...)").matches("String,Object[]")
    }

    @Test
    fun dotDotMatchesArrayArgs(jp: JavaParser) {
        argRegex("A foo(..)").matches("String,Object[]")
    }

    @Test
    fun matchesArrayArguments(jp: JavaParser) {
        assertTrue(argRegex("A foo(String[])").matches("java.lang.String[]"))
    }

    @Test
    @Disabled("Reproduces issue https://github.com/openrewrite/rewrite/issues/28")
    fun matchesPrimitiveArgument(jp: JavaParser) {
        assertTrue(argRegex("A foo(int)").matches("int"))
        assertTrue(argRegex("A foo(int[])").matches("int[]"))
        assertFalse(argRegex("A foo(int[])").matches("int"))
    }

    @Test
    fun matchesConstructorUsage(jp: JavaParser) {
        val cu = jp.parse(
            """
            package a;

            class A { 
                {
                    A a = new A(); 
                }
            }
        """.trimIndent()
        )[0]

        assertTrue(
            MethodMatcher("a.A A()").matches(
                ((cu.classes.first().body.statements.first() as J.Block)
                    .statements.first() as J.VariableDeclarations)
                    .variables.first().initializer as J.NewClass
            )
        )
    }

    @Test
    fun matchesMethod(jp: JavaParser) {
        val cu = jp.parse(
            """
            package a;
            
            class A {
                void setInt(int value) {}
                int getInt() {}
                void setInteger(Integer value) {}
                Integer getInteger(){}
            }
        """.trimIndent()
        ).first()
        val classDecl = cu.classes.first()
        val setIntMethod = classDecl.body.statements[0] as J.MethodDeclaration
        val getIntMethod = classDecl.body.statements[1] as J.MethodDeclaration
        val setIntegerMethod = classDecl.body.statements[2] as J.MethodDeclaration
        val getIntegerMethod = classDecl.body.statements[3] as J.MethodDeclaration
        assertTrue(MethodMatcher("a.A setInt(int)").matches(setIntMethod, classDecl))
        assertTrue(MethodMatcher("a.A getInt()").matches(getIntMethod, classDecl))
        assertTrue(MethodMatcher("a.A setInteger(Integer)").matches(setIntegerMethod, classDecl))
        assertTrue(MethodMatcher("a.A getInteger()").matches(getIntegerMethod, classDecl))
    }

    @Disabled
    @Issue("#383")
    @Test
    fun matchesMethodWithWildcardForClassInPackage(jp: JavaParser) {
        val cu = jp.parse(
            """
            package a;
            
            class A {
                void foo() {}
            }
        """
        ).first()
        val classDecl = cu.classes.first()
        val fooMethod = classDecl.body.statements[0] as J.MethodDeclaration
        assertTrue(MethodMatcher("* foo(..)").matches(fooMethod, classDecl))
    }

    @Test
    fun matchesMethodWithWildcardForClassNotInPackage(jp: JavaParser) {
        val cu = jp.parse(
            """
            class A {
                void foo() {}
            }
        """
        ).first()
        val classDecl = cu.classes.first()
        val fooMethod = classDecl.body.statements[0] as J.MethodDeclaration
        assertTrue(MethodMatcher("* foo(..)").matches(fooMethod, classDecl))
    }
}
