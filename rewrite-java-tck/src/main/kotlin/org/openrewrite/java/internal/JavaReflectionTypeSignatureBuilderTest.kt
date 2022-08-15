/*
 * Copyright 2022 the original author or authors.
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

import org.openrewrite.java.JavaTypeGoat
import org.openrewrite.java.JavaTypeSignatureBuilderTest
import java.lang.reflect.Type

class JavaReflectionTypeSignatureBuilderTest : JavaTypeSignatureBuilderTest {
    override fun fieldSignature(field: String): String = signatureBuilder()
        .variableSignature(JavaTypeGoat::class.java.getDeclaredField(field))

    override fun methodSignature(methodName: String): String = signatureBuilder()
        .methodSignature(JavaTypeGoat::class.java.declaredMethods.first { it.name == methodName }, "org.openrewrite.java.JavaTypeGoat")

    override fun constructorSignature(): String = signatureBuilder()
        .methodSignature(JavaTypeGoat::class.java.declaredConstructors.first(), "org.openrewrite.java.JavaTypeGoat")

    override fun firstMethodParameter(methodName: String): Type = JavaTypeGoat::class.java.declaredMethods
        .first { it.name == methodName }
        .genericParameterTypes[0]

    override fun innerClassSignature(innerClassSimpleName: String): Type = JavaTypeGoat::class.java.declaredClasses
        .first { it.simpleName == innerClassSimpleName }

    override fun lastClassTypeParameter(): Any =
        JavaTypeGoat::class.java.typeParameters.last()

    override fun signatureBuilder() = JavaReflectionTypeSignatureBuilder()
}
