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
    fun genericTypeVariable() {
        val generic = firstMethodParameter("generic").asParameterized()!!.typeParameters[0] as JavaType.GenericTypeVariable
        assertThat(generic.name).isEqualTo("?")
        assertThat(generic.variance).isEqualTo(COVARIANT)
        assertThat(generic.bounds[0].asFullyQualified()!!.fullyQualifiedName).isEqualTo("org.openrewrite.java.C")
    }

    @Test
    fun genericVariableContravariant() {
        val generic = firstMethodParameter("genericContravariant").asParameterized()!!.typeParameters[0] as JavaType.GenericTypeVariable
        assertThat(generic.name).isEqualTo("?")
        assertThat(generic.variance).isEqualTo(CONTRAVARIANT)
        assertThat(generic.bounds[0].asFullyQualified()!!.fullyQualifiedName).isEqualTo("org.openrewrite.java.C")
    }

    @Test
    fun genericVariableMultipleBounds() {
        val generic = classTypeParameter()
        assertThat(generic.name).isEqualTo("T")
        assertThat(generic.variance).isEqualTo(COVARIANT)
        assertThat(generic.bounds[0].asFullyQualified()!!.fullyQualifiedName).isEqualTo("org.openrewrite.java.JavaTypeGoat")
        assertThat(generic.bounds[1].asFullyQualified()!!.fullyQualifiedName).isEqualTo("org.openrewrite.java.C")
    }

    @Test
    fun genericTypeVariableUnbounded() {
        val generic = firstMethodParameter("genericUnbounded").asParameterized()!!.typeParameters[0] as JavaType.GenericTypeVariable
        assertThat(generic.name).isEqualTo("U")
        assertThat(generic.variance).isEqualTo(INVARIANT)
        assertThat(generic.bounds).isEmpty()
    }
}
