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
import org.junit.jupiter.api.Test
import org.openrewrite.java.tree.JavaType
import org.openrewrite.java.tree.JavaType.GenericTypeVariable.Variance.*

/**
 * Based on type attribution mappings of [JavaTypeGoat].
 */
interface JavaTypeMappingTest {
    /**
     * Type attribution for the [JavaTypeGoat] class.
     */
    fun goatType() : JavaType.Parameterized

    /**
     * The type of the first parameter of the method named [methodName].
     */
    fun firstMethodParameter(methodName: String): JavaType =
        goatType().methods.find { it.name == methodName }!!.genericSignature!!.paramTypes[0]

    /**
     * The type of the first type parameter of [JavaTypeGoat].
     */
    fun classTypeParameter(): JavaType.GenericTypeVariable =
        goatType().typeParameters[0].asGeneric()!!

    @Test
    fun arraySignature() {
        val arr = firstMethodParameter("array") as JavaType.Array
        assertThat(arr.elemType.asFullyQualified()!!.fullyQualifiedName).isEqualTo("org.openrewrite.java.C")
    }

    @Test
    fun classSignature() {
        val clazz = firstMethodParameter("clazz") as JavaType.Class
        assertThat(clazz.asFullyQualified()!!.fullyQualifiedName).isEqualTo("org.openrewrite.java.C")
    }

    @Test
    fun primitiveSignature() {
        val primitive = firstMethodParameter("primitive") as JavaType.Primitive
        assertThat(primitive).isSameAs(JavaType.Primitive.Int)
    }

    @Test
    fun parameterizedSignature() {
        val parameterized = firstMethodParameter("parameterized") as JavaType.Parameterized
        assertThat(parameterized.type!!.fullyQualifiedName).isEqualTo("org.openrewrite.java.PT")
        assertThat(parameterized.typeParameters[0].asFullyQualified()!!.fullyQualifiedName).isEqualTo("org.openrewrite.java.C")
    }

    @Test
    fun generic() {
        val generic = firstMethodParameter("generic").asParameterized()!!.typeParameters[0] as JavaType.GenericTypeVariable
        assertThat(generic.name).isEqualTo("?")
        assertThat(generic.variance).isEqualTo(COVARIANT)
        assertThat(generic.bounds[0].asFullyQualified()!!.fullyQualifiedName).isEqualTo("org.openrewrite.java.C")
    }

    @Test
    fun genericContravariant() {
        val generic = firstMethodParameter("genericContravariant").asParameterized()!!.typeParameters[0] as JavaType.GenericTypeVariable
        assertThat(generic.name).isEqualTo("?")
        assertThat(generic.variance).isEqualTo(CONTRAVARIANT)
        assertThat(generic.bounds[0].asFullyQualified()!!.fullyQualifiedName).isEqualTo("org.openrewrite.java.C")
    }

    @Test
    fun genericMultipleBounds() {
        val generic = classTypeParameter()
        assertThat(generic.name).isEqualTo("T")
        assertThat(generic.variance).isEqualTo(COVARIANT)
        assertThat(generic.bounds[0].asFullyQualified()!!.fullyQualifiedName).isEqualTo("org.openrewrite.java.JavaTypeGoat")
        assertThat(generic.bounds[1].asFullyQualified()!!.fullyQualifiedName).isEqualTo("org.openrewrite.java.C")
    }

    @Test
    fun genericUnbounded() {
        val generic = firstMethodParameter("genericUnbounded").asParameterized()!!.typeParameters[0] as JavaType.GenericTypeVariable
        assertThat(generic.name).isEqualTo("U")
        assertThat(generic.variance).isEqualTo(INVARIANT)
        assertThat(generic.bounds).isEmpty()
    }
}
