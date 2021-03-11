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

import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.Credentials
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Issue
import org.openrewrite.Parser
import org.openrewrite.maven.tree.Maven
import org.openrewrite.maven.tree.Pom
import org.openrewrite.maven.tree.Scope
import java.nio.file.Paths

class MavenParserTest {
    private val parser = MavenParser.builder().resolveOptional(false).build()
    private val ctx = InMemoryExecutionContext { t -> throw t }

    @Test
    fun parse() {
        val parser = MavenParser.builder().build()

        val maven = parser.parse("""
            <project>
                <modelVersion>4.0.0</modelVersion>
            
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <packaging>pom</packaging>
                <developers>
                    <developer>
                        <name>Trygve Laugst&oslash;l</name>
                    </developer>
                </developers>

                <dependencies>
                  <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter-api</artifactId>
                    <version>5.7.0</version>
                    <type>pom</type>
                    <scope>test</scope>
                  </dependency>
                </dependencies>
            </project>
        """)[0]

        assertThat(maven.model.dependencies.first().model.licenses.first()?.type)
                .isEqualTo(Pom.LicenseType.Eclipse)
        assertThat(maven.model.dependencies.first().type)
                .isEqualTo("pom")
        assertThat(maven.model.packaging)
                .isEqualTo("pom")
    }

    @Test
    fun emptyArtifactPolicy() {
        // example from https://repo1.maven.org/maven2/org/openid4java/openid4java-parent/0.9.6/openid4java-parent-0.9.6.pom
        MavenParser.builder().build().parse("""
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <repositories>
                    <repository>
                        <id>alchim.snapshots</id>
                        <name>Achim Repository Snapshots</name>
                        <url>http://alchim.sf.net/download/snapshots</url>
                        <snapshots/>
                    </repository>
                </repositories>
            </project>
        """.trimIndent())
    }

