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
import io.micrometer.core.instrument.util.DoubleFormat
import org.junit.jupiter.api.Test
import org.openjdk.jol.info.GraphStats
import org.openrewrite.java.JavaTypeMappingTest
import org.openrewrite.java.asParameterized
import kotlin.math.ln
import kotlin.math.pow

class ClassgraphTypeMappingTest : JavaTypeMappingTest {
    companion object {
        private val typeMapping = ClassgraphTypeMapping(mutableMapOf(), mapOf())

        private val goat = typeMapping.type(
            ClassGraph()
                .filterClasspathElements { e -> e.contains("rewrite") }
                .enableAnnotationInfo()
                .enableMemoryMapping()
                .enableClassInfo()
                .enableFieldInfo()
                .enableMethodInfo()
                .ignoreClassVisibility()
                .acceptClasses("org.openrewrite.java.*")
                .scan()
                .getClassInfo("org.openrewrite.java.JavaTypeGoat")
        ).asParameterized()!!
    }

    override fun goatType() = goat

    @Test
    fun eclipsePersistenceRecursiveParameterizedTypeDefinition() {
        val type = typeMapping.type(
            ClassGraph()
                .enableAnnotationInfo()
                .enableMemoryMapping()
                .enableClassInfo()
                .enableFieldInfo()
                .enableMethodInfo()
                .ignoreClassVisibility()
                .scan()
                .getClassInfo("org.eclipse.persistence.core.queries.CoreAttributeItem")
        ).asParameterized()!!

        println("Heap size: ${humanReadableByteCount(GraphStats.parseInstance(type).totalSize().toDouble())}")

        // if not handled correctly, causes StackOverflowException in DefaultJavaTypeSignatureBuilder
        type.toString()
    }

    private fun humanReadableByteCount(bytes: Double): String {
        val unit = 1024
        return if (bytes >= unit.toDouble() && !bytes.isNaN()) {
            val exp = (ln(bytes) / ln(unit.toDouble())).toInt()
            val pre = "${"KMGTPE"[exp - 1]}i"
            "${DoubleFormat.decimalOrNan(bytes / unit.toDouble().pow(exp.toDouble()))} ${pre}B"
        } else {
            "${DoubleFormat.decimalOrNan(bytes)} B"
        }
    }
}
