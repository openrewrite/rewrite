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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIf
import org.junit.jupiter.api.io.TempDir
import org.openrewrite.Issue
import java.io.File
import java.nio.file.Path

class MavenParserTest {
    @Test
    fun milestoneParent() {
        val pom = MavenParser.builder().build()
            .parse("""
                <project>
                  <modelVersion>4.0.0</modelVersion>
                 
                  <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>2.4.0-M3</version>
                  </parent>

                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <repositories>
                    <repository>
                        <id>spring-milestones</id>
                        <name>Spring Milestones</name>
                        <url>http://repo.spring.io/milestone</url>
                    </repository>
                  </repositories>
                </project>
            """.trimIndent())[0]

        assertThat(pom!!.model!!.parent!!.licenses).isNotNull()
    }

    @Test
    fun milestoneDependencyManagement() {
        val pom = MavenParser.builder().build()
                .parse("""
                <project>
                  <modelVersion>4.0.0</modelVersion>

                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.springframework.cloud</groupId>
                                <artifactId>spring-cloud-dependencies</artifactId>
                                <version>Greenwich.RC2</version>
                                <type>pom</type>
                                <scope>import</scope>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                  
                  <dependencies>
                      <dependency>
                        <groupId>org.springframework.cloud</groupId>
                        <artifactId>spring-cloud-commons</artifactId>
                      </dependency>
                  </dependencies>
                  
                  <repositories>
                    <repository>
                        <id>spring-milestones</id>
                        <name>Spring Milestones</name>
                        <url>http://repo.spring.io/milestone</url>
                    </repository>
                  </repositories>
                </project>
            """.trimIndent())[0]

        assertThat(pom!!.model!!.dependencies[0].moduleVersion.version).isNotBlank()
    }

    /**
     * https://maven.apache.org/guides/introduction/introduction-to-the-pom.html#minimal-pom
     */
    @Test
    fun minimal(@TempDir tempDir: Path) {
        val pomFile = File(tempDir.toFile(), "pom.xml").apply {
            writeText("""
                <project>
                  <modelVersion>4.0.0</modelVersion>
                 
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                </project>
            """.trimIndent().trim())
        }

        val pom = MavenParser.builder().build()
                .parse(pomFile.toPath(), tempDir)

        assertThat(pom.groupId).isEqualTo("com.mycompany.app")
        assertThat(pom.artifactId).isEqualTo("my-app")
        assertThat(pom.version).isEqualTo("1")
    }

    @Test
    fun resolveProperties(@TempDir tempDir: Path) {
        val pomFile = File(tempDir.toFile(), "pom.xml").apply {
            writeText("""
                <project>
                  <modelVersion>4.0.0</modelVersion>
                   
                  <properties>
                    <guava.version>29.0-jre</guava.version>
                  </properties>
                  
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>${"$"}{guava.version}</version>
                      <scope>test</scope>
                    </dependency>
                  </dependencies>
                </project>
            """.trimIndent().trim())
        }

        val pom = MavenParser.builder()
                .resolveDependencies(false)
                .build()
                .parse(pomFile.toPath(), tempDir)

        assertThat(pom.dependencies).hasSize(1)
        assertThat(pom.dependencies[0].version).isEqualTo("${"$"}{guava.version}")
        assertThat(pom.model.valueOf(pom.dependencies[0].version)).isEqualTo("29.0-jre")
    }

    /**
     * https://maven.apache.org/guides/introduction/introduction-to-the-pom.html#project-inheritance
     */
    @Test
    fun projectInheritance(@TempDir tempDir: Path) {
        val parentPomFile = File(tempDir.toFile(), "pom.xml").apply {
            writeText("""
                <project>
                  <modelVersion>4.0.0</modelVersion>
                 
                  <packaging>pom</packaging>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                </project>
            """.trimIndent().trim())
        }

        val myModuleProject = File(tempDir.toFile(), "my-module")
        myModuleProject.mkdirs()

        val pomFile = File(myModuleProject, "pom.xml").apply {
            writeText("""
                <project>
                  <modelVersion>4.0.0</modelVersion>
                 
                  <parent>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </parent>
                
                  <artifactId>my-module</artifactId>
                
                  <dependencies>
                    <dependency>
                      <groupId>org.junit.jupiter</groupId>
                      <artifactId>junit-jupiter-api</artifactId>
                      <version>5.6.2</version>
                      <scope>test</scope>
                    </dependency>
                  </dependencies>
                </project>
            """.trimIndent())
        }

        val (pom, parentPom) = MavenParser.builder()
                .resolveDependencies(false)
                .build()
                .parse(listOf(pomFile.toPath(), parentPomFile.toPath()), tempDir)

        assertThat(pom.model.moduleVersion.groupId).isEqualTo("com.mycompany.app")
        assertThat(pom.artifactId).isEqualTo("my-module")
        assertThat(pom.model.moduleVersion.version).isEqualTo("1")

        assertThat(parentPom.model.inheriting.firstOrNull()?.moduleVersion)
                .isEqualTo(pom.model.moduleVersion)
    }

    @Issue("23")
    @Test
    fun httpRepository(@TempDir tempDir: Path) {
        val pom = MavenParser.builder()
                .remoteRepositories(emptyList())
                .localRepository(tempDir.toFile())
                .build()
                .parse("""
                   <project>
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>my-group</groupId>
                      <artifactId>my-module</artifactId>
                      <version>0.1.0</version>
                      
                      <repositories>
                        <repository>
                          <id>jcenter</id>
                          <name>jcenter without https</name>
                          <url>http://jcenter.bintray.com</url>
                        </repository>
                      </repositories>
                    
                      <dependencies>
                        <dependency>
                          <groupId>org.junit.jupiter</groupId>
                          <artifactId>junit-jupiter-api</artifactId>
                          <version>[5.6,)</version>
                          <scope>test</scope>
                        </dependency>
                      </dependencies>
                    </project> 
                """)[0]

        assertThat(pom.model.dependencies[0].moduleVersion.version).doesNotContain("[")
    }
}
