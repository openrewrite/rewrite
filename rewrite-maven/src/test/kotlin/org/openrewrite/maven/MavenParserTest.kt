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
import org.junit.jupiter.api.io.TempDir
import org.openrewrite.Issue
import org.openrewrite.Parser
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class MavenParserTest {

    /**
     * This tests resolving dependencies from a password-protected repository with credentials provided by settings.xml.
     * We don't have a password-protected repository up and running all the time, so a suitably configured repository must be manually prepared.
     * In the future this may be automated, but for now these steps can be used to prepare such an environment:
     *
     * 1. Install docker
     * 2. docker pull docker.bintray.io/jfrog/artifactory-oss:latest
     * 3. docker run -d -p 8081:8081 -p 8082:8082 docker.bintray.io/jfrog/artifactory-oss:latest
     * 4. Browse to http://localhost:8081/ and go through setup. The initial username/password is admin/password
     * 5. When prompted, change the password for admin to E0Sl0n85N0DK
     * 6. Create a virtual repository that mirrors jcenter and requires authentication
     * 7. Remove the @Disabled annotation from this test and run it
     */
    @Disabled("This test requires that a properly configured repository be running locally, and that process is not automated")
    @Test
    fun repositoryAccessedWithCredentialsFromSettings() {
        val settingsXml = """
            <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                <servers>
                    <server>
                        <id>jcenter-auth</id>
                        <username>admin</username>
                        <password>E0Sl0n85N0DK</password>
                    </server>
                </servers>
                <activeProfiles>
                    <activeProfile>
                        repo
                    </activeProfile>
                </activeProfiles>
                <profiles>
                    <profile>
                        <id>repo</id>
                        <repositories>
                            <repository>
                                <id>jcenter-auth</id>
                                <name>JCenter Authenticated</name>
                                <url>http://localhost:8082/artifactory/jcenter-authenticated/</url>
                            </repository>
                        </repositories>
                    </profile>
                </profiles>
            </settings>
        """.trimIndent()

        val pom = MavenParser.builder()
                .noMavenCentral()
                .userSettingsXml(Parser.Input(Paths.get("settings.xml"),
                        { ByteArrayInputStream(settingsXml.toByteArray()) },
                        true))
                .build()
                .parse("""
                    <project>
                        <modelVersion>4.0.0</modelVersion>
                    
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                    
                        <dependencies>
                          <dependency>
                            <groupId>org.junit.jupiter</groupId>
                            <artifactId>junit-jupiter-api</artifactId>
                            <version>5.7.0</version>
                            <scope>test</scope>
                          </dependency>
                          <dependency>
                            <groupId>org.junit.jupiter</groupId>
                            <artifactId>junit-jupiter-engine</artifactId>
                            <version>5.7.0</version>
                            <scope>test</scope>
                          </dependency>
                        </dependencies>
                    </project>
                """.trimIndent())
                .first()
        // If transitive dependencies were found then dependency resolution successfully used the credentials from settings.xml
        assertThat(pom.model.transitiveDependenciesByScope["test"]!!.size).isGreaterThan(0)
    }

    @Test
    fun repositoryDefinedInSettings() {
        val settingsXml = """
            <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                <activeProfiles>
                    <activeProfile>
                        repo
                    </activeProfile>
                </activeProfiles>
                <profiles>
                    <profile>
                        <id>repo</id>
                        <repositories>
                            <repository>
                                <id>spring-milestones</id>
                                <name>Spring Milestones</name>
                                <url>https://repo.spring.io/milestone</url>
                            </repository>
                        </repositories>
                    </profile>
                </profiles>
            </settings>
        """.trimIndent()

        val pom = MavenParser.builder()
                .userSettingsXml(Parser.Input(Paths.get("settings.xml"),
                        { ByteArrayInputStream(settingsXml.toByteArray()) },
                        true))
                .build()
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
                    </project>
                """.trimIndent())
                .first()

        assertThat(pom!!.model!!.parent!!.licenses).isNotNull
    }

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
            """.trimIndent())
                .first()

        assertThat(pom!!.model!!.parent!!.licenses).isNotNull
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
                                <version>Greenwich.SR6</version>
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
