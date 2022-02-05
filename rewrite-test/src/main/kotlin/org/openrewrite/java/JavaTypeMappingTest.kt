/*
 * Copyright 2021 the original author or authors.
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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.java.tree.Flag
import org.openrewrite.java.tree.JavaType
import org.openrewrite.java.tree.JavaType.GenericTypeVariable.Variance.*

/**
 * Based on type attribution mappings of [JavaTypeGoat].
 */
interface JavaTypeMappingTest {
    fun classType(fqn: String): JavaType.FullyQualified

    fun goatType(): JavaType.Parameterized = classType("org.openrewrite.java.JavaTypeGoat").asParameterized()!!

    fun methodType(methodName: String): JavaType.Method {
        val type = goatType().methods.find { it.name == methodName }!!
        assertThat(type.declaringType.toString()).isEqualTo("org.openrewrite.java.JavaTypeGoat")
        return type
    }

    fun firstMethodParameter(methodName: String): JavaType = methodType(methodName).parameterTypes[0]

    @Test
    fun javaLangObjectHasNoSupertype() {
        assertThat(goatType().supertype.supertype).isNull()
    }

    @Test
    fun interfacesContainImplicitAbstractFlag() {
        val clazz = firstMethodParameter("clazz") as JavaType.Class
        val methodType = methodType("clazz")
        assertThat(clazz.flags.contains(Flag.Abstract))
        assertThat(methodType.flags.contains(Flag.Abstract))
    }

    @Test
    fun extendsJavaLangObject() {
        // even though it is implicit in the source code...
        assertThat(goatType().supertype.fullyQualifiedName).isEqualTo("java.lang.Object")
    }

    @Test
    fun constructor() {
        val ctor = methodType("<constructor>")
        assertThat(ctor.declaringType.fullyQualifiedName).isEqualTo("org.openrewrite.java.JavaTypeGoat")
    }

    @Test
    fun array() {
        val arr = firstMethodParameter("array") as JavaType.Array
        assertThat(arr.elemType.asArray()).isNotNull
        assertThat(arr.elemType.asArray()!!.elemType.asFullyQualified()!!.fullyQualifiedName).isEqualTo("org.openrewrite.java.C")
    }

    @Test
    fun className() {
        val clazz = firstMethodParameter("clazz") as JavaType.Class
        assertThat(clazz.asFullyQualified()!!.fullyQualifiedName).isEqualTo("org.openrewrite.java.C")
    }

    @Test
    fun primitive() {
        val primitive = firstMethodParameter("primitive") as JavaType.Primitive
        assertThat(primitive).isSameAs(JavaType.Primitive.Int)
    }

    @Test
    fun parameterized() {
        val parameterized = firstMethodParameter("parameterized") as JavaType.Parameterized
        assertThat(parameterized.type.fullyQualifiedName).isEqualTo("org.openrewrite.java.PT")
        assertThat(parameterized.typeParameters[0].asFullyQualified()!!.fullyQualifiedName).isEqualTo("org.openrewrite.java.C")
    }

    @Test
    fun generic() {
        val generic =
            firstMethodParameter("generic").asParameterized()!!.typeParameters[0] as JavaType.GenericTypeVariable
        assertThat(generic.variance).isEqualTo(COVARIANT)
        assertThat(generic.bounds[0].asFullyQualified()!!.fullyQualifiedName).isEqualTo("org.openrewrite.java.C")
    }

    @Test
    fun genericContravariant() {
        val generic =
            firstMethodParameter("genericContravariant").asParameterized()!!.typeParameters[0] as JavaType.GenericTypeVariable
        assertThat(generic.variance).isEqualTo(CONTRAVARIANT)
        assertThat(generic.bounds[0].asFullyQualified()!!.fullyQualifiedName).isEqualTo("org.openrewrite.java.C")
    }

    @Test
    fun genericMultipleBounds() {
        val generic = goatType().typeParameters.last().asGeneric()!!
        assertThat(generic.variance).isEqualTo(COVARIANT)
        assertThat(generic.bounds[0].asFullyQualified()!!.fullyQualifiedName).isEqualTo("org.openrewrite.java.PT")
        assertThat(generic.bounds[1].asFullyQualified()!!.fullyQualifiedName).isEqualTo("org.openrewrite.java.C")
    }

