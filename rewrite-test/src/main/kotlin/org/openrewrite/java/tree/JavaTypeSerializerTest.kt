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

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.cfg.ConstructorDetector
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.TreeSerializer
import org.openrewrite.java.JavaParser

interface JavaTypeSerializerTest {
    fun treeSerializer() : TreeSerializer<J.CompilationUnit> = TreeSerializer()

    fun objectMapper() : ObjectMapper {
        val m = JsonMapper.builder()
            .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
            .build()
            .registerModule(ParameterNamesModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)

         return m.setVisibility(m.serializationConfig.defaultVisibilityChecker
            .withCreatorVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
            .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
            .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
            .withFieldVisibility(JsonAutoDetect.Visibility.ANY))
    }

    @Test
    fun serializeClass(jp: JavaParser) {
        val clazz = jp.parse("""
            import java.util.ArrayList;
            import java.util.List;

            public abstract class A extends ArrayList<String> implements List<String> {
                protected static final class B extends ArrayList<String> implements List<String> {
                }
            
                private class C extends ArrayList<String> implements List<String> {
                }

                class D extends ArrayList<String> implements List<String> {
                }
            }
        """)[0].classes[0]

        val mapper = objectMapper()

        //Outer class A
        var javaClass = clazz.type
        var classTypeOut = mapper.writeValueAsString(javaClass)
        var copy = mapper.readValue(classTypeOut, JavaType.Class::class.java)

        assertThat(TypeUtils.deepEquals(javaClass, copy)).isTrue
        assertThat(copy.flags).hasSize(2)
        assertThat(copy.hasFlags(Flag.Public, Flag.Abstract)).isTrue

        //Inner Class B
        javaClass = (clazz.body.statements[0] as J.ClassDeclaration).type
        classTypeOut = mapper.writeValueAsString(javaClass)
        copy = mapper.readValue(classTypeOut, JavaType.Class::class.java)

        assertThat(TypeUtils.deepEquals(javaClass, copy)).isTrue
        assertThat(copy.flags).hasSize(3)
        assertThat(copy.hasFlags(Flag.Protected, Flag.Static, Flag.Final)).isTrue

        //Inner Class C
        javaClass = (clazz.body.statements[1] as J.ClassDeclaration).type
        classTypeOut = mapper.writeValueAsString(javaClass)
        copy = mapper.readValue(classTypeOut, JavaType.Class::class.java)

        assertThat(TypeUtils.deepEquals(javaClass, copy)).isTrue
        assertThat(copy.flags).hasSize(1)
        assertThat(copy.hasFlags(Flag.Private)).isTrue

        //Inner Class D
        javaClass = (clazz.body.statements[2] as J.ClassDeclaration).type
        classTypeOut = mapper.writeValueAsString(javaClass)
        copy = mapper.readValue(classTypeOut, JavaType.Class::class.java)

        assertThat(TypeUtils.deepEquals(javaClass, copy)).isTrue
        assertThat(copy.flags).hasSize(0)
    }

    @Suppress("rawtypes")
    @Test
    fun serializeWildcard(jp: JavaParser) {
        val clazz = jp.parse("""
            import java.util.ArrayList;
            import java.util.List;
            class E <T extends Number> {
                List<? extends Number> numberList1 = new ArrayList<>();
                List<? super Number> numberList2 = new ArrayList<>();
                List<?> numberList3 = new ArrayList();
                List<? extends T> numberList4 = new ArrayList<>();
            }
        """)[0].classes[0]

        val mapper = objectMapper()

        //Make sure the whole class serializes properly
        val classOut = mapper.writeValueAsString(clazz)
        val classCopy = mapper.readValue(classOut, J.ClassDeclaration::class.java)
        assertThat(clazz.equals(classCopy)).isTrue

        //NumberList1
        var variableType = (clazz.body.statements[0] as J.VariableDeclarations).variables[0].type
        var variableTypeOut = mapper.writeValueAsString(variableType)
        var copy = mapper.readValue(variableTypeOut, JavaType.Parameterized::class.java)
        var typeParameter = (variableType as JavaType.Parameterized).typeParameters[0] as JavaType.Class
        assertThat(TypeUtils.deepEquals(variableType, copy)).isTrue
        assertThat(typeParameter.fullyQualifiedName).isEqualTo("java.lang.Number")

        //NumberList2
        variableType = (clazz.body.statements[1] as J.VariableDeclarations).variables[0].type
        variableTypeOut = mapper.writeValueAsString(variableType)
        copy = mapper.readValue(variableTypeOut, JavaType.Parameterized::class.java)
        typeParameter = (variableType as JavaType.Parameterized).typeParameters[0] as JavaType.Class
        assertThat(TypeUtils.deepEquals(variableType, copy)).isTrue
        assertThat(typeParameter.fullyQualifiedName).isEqualTo("java.lang.Number")

        //NumberList3
        variableType = (clazz.body.statements[2] as J.VariableDeclarations).variables[0].type
        variableTypeOut = mapper.writeValueAsString(variableType)
        copy = mapper.readValue(variableTypeOut, JavaType.Parameterized::class.java)
        typeParameter = (variableType as JavaType.Parameterized).typeParameters[0] as JavaType.Class
        assertThat(TypeUtils.deepEquals(variableType, copy)).isTrue
        assertThat(typeParameter.fullyQualifiedName).isEqualTo("java.lang.Object")

        //NumberList4
        variableType = (clazz.body.statements[3] as J.VariableDeclarations).variables[0].type
        variableTypeOut = mapper.writeValueAsString(variableType)
        copy = mapper.readValue(variableTypeOut, JavaType.Parameterized::class.java)
        val genericTypeParameter = (variableType as JavaType.Parameterized).typeParameters[0] as JavaType.GenericTypeVariable
        assertThat(TypeUtils.deepEquals(variableType, copy)).isTrue
        assertThat(genericTypeParameter.bound?.fullyQualifiedName).isEqualTo("java.lang.Number")

    }