    @Test
    fun handlesRepositories() {
        MavenParser.builder().build().parse("""
            <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>org.openrewrite.maven</groupId>
                <artifactId>single-project</artifactId>
                <version>0.1.0-SNAPSHOT</version>

                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>29.0-jre</version>
                    </dependency>
                </dependencies>

                <repositories>
                    <repository>
                        <id>jcenter</id>
                        <name>JCenter</name>
                        <url>https://jcenter.bintray.com/</url>
                    </repository>
                </repositories>
            </project>
        """)
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/198")
    @Test
    fun handlesPropertiesInDependencyScope() {
        val maven = MavenParser.builder().build().parse("""
            <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>org.openrewrite.maven</groupId>
                <artifactId>single-project</artifactId>
                <version>0.1.0-SNAPSHOT</version>

                <properties>
                    <dependency.scope>compile</dependency.scope>
                </properties>

                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>29.0-jre</version>
                        <scope>${"$"}{dependency.scope}</scope>
                    </dependency>
                </dependencies>
            </project>

        """).first()
        assertThat(maven.model.dependencies).hasSize(1)
        assertThat(maven.model.dependencies.first()).matches { it.scope == Scope.Compile }
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/199")
    @Test
    fun continueOnErrorInvalidScope() {
        val invalidPom = """
            <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>org.openrewrite.maven</groupId>
                <artifactId>single-project</artifactId>
                <version>0.1.0-SNAPSHOT</version>

                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>29.0-jre</version>
                        <scope>${"$"}{dependency.scope}</scope>
                    </dependency>
                </dependencies>
            </project>
        """
        assertThatThrownBy { parser.parse(ctx, invalidPom) }
        parser.parse(invalidPom)
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/199")
    @Test
    fun continueOnErrorMissingGroupId() {
        val invalidPom = """
            <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>org.openrewrite.maven</groupId>
                <artifactId>single-project</artifactId>
                <version>0.1.0-SNAPSHOT</version>

                <dependencies>
                    <dependency>
                        <artifactId>guava</artifactId>
                        <version>29.0-jre</version>
                    </dependency>
                </dependencies>
            </project>
        """
        assertThatThrownBy { parser.parse(ctx, invalidPom) }
        parser.parse(invalidPom)
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/199")
    @Test
    fun continueOnErrorMissingVersion() {
        val invalidPom = """
            <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>org.openrewrite.maven</groupId>
                <artifactId>single-project</artifactId>
                <version>0.1.0-SNAPSHOT</version>

                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                    </dependency>
                </dependencies>
            </project>
        """
        assertThatThrownBy { parser.parse(ctx, invalidPom) }
        parser.parse(invalidPom)
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/199")
    @Test
    fun continueOnErrorMalformedExclusion() {
        val invalidPom = """
            <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                    <dependency>
                            <groupId>io.github.resilience4j</groupId>
                            <artifactId>resilience4j-retry</artifactId>
                            <version>1.7.0</version>
                            <exclusions>
                                <exclusion>
                                    <groupId>${"$"}{missing.property}</groupId>
                                </exclusion>
                            </exclusions> 
                    </dependency>
                </dependencies>
            </project>
        """
        assertThatThrownBy { parser.parse(ctx, invalidPom) }
        parser.parse(invalidPom)
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/135")
    @Test
    fun selfRecursiveParent() {
        MavenParser.builder()
                .resolveOptional(false)
                .build()
                .parse("""
                <project>
                    <modelVersion>4.0.0</modelVersion>
                
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    
                    <parent>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                    </parent>
                </project>
            """)
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/135")
    @Test
    fun selfRecursiveDependency() {
        val maven = MavenParser.builder()
                .resolveOptional(false)
                .build()
                .parse("""
                <project>
                    <modelVersion>4.0.0</modelVersion>

                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    
                    <dependencies>
                        <dependency>
                            <groupId>com.mycompany.app</groupId>
                            <artifactId>my-app</artifactId>
                            <version>1</version>
                        </dependency>
                    </dependencies>
                </project>
            """)
                .first()

        // Maven itself would respond to this pom with a fatal error.
        // So long as we don't produce an AST with cycles it's OK
        assertThat(maven.model.dependencies).hasSize(1)
        assertThat(maven.model.dependencies.first().model.dependencies).hasSize(0)
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/323")
    @Test
    fun inheritScopeFromDependencyManagement() {
        val pomSource = """<?xml version="1.0" encoding="UTF-8"?>
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencyManagement>
                    <dependencies>
                       <dependency>
                            <groupId>org.junit.jupiter</groupId>
                            <artifactId>junit-jupiter</artifactId>
                            <version>5.7.1</version>
                            <scope>test</scope>
                        </dependency>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>29.0-jre</version>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
                <dependencies>
                    <dependency>
                        <groupId>org.junit.jupiter</groupId>
                        <artifactId>junit-jupiter</artifactId>
                    </dependency>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                    </dependency>
                </dependencies>
            </project>
        """

        val maven = MavenParser.builder().build().parse(pomSource)[0]
        assertThat(maven.model.dependencies.map { it.artifactId to it.scope })
                .containsExactly("junit-jupiter" to Scope.Test, "guava" to Scope.Compile)
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/323")
    @Test
    fun dependencyScopeTakesPrecedenceOverDependencyManagementScope() {
        val pomSource = """<?xml version="1.0" encoding="UTF-8"?>
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencyManagement>
                    <dependencies>
                       <dependency>
                            <groupId>org.junit.jupiter</groupId>
                            <artifactId>junit-jupiter</artifactId>
                            <version>5.7.1</version>
                            <scope>test</scope>
                        </dependency>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>29.0-jre</version>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
                <dependencies>
                    <dependency>
                        <groupId>org.junit.jupiter</groupId>
                        <artifactId>junit-jupiter</artifactId>
                        <scope>compile</scope>
                    </dependency>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                    </dependency>
                </dependencies>
            </project>
        """

        val maven = MavenParser.builder().build().parse(pomSource)[0]
        assertThat(maven.model.dependencies.map { it.artifactId to it.scope })
                .containsExactly("junit-jupiter" to Scope.Compile, "guava" to Scope.Compile)
    }

    @Test
    fun mirrorsAndAuth() {
        // Set up a web server that returns 401 to any request without an Authorization header corresponding to specific credentials
        // Exceptions in the console output are due to MavenPomDownloader attempting to access via https first before falling back to http
        val username = "admin"
        val password = "password"
        MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    if (request.headers.find { it.first == "Authorization" && it.second == Credentials.basic(username, password) } == null) {
                        return MockResponse().apply {
                            setResponseCode(401)
                        }
                    } else {
                        return MockResponse().apply {
                            setResponseCode(200)
                            setBody("""
                                <project>
                                  <modelVersion>4.0.0</modelVersion>

                                  <groupId>com.foo</groupId>
                                  <artifactId>bar</artifactId>
                                  <version>1.0.0</version>
                                  
                                </project>
                            """.trimIndent())
                        }
                    }
                }
            }
        }.use { mockRepo ->
            val ctx = InMemoryExecutionContext { err -> throw err }
            MavenSettings.parse(Parser.Input(Paths.get("settings.xml")) {
                """
                <settings>
                    <mirrors>
                        <mirror>
                            <mirrorOf>*</mirrorOf>
                            <name>repo</name>
                            <url>http://${mockRepo.hostName}:${mockRepo.port}</url>
                            <id>repo</id>
                        </mirror>
                    </mirrors>
                    <servers>
                        <server>
                            <id>repo</id>
                            <username>${username}</username>
                            <password>${password}</password>
                        </server>
                    </servers>
                </settings>
                """.trimIndent().byteInputStream()
            }, ctx)

            val maven: Maven = MavenParser.builder().build().parse(
                    ctx,
                    """
                        <project>
                            <modelVersion>4.0.0</modelVersion>
                        
                            <groupId>org.openrewrite.test</groupId>
                            <artifactId>foo</artifactId>
                            <version>0.1.0-SNAPSHOT</version>
                        
                            <dependencies>
                                <dependency>
                                    <groupId>com.foo</groupId>
                                    <artifactId>bar</artifactId>
                                    <version>1.0.0</version>
                                </dependency>
                            </dependencies>
                        </project>
                    """
            ).first()

            assertThat(mockRepo.requestCount)
                    .isGreaterThan(0)
                    .`as`("The mock repository received no requests. Applying mirrors is probably broken")

            assertThat(maven.model.dependencies)
                    .hasSize(1)
                    .matches { it.first().groupId == "com.foo" && it.first().artifactId == "bar" }
        }
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/95")
    @Test
    fun recursivePropertyFromParentPoms() {
        // a depends on d. The version number is a property specified in a's parent, b
        // b gets part of the version number for d from a property specified in b's parent, c
        val maven = MavenParser.builder()
                .build()
                .parse("""
                    <project>
                        <modelVersion>4.0.0</modelVersion>
                    
                        <parent>
                            <groupId>org.openrewrite.maven</groupId>
                            <artifactId>b</artifactId>
                            <version>0.1.0-SNAPSHOT</version>
                            <relativePath />
                        </parent>
                    
                        <artifactId>a</artifactId>
                    
                        <dependencies>
                            <dependency>
                                <groupId>org.openrewrite.maven</groupId>
                                <artifactId>d</artifactId>
                                <version>${"$"}{d.version}</version>
                            </dependency>
                        </dependencies>
                    </project>
                """,
                """
                    <project>
                        <modelVersion>4.0.0</modelVersion>
                    
                        <parent>
                            <groupId>org.openrewrite.maven</groupId>
                            <artifactId>c</artifactId>
                            <version>0.1.0-SNAPSHOT</version>
                            <relativePath />
                        </parent>
                    
                        <artifactId>b</artifactId>
                        <packaging>pom</packaging>
                    
                        <properties>
                            <maven.compiler.source>1.8</maven.compiler.source>
                            <maven.compiler.target>1.8</maven.compiler.target>
                            <d.version>0.1.0${"$"}{d.version.snapshot}</d.version>
                        </properties>
                    </project>
                """,
                """
                    <project>
                        <modelVersion>4.0.0</modelVersion>
                    
                        <artifactId>c</artifactId>
                        <groupId>org.openrewrite.maven</groupId>
                        <version>0.1.0-SNAPSHOT</version>
                        <packaging>pom</packaging>
                    
                        <properties>
                            <d.version.snapshot>-SNAPSHOT</d.version.snapshot>
                        </properties>
                    </project>

                """,
                """
                    <project>
                        <modelVersion>4.0.0</modelVersion>
                    
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>d</artifactId>
                        <version>0.1.0-SNAPSHOT</version>
                    
                        <properties>
                            <maven.compiler.source>1.8</maven.compiler.source>
                            <maven.compiler.target>1.8</maven.compiler.target>
                        </properties>
                    
                    </project>
                """
                )
                .find { it.model.artifactId == "a" }!!

        assertThat(maven.model.dependencies.first().version).isEqualTo("0.1.0-SNAPSHOT")
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/124")
    @Test
    fun indirectBomImportedFromParent() {
        // a depends on d without specifying version number. a's parent is b
        // b imports c into its dependencyManagement section
        // c's dependencyManagement specifies the version of d to use
        // So if all goes well a will have the version of d from c's dependencyManagement
        val maven = MavenParser.builder()
                .build()
                .parse("""
                    <project>
                        <modelVersion>4.0.0</modelVersion>
                    
                        <parent>
                            <groupId>org.openrewrite.maven</groupId>
                            <artifactId>b</artifactId>
                            <version>0.1.0-SNAPSHOT</version>
                            <relativePath />
                        </parent>
                    
                        <artifactId>a</artifactId>
                    
                        <properties>
                            <maven.compiler.source>1.8</maven.compiler.source>
                            <maven.compiler.target>1.8</maven.compiler.target>
                        </properties>
                        
                        <dependencies>
                            <dependency>
                                <groupId>org.openrewrite.maven</groupId>
                                <artifactId>d</artifactId>
                            </dependency>
                        </dependencies>
                    </project>
                """,
                        """
                    <project>
                        <modelVersion>4.0.0</modelVersion>
                    
                        <artifactId>b</artifactId>
                        <groupId>org.openrewrite.maven</groupId>
                        <version>0.1.0-SNAPSHOT</version>
                        <packaging>pom</packaging>
                    
                        <properties>
                            <maven.compiler.source>1.8</maven.compiler.source>
                            <maven.compiler.target>1.8</maven.compiler.target>
                        </properties>
                    
                        <dependencyManagement>
                            <dependencies>
                                <dependency>
                                    <groupId>org.openrewrite.maven</groupId>
                                    <artifactId>c</artifactId>
                                    <version>0.1.0-SNAPSHOT</version>
                                    <type>pom</type>
                                    <scope>import</scope>
                                </dependency>
                            </dependencies>
                        </dependencyManagement>
                    </project>
                     
                """,
                        """
                    <project>
                        <modelVersion>4.0.0</modelVersion>
                    
                        <artifactId>c</artifactId>
                        <groupId>org.openrewrite.maven</groupId>
                        <version>0.1.0-SNAPSHOT</version>
                        <packaging>pom</packaging>
                    
                        <dependencyManagement>
                            <dependencies>
                                <dependency>
                                    <groupId>org.openrewrite.maven</groupId>
                                    <artifactId>d</artifactId>
                                    <version>0.1.0-SNAPSHOT</version>
                                </dependency>
                            </dependencies>
                        </dependencyManagement>
                    </project>
                """,
                        """
                    <project>
                        <modelVersion>4.0.0</modelVersion>
                    
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>d</artifactId>
                        <version>0.1.0-SNAPSHOT</version>
                    
                        <properties>
                            <maven.compiler.source>1.8</maven.compiler.source>
                            <maven.compiler.target>1.8</maven.compiler.target>
                        </properties>
                    </project>
                """
                )
                .find { it.model.artifactId == "a" }!!

        assertThat(maven.model.dependencies.first().version).isEqualTo("0.1.0-SNAPSHOT")
    }

    @Test
    fun parentPomProfileProperty() {
        val maven = MavenParser.builder()
                .resolveOptional(false)
                .build()
                .parse("""
                <project>
                    <modelVersion>4.0.0</modelVersion>

                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>multi-module-project-parent</artifactId>
                    <version>0.1.0-SNAPSHOT</version>
                    <packaging>pom</packaging>

                    <modules>
                        <module>a</module>
                    </modules>

                    <profiles>
                        <profile>
                          <id>appserverConfig-dev-2</id>
                          <activation>
                            <activeByDefault>true</activeByDefault>
                          </activation>
                          <properties>
                            <guava.version>29.0-jre</guava.version>
                          </properties>
                        </profile>
                    </profiles>
                </project>
            """,
                        """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                
                    <parent>
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>multi-module-project-parent</artifactId>
                        <version>0.1.0-SNAPSHOT</version>
                    </parent>
                
                    <artifactId>a</artifactId>
                
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>${"$"}{guava.version}</version>
                        </dependency>
                    </dependencies>
                </project>
            """)
                .find { it.model.artifactId == "a" }

        assertThat(maven!!.model.dependencies)
                .hasSize(1)
                .matches { it.first().artifactId == "guava" && it.first().version == "29.0-jre" }
    }
}
