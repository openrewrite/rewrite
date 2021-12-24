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
import io.github.classgraph.ClassTypeSignature
import io.github.classgraph.TypeParameter
import io.github.classgraph.TypeSignature
import org.openrewrite.java.JavaTypeSignatureBuilder
import org.openrewrite.java.JavaTypeSignatureBuilderTest

class ClassgraphJavaTypeSignatureBuilderTest : JavaTypeSignatureBuilderTest {
    companion object {
        private val goat = ClassGraph()
            .filterClasspathElements { e -> !e.endsWith(".jar") }
            .enableMemoryMapping()
            .enableClassInfo()
            .enableFieldInfo()
            .enableMethodInfo()
            .ignoreClassVisibility()
            .acceptClasses("org.openrewrite.java.*")
            .scan()
            .getClassInfo("org.openrewrite.java.JavaTypeGoat")
    }

    override fun fieldSignature(field: String): String = signatureBuilder()
        .variableSignature(goat.getFieldInfo(field))

    override fun methodSignature(methodName: String): String = signatureBuilder()
        .methodSignature(goat.getMethodInfo(methodName)[0])

    override fun firstMethodParameter(methodName: String): TypeSignature = goat.getMethodInfo(methodName)[0]
        .parameterInfo[0].run { typeSignature ?: typeDescriptor }

    override fun lastClassTypeParameter(): TypeParameter = (goat.typeSignature as ClassTypeSignature).typeParameters.last()

    override fun signatureBuilder(): ClassgraphJavaTypeSignatureBuilder = ClassgraphJavaTypeSignatureBuilder(emptyMap())
}
