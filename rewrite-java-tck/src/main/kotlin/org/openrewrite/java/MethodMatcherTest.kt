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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.openrewrite.Issue
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType

@Suppress("ClassInitializerMayBeStatic", "JUnitMalformedDeclaration")
interface MethodMatcherTest {
    fun typeRegex(signature: String) = MethodMatcher(signature).targetTypePattern.toRegex()
    fun nameRegex(signature: String) = MethodMatcher(signature).methodNamePattern.toRegex()
    fun argRegex(signature: String) = MethodMatcher(signature).argumentPattern.toRegex()

    @Test
    @Suppress("rawtypes")
    fun matchesSuperclassTypeOfInterfaces(jp: JavaParser) {
        val listType = (jp.parse("class Test { java.util.List l; }")[0].classes[0].body.statements[0] as J.VariableDeclarations).typeAsFullyQualified
        assertTrue(MethodMatcher("java.util.Collection size()", true).matchesTargetType(listType))
        assertFalse(MethodMatcher("java.util.Collection size()").matchesTargetType(listType))
        // ensuring subtypes do not match parents, regardless of matchOverrides
        assertFalse(MethodMatcher("java.util.List size()", true).matchesTargetType(JavaType.ShallowClass.build("java.util.Collection")))
        assertFalse(MethodMatcher("java.util.List size()").matchesTargetType(JavaType.ShallowClass.build("java.util.Collection")))
    }

    @Test
    fun matchesSuperclassTypeOfClasses(jp: JavaParser) {
        assertTrue(MethodMatcher("Object equals(Object)", true).matchesTargetType(JavaType.ShallowClass.build("java.lang.String")))
        assertFalse(MethodMatcher("Object equals(Object)").matchesTargetType(JavaType.ShallowClass.build("java.lang.String")))
        // ensuring subtypes do not match parents, regardless of matchOverrides
        assertFalse(MethodMatcher("String equals(String)", true).matchesTargetType(JavaType.ShallowClass.build("java.lang.Object")))
        assertFalse(MethodMatcher("String equals(String)").matchesTargetType(JavaType.ShallowClass.build("java.lang.Object")))
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
                A a = new A();
            }
        """.trimIndent()
        )[0]

        assertTrue(
            MethodMatcher("a.A <constructor>()").matches(
                (cu.classes.first().body.statements.first() as J.VariableDeclarations)
                    .variables.first().initializer as J.NewClass
            )
        )
    }

    @Test
    fun matchesConstructorAsExpressionUsage(jp: JavaParser) {
        val cu = jp.parse(
            """
            package a;
            class A {
                A a = new A();
            }
        """.trimIndent()
        )[0]

        assertTrue(
            MethodMatcher("a.A <constructor>()").matches(
                (cu.classes.first().body.statements.first() as J.VariableDeclarations)
                    .variables.first().initializer!!
            )
        )
        assertTrue(
            MethodMatcher("a.A *()").matches(
                (cu.classes.first().body.statements.first() as J.VariableDeclarations)
                    .variables.first().initializer!!
            )
        )
    }

    @Test
    fun matchesMemberReferenceAsExpressionUsage(jp: JavaParser) {
        val cu = jp.parse(
            """
            package a;
            import java.util.function.Supplier;
            
            class A {
                Supplier<A> a = A::new;
            }
        """.trimIndent()
        )[0]

        assertTrue(
            MethodMatcher("a.A <constructor>()").matches(
                (cu.classes.first().body.statements.first() as J.VariableDeclarations)
                    .variables.first().initializer!!
            )
        )
        assertTrue(
            MethodMatcher("a.A *()").matches(
                (cu.classes.first().body.statements.first() as J.VariableDeclarations)
                    .variables.first().initializer!!
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

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1215")
    fun strictMatchMethodOverride(jp: JavaParser) {
        val cu = jp.parse(
            """
                package com.abc;

                class Parent {
                    public void method(String s) {
                    }

                    @Override
                    public String toString() {
                        return "empty";
                    }
                }

