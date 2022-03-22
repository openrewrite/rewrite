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
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Issue
import org.openrewrite.Parser
import org.openrewrite.maven.cache.InMemoryMavenPomCache
import org.openrewrite.maven.cache.MavenPomCache
import org.openrewrite.maven.tree.License
import org.openrewrite.maven.tree.Scope
import java.nio.file.Paths

class MavenParserTest {
    private lateinit var cache: MavenPomCache
    private lateinit var ctx: MavenExecutionContextView

    private val parser = MavenParser.builder().build()

    @BeforeEach
    fun before() {
        cache = InMemoryMavenPomCache()
        ctx = MavenExecutionContextView(InMemoryExecutionContext { t -> throw t })
            .apply { pomCache = cache }
    }

    @Test
    fun rangeVersion() {
        parser.parse(ctx,
            """
                <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                
                  <dependencies>
                    <dependency>
                      <groupId>junit</groupId>
                      <artifactId>junit</artifactId>
                      <version>[4.11]</version>
                    </dependency>
                  </dependencies>
                </project>
            """
        )
    }

    @Test
    fun transitiveDependencyVersionDeterminedByBom() {
        parse("org.neo4j:neo4j-ogm-core:3.2.21")
    }

    @Test
    fun guava25() {
        parse("com.google.guava:guava:25.0-android")
    }