    @Test
    fun serializeMethod(jp: JavaParser) {
        val statements = jp.parse("""
            import java.util.List;

            public abstract class A {
                public abstract String getFoo(List<String> list);
                private static String getFoo2(List<String> list) {
                    return null;
                }
                protected final String getFoo3(List<String> list) {
                    return null;
                }
                String getFoo4(List<String> list) {
                    return null;
                }
            }
        """)[0].classes[0].body.statements

        val mapper = objectMapper()

        //getFoo
        var methodType = (statements[0] as J.MethodDeclaration).type
        var methodTypeOut = mapper.writeValueAsString(methodType)
        var copy = mapper.readValue(methodTypeOut, JavaType.Method::class.java)

        assertThat(TypeUtils.deepEquals(methodType, copy)).isTrue
        assertThat(copy.flags).hasSize(2)
        assertThat(copy.hasFlags(Flag.Public, Flag.Abstract)).isTrue

        //getFoo2
        methodType = (statements[1] as J.MethodDeclaration).type
        methodTypeOut = mapper.writeValueAsString(methodType)
        copy = mapper.readValue(methodTypeOut, JavaType.Method::class.java)

        assertThat(TypeUtils.deepEquals(methodType, copy)).isTrue
        assertThat(copy.flags).hasSize(2)
        assertThat(copy.hasFlags(Flag.Private, Flag.Static)).isTrue

        //getFoo3
        methodType = (statements[2] as J.MethodDeclaration).type
        methodTypeOut = mapper.writeValueAsString(methodType)
        copy = mapper.readValue(methodTypeOut, JavaType.Method::class.java)

        assertThat(TypeUtils.deepEquals(methodType, copy)).isTrue
        assertThat(copy.flags).hasSize(2)
        assertThat(copy.hasFlags(Flag.Protected, Flag.Final)).isTrue

        //getFoo4
        methodType = (statements[3] as J.MethodDeclaration).type
        methodTypeOut = mapper.writeValueAsString(methodType)
        copy = mapper.readValue(methodTypeOut, JavaType.Method::class.java)

        assertThat(TypeUtils.deepEquals(methodType, copy)).isTrue
        assertThat(copy.flags).isEmpty()
    }

    @Test
    fun serializeVariable(jp: JavaParser) {
        @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS") val members = (jp.parse("""
            public abstract class A {
                public String getFoo;
                private final String getFoo2 = "fred";
                protected static String getFoo3;
                Long getFoo4;
            }
        """)[0].classes[0].type as JavaType.Class).members

        val mapper = objectMapper()

        //getFoo
        var variableType = members[0]
        var variableTypeOut = mapper.writeValueAsString(variableType)
        var copy = mapper.readValue(variableTypeOut, JavaType.Variable::class.java)

        assertThat(TypeUtils.deepEquals(variableType, copy)).isTrue
        assertThat(copy.flags).hasSize(1)
        assertThat(copy.hasFlags(Flag.Public)).isTrue

        //getFoo2
        variableType = members[1]
        variableTypeOut = mapper.writeValueAsString(variableType)
        copy = mapper.readValue(variableTypeOut, JavaType.Variable::class.java)

        assertThat(TypeUtils.deepEquals(variableType, copy)).isTrue
        assertThat(copy.flags).hasSize(3)
        assertThat(copy.hasFlags(Flag.Private, Flag.Final)).isTrue

        //getFoo3
        variableType = members[2]
        variableTypeOut = mapper.writeValueAsString(variableType)
        copy = mapper.readValue(variableTypeOut, JavaType.Variable::class.java)

        assertThat(TypeUtils.deepEquals(variableType, copy)).isTrue
        assertThat(copy.flags).hasSize(2)
        assertThat(copy.hasFlags(Flag.Protected, Flag.Static)).isTrue

        //getFoo4
        variableType = members[3]
        variableTypeOut = mapper.writeValueAsString(variableType)
        copy = mapper.readValue(variableTypeOut, JavaType.Variable::class.java)

        assertThat(TypeUtils.deepEquals(variableType, copy)).isTrue
        assertThat(copy.flags).isEmpty()
    }
}