    @Test
    fun genericUnbounded() {
        val generic =
            firstMethodParameter("genericUnbounded").asParameterized()!!.typeParameters[0] as JavaType.GenericTypeVariable
        assertThat(generic.variance).isEqualTo(INVARIANT)
        assertThat(generic.bounds).isEmpty()
    }

    @Test
    fun genericRecursive() {
        val param = firstMethodParameter("genericRecursive")
        val typeParam = param.asParameterized()!!.typeParameters[0]
        val generic = typeParam as JavaType.GenericTypeVariable
        assertThat(generic.variance).isEqualTo(COVARIANT)
        assertThat(generic.bounds[0].asArray()).isNotNull

        val elemType = generic.bounds[0].asArray()!!.elemType.asGeneric()!!
        assertThat(elemType.variance).isEqualTo(COVARIANT)
        assertThat(elemType.bounds).hasSize(1)
    }

    @Test
    fun genericArray() {
        val param = firstMethodParameter("genericArray")
        val arr = param as JavaType.Array
        val parameterized = arr.elemType.asParameterized()

        assertThat(parameterized).isNotNull
        assertThat(parameterized!!.type.fullyQualifiedName).isEqualTo("org.openrewrite.java.PT")
        assertThat(parameterized.typeParameters[0].asFullyQualified()!!.fullyQualifiedName).isEqualTo("org.openrewrite.java.C")
    }

    @Test
    fun innerClass() {
        val clazz = firstMethodParameter("inner").asFullyQualified()!!
        assertThat(clazz.fullyQualifiedName).isEqualTo("org.openrewrite.java.C${"$"}Inner")
    }

    @Disabled("Disabled until Classgraph is removed.")
    @Test
    fun inheritedJavaTypeGoat() {
        val clazz = firstMethodParameter("inheritedJavaTypeGoat") as JavaType.Parameterized
        assertThat(clazz.typeParameters[0].toString()).isEqualTo("Generic{}")
        assertThat(clazz.typeParameters[1].toString()).isEqualTo("Generic{extends org.openrewrite.java.PT<Generic{}> & org.openrewrite.java.C}")
        assertThat(clazz.toString()).isEqualTo("org.openrewrite.java.JavaTypeGoat${"$"}InheritedJavaTypeGoat<Generic{}, Generic{extends org.openrewrite.java.PT<Generic{}> & org.openrewrite.java.C}>")
    }

    @Disabled("Disabled until Classgraph is removed.")
    @Issue("https://github.com/openrewrite/rewrite/pull/1365")
    @Test
    fun genericIntersectionType() {
        val clazz = firstMethodParameter("genericIntersection") as JavaType.GenericTypeVariable
        assertThat(clazz.bounds[0].toString()).isEqualTo("org.openrewrite.java.JavaTypeGoat${"$"}TypeA")
        assertThat(clazz.bounds[1].toString()).isEqualTo("org.openrewrite.java.PT<Generic{extends org.openrewrite.java.JavaTypeGoat${"$"}TypeA & org.openrewrite.java.C}>")
        assertThat(clazz.bounds[2].toString()).isEqualTo("org.openrewrite.java.C")
        assertThat(clazz.toString()).isEqualTo("Generic{extends org.openrewrite.java.JavaTypeGoat${"$"}TypeA & org.openrewrite.java.PT<Generic{}> & org.openrewrite.java.C}")
    }

    @Disabled("Temporarily disabled: JavaReflection returns the implicit constructor that is added by the compiler, and ClassgraphTypeMapping returns the wrong type.")
    @Issue("https://github.com/openrewrite/rewrite/issues/1349")
    @Test
    fun enumType() {
        val clazz = firstMethodParameter("enumType") as JavaType.Class
        val type = clazz.methods.find { it.name == "<constructor>" }
        assertThat(type).isNull()

        val supertype = clazz.supertype
        assertThat(supertype).isNotNull
        assertThat(supertype!!.toString()).isEqualTo("java.lang.Enum<org.openrewrite.java.JavaTypeGoat${"$"}EnumType>")
    }

    @Test
    fun ignoreSourceRetentionAnnotations() {
        val goat = goatType()
        assertThat(goat.annotations.size == 1)
        assertThat(goat.annotations.first().className == "AnnotationWithRuntimeRetention")

        val clazzMethod = methodType("clazz")
        assertThat(clazzMethod.annotations.size == 1)
        assertThat(clazzMethod.annotations.first().className == "AnnotationWithRuntimeRetention")
    }
}
