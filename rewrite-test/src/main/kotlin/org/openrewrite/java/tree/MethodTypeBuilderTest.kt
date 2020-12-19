package org.openrewrite.java.tree

import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.tree.JavaType.GenericTypeVariable
import org.openrewrite.java.tree.MethodTypeBuilder.newMethodType
import java.lang.IllegalArgumentException

interface MethodTypeBuilderTest {

    @Test
    fun errorThrownIfMethodNameIsNotDefined() {
        assertThatThrownBy {
            newMethodType().declaringClass("A").build() }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("The method name is required.")
    }

    @Test
    fun errorThrownIfDeclaringTypeIsNotDefined() {
        assertThatThrownBy { newMethodType().name("notification").build() }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("The declaring type is required.")
    }

    @Test
    fun methodTypeWithDeclaringType(jp: JavaParser) {
        val a = jp.parse("""
            public class A {
                public void foo() {
                    String str = notification();
                }
                
                String notification() {
                    return "You have been notified";
                }
            }
        """.trimIndent())
        val b = (((a[0].classes[0].body.statements[0] as J.MethodDecl).body.statements[0] as J.VariableDecls).vars[0].initializer.type as JavaType.Method).declaringType

        val m = newMethodType()
            .name("notification")
            .declaringClass("A")
            .build()

        assertThat(m.declaringType).isEqualTo(b)
    }

    @Test
    fun methodTypeWithoutReturnType(jp: JavaParser) {
        val method = newMethodType()
            .name("notification")
            .declaringClass("A")
            .build()

        assertThat(method.resolvedSignature.returnType).isEqualTo(JavaType.Primitive.Void)
        assertThat(method.genericSignature.returnType).isEqualTo(JavaType.Primitive.Void)
    }

    @Test
    fun methodTypeWithReturnType(jp: JavaParser) {
        val cu = jp.parse("""
            public class A {
                public void foo() {
                    String str1 = notification1();
                    String[] str2 = notification2();
                }
                
                String notification1() {
                    return "You have been notified once";
                }
                
                String[] notification2() {
                    return new String[]{"You have been notified", "Of two important things"};
                }
            }
        """.trimIndent())
        val expectedReturnType1 = (((cu[0].classes[0].body.statements[0] as J.MethodDecl).body.statements[0] as J.VariableDecls).vars[0].initializer.type as JavaType.Method).resolvedSignature.returnType
        val expectedReturnType2 = (((cu[0].classes[0].body.statements[0] as J.MethodDecl).body.statements[1] as J.VariableDecls).vars[0].initializer.type as JavaType.Method).resolvedSignature.returnType

        val method1 = newMethodType()
            .name("notification1")
            .declaringClass("A")
            .returnType("java.lang.String")
            .build()

        val method2 = newMethodType()
            .name("notification2")
            .declaringClass("A")
            .returnType(JavaType.Array(JavaType.buildType("java.lang.String")))
            .build()

        assertThat(method1.resolvedSignature.returnType).isEqualTo(expectedReturnType1)
        assertThat(method2.resolvedSignature.returnType).isEqualTo(expectedReturnType2)
    }

    @Test
    fun methodTypeWithGenericReturnType(jp: JavaParser) {
        val cu = jp.parse("""
            public class A {
                public void foo() {
                    String str = notification();
                }
                
                <T> T notification() {
                    return "You have been notified";
                }
            }
        """.trimIndent())
        val expectedReturnType = (((cu[0].classes[0].body.statements[0] as J.MethodDecl).body.statements[0] as J.VariableDecls).vars[0].initializer.type as JavaType.Method)

        val method1 = newMethodType()
            .name("notification")
            .declaringClass("A")
            .returnType(
                "java.lang.String",
                "T"
            )
            .build()

        val method2 = newMethodType()
            .name("notification")
            .declaringClass("A")
            .returnType(
                JavaType.buildType("java.lang.String"),
                GenericTypeVariable("T", JavaType.Class.build("java.lang.Object"))
            )
            .build()

        assertThat(method1.resolvedSignature.returnType).isEqualTo(expectedReturnType.resolvedSignature.returnType)
        assertThat(method1.genericSignature.returnType).isEqualTo(expectedReturnType.genericSignature.returnType)

        assertThat(method2.resolvedSignature.returnType).isEqualTo(expectedReturnType.resolvedSignature.returnType)
        assertThat(method2.genericSignature.returnType).isEqualTo(expectedReturnType.genericSignature.returnType)
    }

