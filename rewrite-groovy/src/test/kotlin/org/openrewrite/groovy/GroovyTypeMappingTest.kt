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
package org.openrewrite.groovy

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.java.JavaTypeMappingTest
import org.openrewrite.java.JavaTypeVisitor
import org.openrewrite.java.asParameterized
import org.openrewrite.java.tree.JavaType
import java.util.*

@Disabled
class GroovyTypeMappingTest : JavaTypeMappingTest {
    companion object {
        private val goat = GroovyTypeMappingTest::class.java.getResourceAsStream("/GroovyTypeGoat.groovy")!!
            .bufferedReader().readText()
    }

    override fun goatType(): JavaType.Parameterized = GroovyParser.builder()
        .logCompilationWarningsAndErrors(true)
        .build()
        .parse(InMemoryExecutionContext { t -> fail(t) }, goat)[0]
        .classes[0]
        .type
        .asParameterized()!!

    @Test
    fun noDuplicateSignatures() {
        val cu = GroovyParser.builder().build().parse(
            InMemoryExecutionContext { e -> throw e },
            """
                def a = "hi"
            """
        )[0].statements[0]

        val uniqueTypes: MutableSet<JavaType> = Collections.newSetFromMap(IdentityHashMap())
        val typeBySignatureAfterMapping = mutableMapOf<String, JavaType>()
        val signatureCollisions = sortedMapOf<String, Int>()

        object : GroovyVisitor<Int>() {
            override fun visitType(javaType: JavaType?, p: Int): JavaType? {
                return object : JavaTypeVisitor<Int>() {
                    override fun visit(javaType: JavaType?, p: Int): JavaType? {
                        if (javaType is JavaType) {
                            if (uniqueTypes.add(javaType)) {
                                typeBySignatureAfterMapping.compute(javaType.toString()) { _, existing ->
                                    if (existing != null && javaType !== existing) {
                                        signatureCollisions.compute(javaType.toString()) { _, acc -> (acc ?: 0) + 1 }
                                    }
                                    javaType
                                }
                                return super.visit(javaType, p)
                            }
                        }
                        return javaType
                    }
                }.visit(javaType, 0)
            }
        }.visit(cu, 0)

        println("Unique signatures: ${typeBySignatureAfterMapping.size}")
        println("Deep type count: ${uniqueTypes.size}")
        println("Signature collisions: ${signatureCollisions.size}")

        assertThat(signatureCollisions.entries.map { "${it.key}${if (it.value > 1) " (x${it.value})" else ""}" })
            .`as`("More than one instance of a type collides on the same signature.")
            .isEmpty()
    }
}
