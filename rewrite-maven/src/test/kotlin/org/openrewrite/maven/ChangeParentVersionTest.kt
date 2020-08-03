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
import org.openrewrite.whenParsedBy
import java.io.File
import java.nio.file.Path

class ChangeParentVersionTest {
    private val parser = MavenParser.builder()
            .resolveDependencies(false)
            .build()

    @Test
    fun fixedVersion(@TempDir tempDir: Path) {
        File(tempDir.toFile(), "pom.xml").apply {
            writeText("""
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>1.5.12.RELEASE</version>
                    <relativePath/> <!-- lookup parent from repository -->
                  </parent>
                </project>
            """.trimIndent().trim())
        }
                .toPath()
                .whenParsedBy(parser)
                .whenVisitedBy(ChangeParentVersion().apply {
                    setGroupId("org.springframework.boot")
                    setArtifactId("spring-boot-starter-parent")
                    setToVersion("2.3.1.RELEASE")
                })
                .isRefactoredTo("""
                    <project>
                      <modelVersion>4.0.0</modelVersion>
                      
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-app</artifactId>
                      <version>1</version>
                      
                      <parent>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-parent</artifactId>
                        <version>2.3.1.RELEASE</version>
                        <relativePath/> <!-- lookup parent from repository -->
                      </parent>
                    </project>
                """)
    }
}
