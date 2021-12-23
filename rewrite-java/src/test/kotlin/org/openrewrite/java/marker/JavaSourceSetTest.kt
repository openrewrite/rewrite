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
import kotlin.streams.toList

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

        javaSourceSet.classpath.forEach {
            object : JavaTypeVisitor<Int>() {
                override fun visit(javaType: JavaType?, p: Int): JavaType? {
                    if(javaType is JavaType) {
                        if(uniqueTypes.add(javaType)) {
                            return super.visit(javaType, p)
                        }
                    }
                    return null
                }
            }.visit(it, 0)
        }

        val arrWithInner = uniqueTypes.stream().filter { it.toString().contains("$") }.toList().filterIsInstance<JavaType.Array>().sortedBy { it.toString() }
        val classWithInner = uniqueTypes.stream().filter { it.toString().contains("$") }.toList().filterIsInstance<JavaType.Class>().sortedBy { it.toString() }
        val varWithInner = uniqueTypes.stream().filter { it.toString().contains("$") }.toList().filterIsInstance<JavaType.Variable>().sortedBy { it.toString() }
        val arrWithoutInner = uniqueTypes.stream().filter { !it.toString().contains("$") }.toList().filterIsInstance<JavaType.Array>().sortedBy { it.toString() }
        val classWithoutInner = uniqueTypes.stream().filter { !it.toString().contains("$") }.toList().filterIsInstance<JavaType.Class>().sortedBy { it.toString() }
        val varWithoutInner = uniqueTypes.stream().filter { !it.toString().contains("$") }.toList().filterIsInstance<JavaType.Variable>().sortedBy { it.toString() }

        println("Deep type count: ${uniqueTypes.size}")

        assertThat(javaSourceSet.classpath.map { it.fullyQualifiedName })
            .contains("org.junit.jupiter.api.Test")
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
