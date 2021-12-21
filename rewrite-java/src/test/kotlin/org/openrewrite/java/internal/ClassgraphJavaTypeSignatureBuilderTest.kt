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

import io.github.classgraph.ClassGraph
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaTypeSignatureBuilderTest

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
        Assertions.assertThat(
            signatureBuilder.signature(
                (goat.getMethodInfo("array")[0]
                    .parameterInfo[0].typeDescriptor)
            )
        ).isEqualTo("java.lang.Integer[]")
    }

    @Test
    override fun classSignature() {
        Assertions.assertThat(
            signatureBuilder.signature(
                (goat.getMethodInfo("clazz")[0]
                    .parameterInfo[0].typeDescriptor)
            )
        ).isEqualTo("java.lang.Integer")
    }

    @Test
    override fun primitiveSignature() {
        Assertions.assertThat(
            signatureBuilder.signature(
                (goat.getMethodInfo("primitive")[0]
                    .parameterInfo[0].typeDescriptor)
            )
        ).isEqualTo("int")
    }

    @Test
    override fun parameterizedSignature() {
        Assertions.assertThat(
            signatureBuilder.signature(
                (goat.getMethodInfo("parameterized")[0]
                    .parameterInfo[0].typeSignature)
            )
        ).isEqualTo("java.util.List<java.lang.String>")
    }

    @Test
    override fun genericTypeVariable() {
        Assertions.assertThat(
            signatureBuilder.signature(
                (goat.getMethodInfo("generic")[0]
                    .parameterInfo[0].typeSignature)
            )
        ).isEqualTo("java.util.List<? extends java.lang.String>")
    }

    @Test
    override fun genericVariableContravariant() {
        Assertions.assertThat(
            signatureBuilder.signature(
                (goat.getMethodInfo("genericContravariant")[0]
                    .parameterInfo[0].typeSignature)
            )
        ).isEqualTo("java.util.List<? super java.lang.String>")
    }

    @Test
    override fun traceySpecial() {
        Assertions.assertThat(signatureBuilder.signature(goat.typeSignature))
            .isEqualTo("org.openrewrite.java.JavaTypeGoat<T extends org.openrewrite.java.JavaTypeGoat<? extends T> & java.util.List<?>>")
    }

    @Test
    override fun genericVariableMultipleBounds() {
        traceySpecial()
    }

    @Test
    override fun genericTypeVariableUnbounded() {
        Assertions.assertThat(
            signatureBuilder.signature(
                (goat.getMethodInfo("genericUnbounded")[0]
                    .parameterInfo[0].typeSignature)
            )
        ).isEqualTo("java.util.List<U>")
    }
}