                class Test extends Parent {
                    @Override
                    public void method(String s) {
                    }
                }
        """
        ).first()
        val parentMethodDefinition = (cu.classes[0].body.statements[0] as J.MethodDeclaration).methodType
        val childMethodOverride = (cu.classes[1].body.statements[0] as J.MethodDeclaration).methodType
        assertFalse(MethodMatcher("com.abc.Parent method(String)", false).matches(childMethodOverride))
        assertTrue(MethodMatcher("com.abc.Parent method(String)", true).matches(parentMethodDefinition))
        assertTrue(MethodMatcher("com.abc.Parent method(String)", true).matches(childMethodOverride))
        assertTrue(MethodMatcher("com.abc.Parent method(String)", false).matches(parentMethodDefinition))
        assertFalse(MethodMatcher("com.abc.Test method(String)", true).matches(parentMethodDefinition))

        val parentToStringDefinition = (cu.classes[0].body.statements[1] as J.MethodDeclaration).methodType
        assertTrue(MethodMatcher("com.abc.Parent toString()", true).matches(parentToStringDefinition))
        assertTrue(MethodMatcher("java.lang.Object toString()", true).matches(parentToStringDefinition))
        assertFalse(MethodMatcher("java.lang.Object toString()", false).matches(parentToStringDefinition))
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/383")
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

    @Issue("https://github.com/openrewrite/rewrite/issues/492")
    @Test
    @Suppress("SpellCheckingInspection")
    fun matchesWildcardedMethodNameStartingWithJavaKeyword(jp: JavaParser) {
        assertTrue(nameRegex("A assert*()").matches("assertThat"))
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/629")
    @Test
    fun wildcardType(jp: JavaParser) {
        assertTrue(MethodMatcher("*..* build()").matchesTargetType(JavaType.ShallowClass.build("javax.ws.rs.core.Response")))
        assertTrue(MethodMatcher("javax..* build()").matchesTargetType(JavaType.ShallowClass.build("javax.ws.rs.core.Response")))
    }

    @Test
    fun siteExample(jp: JavaParser) {
        val cu = jp.parse(
            """
            package com.yourorg;

