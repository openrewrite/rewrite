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
package org.openrewrite.java.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaTypeGoat
import org.openrewrite.java.JavaTypeSignatureBuilderTest

class JavaReflectionTypeSignatureBuilderTest : JavaTypeSignatureBuilderTest {
    private val signatureBuilder = JavaReflectionTypeSignatureBuilder()

    @Test
    override fun arraySignature() {
        assertThat(signatureBuilder.signature((methodNamed("array"))))
            .isEqualTo("org.openrewrite.java.C[]")
    }

    @Test
    override fun classSignature() {
        assertThat(signatureBuilder.signature((methodNamed("clazz"))))
            .isEqualTo("org.openrewrite.java.C")
    }

    @Test
    override fun primitiveSignature() {
        assertThat(signatureBuilder.signature((methodNamed("primitive"))))
            .isEqualTo("int")
    }

    @Test
    override fun parameterizedSignature() {
        assertThat(signatureBuilder.signature((methodNamed("parameterized"))))
            .isEqualTo("org.openrewrite.java.PT<org.openrewrite.java.C>")
    }

    @Test
    override fun genericTypeVariable() {
        assertThat(signatureBuilder.signature((methodNamed("generic"))))
            .isEqualTo("org.openrewrite.java.PT<? extends org.openrewrite.java.C>")
    }

    @Test
    override fun genericVariableContravariant() {
        assertThat(signatureBuilder.signature((methodNamed("genericContravariant"))))
            .isEqualTo("org.openrewrite.java.PT<? super org.openrewrite.java.C>")
    }

    @Test
    override fun traceySpecial() {
        assertThat(signatureBuilder.signature((JavaTypeGoat::class.java.typeParameters[0])))
            .isEqualTo("T extends org.openrewrite.java.JavaTypeGoat<? extends T> & org.openrewrite.java.C")
    }

    @Test
    override fun genericVariableMultipleBounds() {
        traceySpecial()
    }

    @Test
    override fun genericTypeVariableUnbounded() {
        assertThat(signatureBuilder.signature((methodNamed("genericUnbounded"))))
            .isEqualTo("org.openrewrite.java.PT<U>")
    }

    private fun methodNamed(name: String) = JavaTypeGoat::class.java.declaredMethods
        .first { it.name == name }
        .genericParameterTypes[0]
}
