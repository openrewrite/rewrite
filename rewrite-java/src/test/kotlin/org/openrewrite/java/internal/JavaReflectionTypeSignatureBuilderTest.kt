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

import org.openrewrite.java.JavaTypeGoat
import org.openrewrite.java.JavaTypeSignatureBuilderTest
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable

class JavaReflectionTypeSignatureBuilderTest : JavaTypeSignatureBuilderTest {
    override fun methodSignature(methodName: String): String = signatureBuilder()
        .methodSignature(JavaTypeGoat::class.java.declaredMethods.first { it.name == methodName })

    override fun firstMethodParameter(methodName: String): Type = JavaTypeGoat::class.java.declaredMethods
        .first { it.name == methodName }
        .genericParameterTypes[0]

    override fun lastClassTypeParameter(): TypeVariable<Class<JavaTypeGoat<*, *>>> =
        JavaTypeGoat::class.java.typeParameters.last()

    override fun signatureBuilder() = JavaReflectionTypeSignatureBuilder()
}