            class Foo {
                void bar(int i, String s) {}
                void other() {
                    bar(0, "");
                }
            }
        """
        ).first()
        val classDecl = cu.classes.first()
        val methodDecl = classDecl.body.statements[1] as J.MethodDeclaration
        val fooMethod = methodDecl.body!!.statements[0] as J.MethodInvocation
        assertTrue(MethodMatcher("com.yourorg.Foo bar(int, String)").matches(fooMethod))
    }

    @Test
    fun matchUnknownTypesNoSelect(jp: JavaParser) {
        val mi = "assertTrue(Foo.bar());".asMethodInvocation(jp)
        assertTrue(MethodMatcher("org.junit.Assert assertTrue(boolean)").matches(mi, true))
    }

    @Test
    fun matchUnknownTypesQualifiedStaticMethod(jp: JavaParser) {
        val mi = "Assert.assertTrue(Foo.bar());".asMethodInvocation(jp)
        assertTrue(MethodMatcher("org.junit.Assert assertTrue(boolean)").matches(mi, true))
    }

    @Test
    fun matchUnknownTypesPackageQualifiedStaticMethod(jp: JavaParser) {
        val mi = "org.junit.Assert.assertTrue(Foo.bar());".asMethodInvocation(jp)
        assertTrue(MethodMatcher("org.junit.Assert assertTrue(boolean)").matches(mi, true))
    }

    @Test
    fun matchUnknownTypesWildcardReceiverType(jp: JavaParser) {
        val mi = "Assert.assertTrue(Foo.bar());".asMethodInvocation(jp)
        assertTrue(MethodMatcher("*..* assertTrue(boolean)").matches(mi, true))
    }

    @Test
    fun matchUnknownTypesFullWildcardReceiverType(jp: JavaParser) {
        val mi = "Assert.assertTrue(Foo.bar());".asMethodInvocation(jp)
        assertTrue(MethodMatcher("* assertTrue(boolean)").matches(mi, true))
    }

    @Test
    fun matchUnknownTypesExplicitPackageWildcardReceiverType(jp: JavaParser) {
        val mi = "Assert.assertTrue(Foo.bar());".asMethodInvocation(jp)
        assertTrue(MethodMatcher("org.junit.* assertTrue(boolean)").matches(mi, true))
    }

    @Test
    fun matchUnknownTypesRejectsMismatchedMethodName(jp: JavaParser) {
        val mi = "Assert.assertTrue(Foo.bar());".asMethodInvocation(jp)
        assertFalse(MethodMatcher("org.junit.Assert assertFalse(boolean)").matches(mi, true))
    }

    @Test
    fun matchUnknownTypesRejectsStaticSelectMismatch(jp: JavaParser) {
        val mi = "Assert.assertTrue(Foo.bar());".asMethodInvocation(jp)
        assertFalse(MethodMatcher("org.junit.FooAssert assertTrue(boolean)").matches(mi, true))
    }

    @Test
    fun matchUnknownTypesRejectsTooManyArguments(jp: JavaParser) {
        val mi = """Assert.assertTrue(Foo.bar(), "message");""".asMethodInvocation(jp)
        assertFalse(MethodMatcher("org.junit.Assert assertTrue(boolean)").matches(mi, true))
    }

    @Test
    fun matchUnknownTypesRejectsTooFewArguments(jp: JavaParser) {
        val mi = """Assert.assertTrue(Foo.bar());""".asMethodInvocation(jp)
        assertFalse(MethodMatcher("org.junit.Assert assertTrue(boolean, String)").matches(mi, true))
    }

    @Test
    fun matchUnknownTypesRejectsMismatchingKnownArgument(jp: JavaParser) {
        val mi = """Assert.assertTrue(Foo.bar(), "message");""".asMethodInvocation(jp)
        assertFalse(MethodMatcher("org.junit.Assert assertTrue(boolean, int)").matches(mi, true))
    }

    @Test
    fun matchUnknownTypesWildcardArguments(jp: JavaParser) {
        val mi = """Assert.assertTrue(Foo.bar(), "message");""".asMethodInvocation(jp)
        assertTrue(MethodMatcher("org.junit.Assert assertTrue(..)").matches(mi, true))
    }

    @Test
    fun matchUnknownTypesSingleWildcardArgument(jp: JavaParser) {
        val mi = """Assert.assertTrue(Foo.bar(), "message");""".asMethodInvocation(jp)
        assertTrue(MethodMatcher("org.junit.Assert assertTrue(*, String)").matches(mi, true))
    }

    fun String.asMethodInvocation(jp: JavaParser): J.MethodInvocation {
        val cu = jp.parse(
            """
                class MyTest {
                    void test() {
                        $this
                    }
                }
            """,
        ).first()
        val classDecl = cu.classes.first()
        val testMethod = classDecl.body.statements[0] as J.MethodDeclaration
        return testMethod.body!!.statements[0] as J.MethodInvocation
    }
    @Test
    fun arrayExample(jp: JavaParser) {
        val cu = jp.parse(
            """
            package com.yourorg;

            class Foo {
                void bar(String[] s) {}
            }
        """
        ).first()
        val classDecl = cu.classes.first()
        val methodDecl = classDecl.body.statements[0] as J.MethodDeclaration
        assertEquals("MethodDeclaration{com.yourorg.Foo{name=bar,return=void,parameters=[java.lang.String[]]}}", methodDecl.toString())
        assertTrue(MethodMatcher("com.yourorg.Foo bar(String[])").matches(methodDecl, classDecl))
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2261")
    @Test
    fun matcherForUnknownType(jp: JavaParser) {
        val cu = jp.parse("""
            class Test {
                void foo(Unknown u) {}
            }
        """).first()
        val methodDecl = cu.classes[0].body.statements[0] as J.MethodDeclaration

        val matcher = MethodMatcher(MethodMatcher.methodPattern(methodDecl))
        assertTrue(matcher.matches(methodDecl.methodType))
    }
}
