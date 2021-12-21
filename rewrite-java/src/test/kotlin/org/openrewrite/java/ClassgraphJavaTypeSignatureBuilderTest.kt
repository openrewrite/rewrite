package org.openrewrite.java

import io.github.classgraph.ClassGraph
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.java.internal.ClassgraphJavaTypeSignatureBuilder

class ClassgraphJavaTypeSignatureBuilderTest : JavaTypeSignatureBuilderTest {
    companion object {
        private val goat = ClassGraph()
            .enableMemoryMapping()
            .enableClassInfo()
            .enableMethodInfo()
            .acceptClasses("org.openrewrite.java.*")
            .acceptClasses("java.lang.*")
            .scan()
            .getClassInfo("org.openrewrite.java.JavaTypeGoat")
    }

    private val signatureBuilder = ClassgraphJavaTypeSignatureBuilder()

    @Test
    override fun arraySignature() {
        assertThat(signatureBuilder.signature((goat.getMethodInfo("array")[0]
            .parameterInfo[0].typeDescriptor))).isEqualTo("java.lang.Integer[]")
    }

    @Test
    override fun classSignature() {
        assertThat(signatureBuilder.signature((goat.getMethodInfo("clazz")[0]
            .parameterInfo[0].typeDescriptor))).isEqualTo("java.lang.Integer")
    }

    @Test
    override fun primitiveSignature() {
        assertThat(signatureBuilder.signature((goat.getMethodInfo("primitive")[0]
            .parameterInfo[0].typeDescriptor))).isEqualTo("int")
    }

    @Test
    override fun parameterizedSignature() {
        assertThat(signatureBuilder.signature((goat.getMethodInfo("parameterized")[0]
            .parameterInfo[0].typeDescriptor))).isEqualTo("java.util.List<java.lang.String>")
    }

    @Test
    override fun genericTypeVariable() {
        TODO("Not yet implemented")
    }

    @Test
    override fun genericVariableContravariant() {
        TODO("Not yet implemented")
    }

    @Test
    override fun traceySpecial() {
        TODO("Not yet implemented")
    }

    @Test
    override fun genericVariableMultipleBounds() {
        TODO("Not yet implemented")
    }

    @Test
    override fun genericTypeVariableUnbounded() {
        TODO("Not yet implemented")
    }
}