    @Test
    fun rewriteCircleci() {
        val rr = parse("org.openrewrite.recipe:rewrite-circleci:1.1.0")
        assertThat(rr.dependencies[Scope.Runtime]!!.map { it.artifactId }).contains("rewrite-yaml")
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1085")
    @Test
    fun parseDependencyManagementWithNoVersion() {
        val pomXml = parser.parse(
            ctx,
            """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <dependencyManagement>
                          <dependencies>
                              <dependency>
                                  <groupId>com.google.guava</groupId>
                                  <artifactId>guava</artifactId>
                                  <exclusions>
                                      <exclusion>
                                          <groupId>org.springframework</groupId>
                                          <artifactId>spring-core</artifactId>
                                      </exclusion>
                                  </exclusions>
                              </dependency>
                          </dependencies>
                    </dependencyManagement>
                    <dependencies>
                        <dependency>
                          <groupId>com.google.guava</groupId>
                          <artifactId>guava</artifactId>
                          <version>14.0</version>
                        </dependency>
                    </dependencies>
                </project>
            """
        )[0]
        assertThat(pomXml.mavenResolutionResult().findDependencies("com.google.guava", "guava", null)[0].version).isEqualTo("14.0")
    }

    @Test
    fun parseMergeExclusions() {
        val pomXml = parser.parse(
            ctx,
            """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-dep</artifactId>
                    <version>1</version>
                    <dependencies>
                        <dependency>
                          <groupId>com.google.guava</groupId>
                          <artifactId>guava</artifactId>
                          <version>14.0</version>
                        </dependency>
                        <dependency>
                          <groupId>org.slf4j</groupId>
                          <artifactId>slf4j-api</artifactId>
                          <version>1.7.20</version>
                        </dependency>
                    </dependencies>
                </project>
            """,
            """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <dependencyManagement>
                          <dependencies>
                              <dependency>
                                  <groupId>com.mycompany.app</groupId>
                                  <artifactId>my-dep</artifactId>
                                  <exclusions>
                                      <exclusion>
                                          <groupId>com.google.guava</groupId>
                                          <artifactId>guava</artifactId>
                                      </exclusion>
                                  </exclusions>
                              </dependency>
                          </dependencies>
                    </dependencyManagement>
                    <dependencies>
                        <dependency>
                          <groupId>com.mycompany.app</groupId>
                          <artifactId>my-dep</artifactId>
                          <version>1</version>
                          <exclusions>
                              <exclusion>
                                  <groupId>org.slf4j</groupId>
                                  <artifactId>slf4j-api</artifactId>
                              </exclusion>
                          </exclusions>
                        </dependency>
                    </dependencies>
                </project>
            """
        )[1]
        //With one exclusion in the dependency and one in the managed dependency, both transitive dependencies
        //should be excluded.
        assertThat(pomXml.mavenResolutionResult().dependencies[Scope.Compile]?.size).isEqualTo(1)
    }

    @Suppress("CheckDtdRefs")
    @Test
    fun parse() {
        val maven = parser.parse(
            ctx,
            """
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
                        <scope>test</scope>
                      </dependency>
                    </dependencies>
                </project>
            """
        )[0]

        assertThat(maven.mavenResolutionResult().dependencies[Scope.Test]?.first()?.licenses?.first()?.type)
            .isEqualTo(License.Type.Eclipse)
        assertThat(maven.mavenResolutionResult().dependencies[Scope.Test]?.first()?.type)
            .isEqualTo("jar")
        assertThat(maven.mavenResolutionResult().pom.packaging)
            .isEqualTo("pom")
    }

    @Test
    fun emptyArtifactPolicy() {
        // example from https://repo1.maven.org/maven2/org/openid4java/openid4java-parent/0.9.6/openid4java-parent-0.9.6.pom
        parser.parse(
            ctx,
            """
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
            """
        )
    }

    @Test
    fun handlesRepositories() {
        parser.parse(
            ctx,
            """
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
            """
        )
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/198")
    @Test
    fun handlesPropertiesInDependencyScope() {
        val maven = parser.parse(
            ctx,
            """
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
            """
        ).first()
        assertThat(maven.mavenResolutionResult().dependencies[Scope.Compile]).hasSize(7)
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/199")
    @Test
    fun continueOnInvalidScope() {
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
        parser.parse(ctx, invalidPom)
        parser.parse(invalidPom)
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/135")
    @Test
    fun selfRecursiveParent() {
        MavenParser.builder()
            .build()
            .parse(
                ctx,
                """
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
                """
            )
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/135")
    @Test
    fun selfRecursiveDependency() {
        val maven = MavenParser.builder()
            .build()
            .parse(
                ctx,
                """
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
                """
            )
            .first()

        // Maven itself would respond to this pom with a fatal error.
        // So long as we don't produce an AST with cycles it's OK
        assertThat(maven.mavenResolutionResult().dependencies[Scope.Compile]).hasSize(1)
    }

    @Test
    fun managedDependenciesInParentInfluenceTransitives() {
        @Language("xml")
        val parent = """
            <project>
                <groupId>com.foo</groupId>
                <artifactId>parent</artifactId>
                <version>1</version>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>org.glassfish.jaxb</groupId>
                            <artifactId>jaxb-runtime</artifactId>
                            <version>2.3.3</version>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
            </project>
        """

        @Language("xml")
        val pomSource = """
            <project>
                <parent>
                    <groupId>com.foo</groupId>
                    <artifactId>parent</artifactId>
                    <version>1</version>
                </parent>
                <groupId>com.foo</groupId>
                <artifactId>test</artifactId>
                <dependencies>
                    <dependency>
                        <groupId>org.hibernate</groupId> 
                        <artifactId>hibernate-core</artifactId>
                        <version>5.4.28.Final</version>
                    </dependency>
                </dependencies>
            </project>
        """

        val maven = parser.parse(ctx, pomSource, parent)[0]
        assertThat(maven.mavenResolutionResult().dependencies[Scope.Compile]?.map { it.artifactId to it.version })
            .contains("jaxb-runtime" to "2.3.3")
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/323")
    @Test
    fun inheritScopeFromDependencyManagement() {
        val pomSource = """
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

        val maven = parser.parse(ctx, pomSource)[0]
        assertThat(maven.mavenResolutionResult().dependencies[Scope.Test]?.map { it.artifactId })
            .contains("junit-jupiter", "guava")
        assertThat(maven.mavenResolutionResult().dependencies[Scope.Compile]?.map { it.artifactId })
            .doesNotContain("junit-jupiter")
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/323")
    @Test
    fun dependencyScopeTakesPrecedenceOverDependencyManagementScope() {
        val pomSource = """
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

        val maven = parser.parse(ctx, pomSource)[0]
        assertThat(maven.mavenResolutionResult().dependencies[Scope.Compile]?.map { it.artifactId }?.take(2))
            .containsExactly("junit-jupiter", "guava")
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
                    if (request.headers.find {
                            it.first == "Authorization" && it.second == Credentials.basic(
                                username,
                                password
                            )
                        } == null) {
                        return MockResponse().apply {
                            setResponseCode(401)
                        }
                    } else {
                        return MockResponse().apply {
                            setResponseCode(200)
                            setBody(
                                """
                                <project>
                                  <modelVersion>4.0.0</modelVersion>

                                  <groupId>com.foo</groupId>
                                  <artifactId>bar</artifactId>
                                  <version>1.0.0</version>
                                  
                                </project>
                            """
                            )
                        }
                    }
                }
            }
        }.use { mockRepo ->
            mockRepo.start()

            val ctx = MavenExecutionContextView(InMemoryExecutionContext { err -> throw err })
            val settings = MavenSettings.parse(Parser.Input(Paths.get("settings.xml")) {
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
                """.byteInputStream()
            }, ctx)!!

            ctx.setMavenSettings(settings)

            val maven = parser.parse(
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

            assertThat(maven.mavenResolutionResult().dependencies[Scope.Compile])
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
            .parse(
                ctx,
                """
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
            .find { it.mavenResolutionResult().pom.artifactId == "a" }!!

        assertThat(maven.mavenResolutionResult().dependencies[Scope.Compile]?.first()?.version).isEqualTo("0.1.0-SNAPSHOT")
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
            .parse(
                ctx,
                """
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
            ).find { it.mavenResolutionResult().pom.artifactId == "a" }!!

        assertThat(maven.mavenResolutionResult().dependencies[Scope.Compile]?.first()?.version).isEqualTo("0.1.0-SNAPSHOT")
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/378")
    @Test
    fun parseNotInProfileActivation() {
        MavenParser.builder()
            .build()
            .parse(
                ctx,
                """
                    <project>
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>test</artifactId>
                        <version>0.1.0-SNAPSHOT</version>
                        <profiles>
                            <profile>
                              <id>repo-incode-work</id>
                              <properties>
                                <name>!skip.repo-incode-work</name>
                              </properties>
                            </profile>
                        </profiles>
                    </project>
                """
            )
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1427")
    @Test
    fun parseEmptyActivationTag() {
        val pomXml = MavenParser.builder()
            .build()
            .parse(
                ctx,
                """
                    <project>
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>test</artifactId>
                        <version>0.1.0-SNAPSHOT</version>
                        <profiles>
                            <profile>
                              <id>repo-incode-work</id>
                              <properties>
                                <name>!skip.repo-incode-work</name>
                              </properties>
                              <activation/>
                            </profile>
                        </profiles>
                    </project>
                """
            )[0]
        assertThat(pomXml.mavenResolutionResult().pom.requested.profiles[0].activation).isNull()

    }

    @Suppress("CheckTagEmptyBody")
    @Issue("https://github.com/openrewrite/rewrite/issues/1427")
    @Test
    fun parseEmptyValueActivationTag() {
        val pomXml = MavenParser.builder()
            .build()
            .parse(
                ctx,
                """
                    <project>
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>test</artifactId>
                        <version>0.1.0-SNAPSHOT</version>
                        <profiles>
                            <profile>
                              <id>repo-incode-work</id>
                              <properties>
                                <name>!skip.repo-incode-work</name>
                              </properties>
                              <activation></activation>
                            </profile>
                        </profiles>
                    </project>
                """
            )[0]
        @Suppress("USELESS_CAST")
        assertThat(pomXml.mavenResolutionResult().pom.requested.profiles[0].activation?.activeByDefault as Boolean?).isNull()
        assertThat(pomXml.mavenResolutionResult().pom.requested.profiles[0].activation?.jdk).isNull()
        assertThat(pomXml.mavenResolutionResult().pom.requested.profiles[0].activation?.property).isNull()

    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1427")
    @Test
    fun parseWithActivationTag() {
        val pomXml = MavenParser.builder()
            .build()
            .parse(
                ctx,
                """
                    <project>
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>test</artifactId>
                        <version>0.1.0-SNAPSHOT</version>
                        <profiles>
                            <profile>
                              <id>repo-incode-work</id>
                              <properties>
                                <name>!skip.repo-incode-work</name>
                              </properties>
                              <activation>
                                <activeByDefault>true</activeByDefault>
                              </activation>
                            </profile>
                        </profiles>
                    </project>
                """
            )[0]
        assertThat(pomXml.mavenResolutionResult().pom.requested.profiles[0].activation!!.activeByDefault as Boolean).isTrue
    }

    @Test
    fun parentPomProfileProperty() {
        val maven = MavenParser.builder()
            .build()
            .parse(
                ctx,
                """
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
                """
            ).find { it.mavenResolutionResult().pom.artifactId == "a" }

        assertThat(maven!!.mavenResolutionResult().dependencies[Scope.Compile])
            .hasSize(7)
            .matches { it.first().artifactId == "guava" && it.first().version == "29.0-jre" }
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/376")
    @Disabled
    @Test
    fun dependencyManagementPropagatesToDependencies() {
        // a depends on b
        // a-parent manages version of d to 0.1
        // b depends on d without specifying version
        // b-parent manages version of d to 0.2
        // Therefore the version of b that wins is 0.1
        val a = MavenParser.builder()
            .build()
            .parse(
                ctx,
                """
                    <project>
                        <parent>
                            <groupId>org.openrewrite.maven</groupId>
                            <artifactId>a-parent</artifactId>
                            <version>0.1.0-SNAPSHOT</version>
                            <relativePath />
                        </parent>
                    
                        <artifactId>a</artifactId>
                    
                        <dependencies>
                            <dependency>
                                <groupId>org.openrewrite.maven</groupId>
                                <artifactId>b</artifactId>
                                <version>0.1.0-SNAPSHOT</version>
                            </dependency>
                        </dependencies>
                    </project>
                """,
                """
                    <project>
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>a-parent</artifactId>
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
                        <parent>
                            <groupId>org.openrewrite.maven</groupId>
                            <artifactId>b-parent</artifactId>
                            <version>0.1.0-SNAPSHOT</version>
                            <relativePath />
                        </parent>
                    
                        <artifactId>b</artifactId>
                    
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
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>b-parent</artifactId>
                        <version>0.1.0-SNAPSHOT</version>
                        <packaging>pom</packaging>
                        <dependencyManagement>
                            <dependencies>
                                <dependency>
                                    <groupId>org.openrewrite.maven</groupId>
                                    <artifactId>d</artifactId>
                                    <version>0.2.0-SNAPSHOT</version>
                                </dependency>
                            </dependencies>
                        </dependencyManagement>
                    </project>
                """,
                """
                    <project>
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>d</artifactId>
                        <version>0.1.0-SNAPSHOT</version>
                    </project>
                """,
                """
                    <project>
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>d</artifactId>
                        <version>0.2.0-SNAPSHOT</version>
                    </project>
                """
            )
            .find { it.mavenResolutionResult().pom.artifactId == "a" }

        val compileDependencies = a!!.mavenResolutionResult().dependencies[Scope.Compile]
        assertThat(compileDependencies).hasSize(2)

        assertThat(compileDependencies).anyMatch { it.artifactId == "b" && it.version == "0.1.0-SNAPSHOT" }
        assertThat(compileDependencies).anyMatch { it.artifactId == "d" && it.version == "0.1.0-SNAPSHOT" }
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1422")
    @Test
    fun managedDependencyInTransitiveAndPom() {
        // a has a managed dependency on junit:junit:4.11
        // a has a dependency defined for junit:junit (version is managed to 4.11)
        // ------------------------
        // b has a managed dependency on junit:junit (with an exclusion on hamcrest, but does NOT define version)
        // b has a dependency on a
        // b does NOT have a direct dependency on junit.
        //
        // b -> a -> junit
        //
        // Resolve dependencies on b should include junit:junit:4.11 but NOT hamcrest.
        val maven = MavenParser.builder()
            .build()
            .parse(
                ctx,
                """
                    <project>
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.managed.test</groupId>
                        <artifactId>a</artifactId>
                        <version>1.0.0</version>
                        <packaging>jar</packaging>

                        <dependencyManagement>
                            <dependencies>
                                <dependency>
                                    <groupId>junit</groupId>
                                    <artifactId>junit</artifactId>
                                    <version>4.11</version>
                                </dependency>
                            </dependencies>
                        </dependencyManagement>
                    
                        <dependencies>
                            <dependency>
                                <groupId>junit</groupId>
                                <artifactId>junit</artifactId>
                            </dependency>
                        </dependencies>
                    </project>
                """,
                """
                    <project>
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.managed.test</groupId>
                        <artifactId>b</artifactId>
                        <version>1.0.0</version>
                        <packaging>jar</packaging>                    
                        <dependencyManagement>
                            <dependencies>
                                <dependency>
                                    <groupId>junit</groupId>
                                    <artifactId>junit</artifactId>
                                    <exclusions>
                                        <exclusion>
                                            <groupId>org.hamcrest</groupId>
                                            <artifactId>hamcrest-core</artifactId>
                                        </exclusion>
                                    </exclusions>
                                </dependency>
                            </dependencies>
                        </dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>com.managed.test</groupId>
                                <artifactId>a</artifactId>
                                <version>1.0.0</version>
                            </dependency>
                        </dependencies>
                    </project>
                """
            ).find { it.mavenResolutionResult().pom.artifactId == "b" }!!
        val compileDependencies = maven.mavenResolutionResult().dependencies[Scope.Compile]
        assertThat(compileDependencies).anyMatch { it.artifactId == "junit" && it.version == "4.11" }
        assertThat(compileDependencies).noneMatch { it.artifactId == "hamcrest-core" }
    }

    @Test
    fun profileNoJdkActivation() {
        val maven = parser.parse(ctx,
            """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>

                    <profiles>
                        <profile>
                            <id>old-jdk</id>
                            <activation>
                                <jdk>1.5</jdk>
                            </activation>
                            <dependencies>
                                <dependency>
                                      <groupId>junit</groupId>
                                      <artifactId>junit</artifactId>
                                      <version>4.11</version>
                                </dependency>
                            </dependencies>
                        </profile>
                    </profiles>        
                </project>
            """
        ).find { it.mavenResolutionResult().pom.artifactId == "my-app" }!!

        val compileDependencies = maven.mavenResolutionResult().dependencies[Scope.Compile]
        assertThat(compileDependencies).isEmpty()

    }

    @Test
    fun profileJdkSoftVersionActivation() {
        val maven = parser.parse(ctx,
            """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>

                    <profiles>
                        <profile>
                            <id>old-jdk</id>
                            <activation>
                                <jdk>11.0</jdk>
                            </activation>
                            <dependencies>
                                <dependency>
                                      <groupId>junit</groupId>
                                      <artifactId>junit</artifactId>
                                      <version>4.11</version>
                                </dependency>
                            </dependencies>
                        </profile>
                    </profiles>        
                </project>
            """
        ).find { it.mavenResolutionResult().pom.artifactId == "my-app" }!!

        val compileDependencies = maven.mavenResolutionResult().dependencies[Scope.Compile]
        assertThat(compileDependencies).hasSize(2)

    }

}
