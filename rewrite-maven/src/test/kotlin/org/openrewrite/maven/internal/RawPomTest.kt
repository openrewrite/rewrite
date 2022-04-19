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
import org.openrewrite.maven.tree.ProfileActivation

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
    fun repositoriesSerializationAndDeserialization() {
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

        assertThat(pom.repositories).hasSize(1)
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

                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-surefire-plugin</artifactId>
                            <version>2.22.1</version>
                            <configuration>
                                <includes>
                                        <include>**/*Tests.java</include>
                                        <include>**/*Test.java</include>
                                </includes>
                                <excludes>
                                        <exclude>**/Abstract*.java</exclude>
                                </excludes>
                                <!-- see https://stackoverflow.com/questions/18107375/getting-skipping-jacoco-execution-due-to-missing-execution-data-file-upon-exec -->
                                <argLine>hello</argLine>
                            </configuration>
                        </plugin>
                        <plugin>
                            <groupId>org.jacoco</groupId>
                            <artifactId>jacoco-maven-plugin</artifactId>
                            <executions>
                                <execution>
                                    <id>agent</id>
                                    <goals>
                                        <goal>prepare-agent</goal>
                                    </goals>
                                </execution>
                                <execution>
                                        <id>report</id>
                                        <phase>test</phase>
                                        <goals>
                                            <goal>report</goal>
                                        </goals>
                                </execution>
                            </executions>
                        </plugin>
                    </plugins>
                    <pluginManagement>
                        <plugins>
                            <plugin>
                                <groupId>fred</groupId>
                                <artifactId>fred</artifactId>
                                <version>fred</version>
                                <extensions>true</extensions>
                                <inherited>true</inherited>
                                <configuration/>
                            </plugin>
                        </plugins>
                    </pluginManagement>
                </build>
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

        assertThat(model.dependencies?.get(0)?.groupId)
            .isEqualTo("org.junit.jupiter")

        assertThat(model.dependencies?.get(0)?.exclusions!!.first().groupId)
            .isEqualTo("com.google.guava")

        assertThat(model.dependencyManagement?.dependencies?.first()?.groupId)
            .isEqualTo("org.springframework.cloud")

        assertThat(model.build?.plugins).hasSize(2)
        assertThat(model.build!!.plugins?.get(1)?.executions).hasSize(2)

        assertThat(model.licenses?.first()?.name)
            .isEqualTo("Apache License, Version 2.0")

        assertThat(model.repositories?.first()?.url)
            .isEqualTo("https://oss.sonatype.org/content/repositories/snapshots")

        assertThat(model.profiles?.get(0)?.dependencies?.first()?.groupId).isEqualTo("javax.xml.bind")
        assertThat(model.profiles?.get(1)?.dependencies).isEmpty()
    }
}
