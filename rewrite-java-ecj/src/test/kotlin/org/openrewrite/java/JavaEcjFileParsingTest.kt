/*
 * Copyright 2023 the original author or authors.
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

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class JavaEcjFileParsingTest {
    @Test
    fun parseJavaSourceFromFile(@TempDir tempDir: Path) {
        val source = File(tempDir.toFile(), "A.java")
        source.writeText("""
            import java.io.Serializable;
            public class A implements Serializable {
                class B {
                }
            }
        """.trimIndent())

        JavaEcjParser.builder().build().parse(listOf(source.toPath()), tempDir)
    }
}
