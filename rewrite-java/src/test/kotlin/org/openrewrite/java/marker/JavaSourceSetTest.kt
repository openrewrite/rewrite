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
import kotlin.math.ln
import kotlin.math.pow

class JavaSourceSetTest {

    @Test
    fun typesFromClasspath() {
        val ctx = InMemoryExecutionContext { e -> throw e }
        val javaSourceSet = JavaSourceSet.build("main", JavaParser.runtimeClasspath(), mutableMapOf(), ctx)

        println(humanReadableByteCount(GraphStats.parseInstance(javaSourceSet).totalSize().toDouble()))

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
