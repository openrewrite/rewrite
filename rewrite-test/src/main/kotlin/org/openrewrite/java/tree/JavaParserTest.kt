/*
 * Copyright 2020 the original author or authors.
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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Parser
import org.openrewrite.TreeSerializer
import org.openrewrite.java.JavaParser
import java.nio.file.Paths
import java.util.Collections.singletonList

interface JavaParserTest {
    @Test
    fun relativeSourcePath(jp: JavaParser) {
        val projectDir = Paths.get("/Users/jon/Projects/github/Netflix/eureka")
        val sourcePath = Paths.get("/Users/jon/Projects/github/Netflix/eureka/eureka-client-archaius2/src/main/java/com/netflix/discovery/EurekaArchaius2ClientConfig.java")
        val cu = jp.parseInputs(listOf(Parser.Input(sourcePath) { "class Test {}".byteInputStream() }), projectDir, InMemoryExecutionContext())[0]

        assertThat(cu.sourcePath).isEqualTo(Paths.get("eureka-client-archaius2/src/main/java/com/netflix/discovery/EurekaArchaius2ClientConfig.java"))
        val serializer = TreeSerializer<J.CompilationUnit>()

        val cu2 = serializer.read(serializer.write(cu))
        assertThat(cu2.sourcePath).isEqualTo(Paths.get("eureka-client-archaius2/src/main/java/com/netflix/discovery/EurekaArchaius2ClientConfig.java"))
    }

    @Test
    fun dependsOn(jp: JavaParser.Builder<*, *>) {
        val cu = jp
            .dependsOn(singletonList(Parser.Input.fromString("class A {}")))
            .build()
            .parse(
                """
                    class Test {
                        A a;
                    }
                """.trimIndent()
            )

        assertThat(
            (cu[0].classes[0].body.statements[0] as J.VariableDeclarations).typeAsClass?.fullyQualifiedName
        ).isEqualTo("A")
    }
}
