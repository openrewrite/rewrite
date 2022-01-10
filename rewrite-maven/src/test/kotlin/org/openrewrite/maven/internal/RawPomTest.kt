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
package org.openrewrite.maven.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.openrewrite.maven.tree.ProfileActivation
import java.nio.file.Path

class RawPomTest {
    @Test
    fun profileActivationByJdk() {
        assertThat(ProfileActivation(false, "11", null).isActive).isTrue()
        assertThat(ProfileActivation(false, "[,12)", null).isActive).isTrue()
        assertThat(ProfileActivation(false, "[,11]", null).isActive).isFalse()
    }

    @Test
    fun profileActivationByAbsenceOfProperty() {
        assertThat(ProfileActivation(false, null,
            ProfileActivation.Property("!inactive", null)).isActive).isTrue()
    }

    @Test
    fun repositoriesSerializationAndDeserialization(@TempDir tempDir: Path) {
        val pom = RawPom.parse(
            """
                <project>
                  `<modelVersion>4.0.0</modelVersion>
                 
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
            """.trimIndent().byteInputStream(), null)

        assertThat(pom.repositories?.repositories).hasSize(1)
    }

    @Test
    fun deserializePom() {
        val pom = """
            <project>
                <modelVersion>4.0.0</modelVersion>
            
                <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>2.4.0</version>
                </parent>
            
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <packaging>jar</packaging>
                
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
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter-api</artifactId>
                    <version>5.7.0</version>
                    <scope>test</scope>
                    <exclusions>
                        <exclusion>
                          <groupId>com.google.guava</groupId>
                          <artifactId>guava</artifactId>
                        </exclusion>
                    </exclusions>
                  </dependency>
                </dependencies>
                
                <licenses>
                  <license>
                    <name>Apache License, Version 2.0</name>
                    <url>https://www.apache.org/licenses/LICENSE-2.0.txt</url>
                    <distribution>repo</distribution>
                    <comments>A business-friendly OSS license</comments>
                  </license>
                </licenses>
                
                <repositories>
                  <repository>
                    <releases>
                      <enabled>false</enabled>
                      <updatePolicy>always</updatePolicy>
                      <checksumPolicy>warn</checksumPolicy>
                    </releases>
                    <snapshots>
                      <enabled>true</enabled>
                      <updatePolicy>never</updatePolicy>
                      <checksumPolicy>fail</checksumPolicy>
                    </snapshots>
                    <name>Nexus Snapshots</name>
                    <id>snapshots-repo</id>
                    <url>https://oss.sonatype.org/content/repositories/snapshots</url>
                    <layout>default</layout>
                  </repository>
                </repositories>
                
                <profiles>
                    <profile>
                        <id>java9+</id>
                        <activation>
                            <jdk>[9,)</jdk>
                        </activation>
                        <dependencies>
                            <dependency>
                                <groupId>javax.xml.bind</groupId>
                                <artifactId>jaxb-api</artifactId>
                            </dependency>
                        </dependencies>
                    </profile>
                    <profile>
                        <id>java11+</id>
                        <dependencies>
                        </dependencies>
                    </profile>
                </profiles>
            </project>
        """.trimIndent()

        val model = MavenXmlMapper.readMapper().readValue(pom, RawPom::class.java)

        assertThat(model.parent!!.groupId).isEqualTo("org.springframework.boot")

        assertThat(model.packaging).isEqualTo("jar")

        assertThat(model.dependencies!!.dependencies[0].groupId)
            .isEqualTo("org.junit.jupiter")

        assertThat(model.dependencies!!.dependencies[0].exclusions!!.first().groupId)
            .isEqualTo("com.google.guava")

        assertThat(model.dependencyManagement?.dependencies?.dependencies?.first()?.groupId)
            .isEqualTo("org.springframework.cloud")

        assertThat(model.licenses?.licenses?.first()?.name)
            .isEqualTo("Apache License, Version 2.0")

        assertThat(model.repositories?.repositories?.first()?.url)
            .isEqualTo("https://oss.sonatype.org/content/repositories/snapshots")

        assertThat(model.profiles!!.profiles[0].dependencies!!.dependencies.first().groupId).isEqualTo("javax.xml.bind")
        assertThat(model.profiles!!.profiles[1].dependencies!!.dependencies).isEmpty()
    }
}
