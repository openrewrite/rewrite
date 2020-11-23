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
package org.openrewrite.maven

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.openrewrite.RefactorVisitorTestForParser
import org.openrewrite.maven.tree.Maven
import java.io.File
import java.nio.file.Path

class AddDependencyTest : RefactorVisitorTestForParser<Maven> {
    override val parser: MavenParser = MavenParser.builder().build()

    private val addDependency = AddDependency().apply {
        setGroupId("org.springframework.boot")
        setArtifactId("spring-boot-starter-web")
        setVersion("1.5.22.RELEASE")
    }

    @Test
    fun addToExistingDependencies(@TempDir tempDir: Path) = assertRefactored(
            visitors = listOf(addDependency),
            before = File(tempDir.toFile(), "pom.xml").apply {
                writeText("""
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <dependencies>
                    <dependency>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-actuator</artifactId>
                      <version>1.5.22.RELEASE</version>
                    </dependency>
                  </dependencies>
                </project>
            """.trimIndent().trim())
            },
            after = """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <dependencies>
                    <dependency>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-actuator</artifactId>
                      <version>1.5.22.RELEASE</version>
                    </dependency>
                    <dependency>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-web</artifactId>
                      <version>1.5.22.RELEASE</version>
                    </dependency>
                  </dependencies>
                </project>
            """
    )

    @Test
    fun addWhenNoDependencies(@TempDir tempDir: Path) = assertRefactored(
            visitors = listOf(addDependency),
            before = File(tempDir.toFile(), "pom.xml").apply {
                writeText("""
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                </project>
            """.trimIndent().trim())
            },
            after = """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencies>
                    <dependency>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-web</artifactId>
                      <version>1.5.22.RELEASE</version>
                    </dependency>
                  </dependencies>
                </project>
            """
    )
}
