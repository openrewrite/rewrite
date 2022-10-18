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

import org.junit.jupiter.api.fail
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType
import java.util.concurrent.atomic.AtomicReference

class Java17TypeMappingTest : JavaParserTypeMappingTest {
    companion object {
        private val goat = Java17TypeMappingTest::class.java.getResourceAsStream("/JavaTypeGoat.java")!!
            .bufferedReader().readText()

        private val goatCu = JavaParserTypeMappingTest.parser
            .parse(InMemoryExecutionContext { t -> fail(t) }, goat)[0]
    }

    override fun classType(fqn: String): JavaType.FullyQualified {
        val type = AtomicReference<JavaType.FullyQualified>()
        object : JavaVisitor<Int>() {
            override fun visitClassDeclaration(classDecl: J.ClassDeclaration, p: Int): J {
                if (classDecl.type?.fullyQualifiedName == fqn) {
                    type.set(classDecl.type)
                }
                return super.visitClassDeclaration(classDecl, p)
            }
        }.visitNonNull(goatCu, 0)
        return type.get()!!
    }
}
