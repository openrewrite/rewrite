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
package org.openrewrite.java.marker

import io.micrometer.core.instrument.util.DoubleFormat.decimalOrNan
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openjdk.jol.info.GraphStats
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaTypeVisitor
import org.openrewrite.java.tree.JavaType
import java.util.*
import kotlin.math.ln
import kotlin.math.pow

class JavaSourceSetTest {

    @Test
    fun typesFromClasspath() {
        val ctx = InMemoryExecutionContext { e -> throw e }
        val typeBySignature = mutableMapOf<String, Any>()
        val javaSourceSet = JavaSourceSet.build("main", JavaParser.runtimeClasspath(), typeBySignature, ctx)

        println("Heap size: ${humanReadableByteCount(GraphStats.parseInstance(javaSourceSet).totalSize().toDouble())}")
        println("Unique signatures: ${typeBySignature.size}")
        println("Shallow type count: ${javaSourceSet.classpath.size}")

        val uniqueTypes: MutableSet<JavaType> = Collections.newSetFromMap(IdentityHashMap())
        val typeBySignatureAfterMapping: MutableMap<String, JavaType> = mutableMapOf()
        var signatureCollisions = 0

        javaSourceSet.classpath.forEach {
            object : JavaTypeVisitor<Int>() {
                override fun visit(javaType: JavaType?, p: Int): JavaType? {
                    if (javaType is JavaType) {
                        if (uniqueTypes.add(javaType)) {
                            typeBySignatureAfterMapping.compute(javaType.toString()) { _, existing ->
                                if(existing != null && javaType !== existing) {
                                    println("multiple instances found for signature $javaType")
                                    signatureCollisions++
                                }
                                javaType
                            }
                            return super.visit(javaType, p)
                        }
                    }
                    return javaType
                }
            }.visit(it, 0)
        }

        println("Deep type count: ${uniqueTypes.size}")

        assertThat(javaSourceSet.classpath.map { it.fullyQualifiedName })
            .contains("org.junit.jupiter.api.Test")

        assertThat(signatureCollisions)
            .`as`("More than one instance of a type collides on the same signature. See the sysout above for details.")
            .isEqualTo(0)
    }

    private fun humanReadableByteCount(bytes: Double): String {
        val unit = 1024
        return if (bytes >= unit.toDouble() && !bytes.isNaN()) {
            val exp = (ln(bytes) / ln(unit.toDouble())).toInt()
            val pre = "${"KMGTPE"[exp - 1]}i"
            "${decimalOrNan(bytes / unit.toDouble().pow(exp.toDouble()))} ${pre}B"
        } else {
            "${decimalOrNan(bytes)} B"
        }
    }
}
