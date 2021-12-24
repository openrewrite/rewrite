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
package org.openrewrite.java.tree

import org.openrewrite.internal.StringUtils
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaTypeSignatureBuilderTest
import org.openrewrite.java.asParameterized
import org.openrewrite.java.internal.DefaultJavaTypeSignatureBuilder

class DefaultJavaTypeSignatureBuilderTest : JavaTypeSignatureBuilderTest {
    companion object {
        private val goat = StringUtils.readFully(
            DefaultJavaTypeSignatureBuilderTest::class.java.getResourceAsStream("/JavaTypeGoat.java")
        )

        private val goatCu = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .build()
            .parse(goat)[0]
            .classes[0]
            .type
            .asParameterized()!!
    }

    override fun fieldSignature(field: String): String = signatureBuilder().variableSignature(goatCu.type.members
        .first { it.name == field })

    override fun methodSignature(methodName: String): String = signatureBuilder().methodSignature(goatCu.type.methods
        .first { it.name == methodName })

    override fun firstMethodParameter(methodName: String): JavaType = goatCu.type.methods
        .first { it.name == methodName }
        .resolvedSignature
        .paramTypes[0]

    override fun lastClassTypeParameter(): JavaType = goatCu.typeParameters.last()

    override fun signatureBuilder(): DefaultJavaTypeSignatureBuilder = DefaultJavaTypeSignatureBuilder()
}