    @Test
    fun methodTypeWithParameters(jp: JavaParser) {
        val cu = jp.parse("""
            public class A {
                public void foo() {
                    String str = notification("Something");
                }
                
                String notification(String message) {
                    return "You have been notified of " + message;
                }
            }
        """.trimIndent())
        val expectedParameters = (((cu[0].classes[0].body.statements[0] as J.MethodDecl).body.statements[0] as J.VariableDecls).vars[0].initializer.type as JavaType.Method).resolvedSignature.paramTypes

        val method1 = newMethodType()
            .name("notification")
            .declaringClass("A")
            .parameter("java.lang.String", "message")
            .build()

        val method2 = newMethodType()
            .name("notification")
            .declaringClass("A")
            .parameter(JavaType.buildType("java.lang.String"), "message")
            .build()

        assertThat(method1.resolvedSignature.paramTypes).isEqualTo(expectedParameters)
        assertThat(method2.resolvedSignature.paramTypes).isEqualTo(expectedParameters)

        // when no generic type has been assigned, the generic parameters should be the same as the resolved parameters
        assertThat(method1.resolvedSignature.paramTypes).isEqualTo(method1.genericSignature.paramTypes)
        assertThat(method2.resolvedSignature.paramTypes).isEqualTo(method2.genericSignature.paramTypes)
    }

    @Test
    fun methodTypeWithGenericParameters(jp: JavaParser) {
        val cu = jp.parse("""
            public class A {
                public void foo() {
                    String str = notification("Something");
                }
                
                <T> String notification(T message) {
                    return "You have been notified of " + message;
                }
            }
        """.trimIndent())
        val expectedMethod = (((cu[0].classes[0].body.statements[0] as J.MethodDecl).body.statements[0] as J.VariableDecls).vars[0].initializer.type as JavaType.Method)

        val method1 = newMethodType()
            .name("notification")
            .declaringClass("A")
            .parameter("java.lang.String", "T", "message")
            .build()

        val method2 = newMethodType()
            .name("notification")
            .declaringClass("A")
            .parameter(
                JavaType.buildType("java.lang.String"),
                GenericTypeVariable(
                    "T",
                    JavaType.Class.build("java.lang.Object")
                ),
                "message"
            )
            .build()

        assertThat(method1.resolvedSignature.paramTypes).isEqualTo(expectedMethod.resolvedSignature.paramTypes)
        assertThat(method1.genericSignature.paramTypes).isEqualTo(expectedMethod.genericSignature.paramTypes)

        assertThat(method2.resolvedSignature.paramTypes).isEqualTo(expectedMethod.resolvedSignature.paramTypes)
        assertThat(method2.genericSignature.paramTypes).isEqualTo(expectedMethod.genericSignature.paramTypes)
    }

    @Test
    fun methodTypeWithFlags(jp: JavaParser) {
        val cu = jp.parse("""
            public class A {
                public void foo() {
                    String str = notification();
                }
                
                public static String notification() {
                    return "You have been notified";
                }
            }
        """.trimIndent())

        val expectedMethod = (((cu[0].classes[0].body.statements[0] as J.MethodDecl).body.statements[0] as J.VariableDecls).vars[0].initializer.type as JavaType.Method)

        val method1 = newMethodType()
            .name("notifications")
            .declaringClass("A")
            .returnType("java.lang.String")
            .flags(Flag.Public, Flag.Static)
            .build()

        assertThat(method1.flags).isEqualTo(expectedMethod.flags)
    }
}
