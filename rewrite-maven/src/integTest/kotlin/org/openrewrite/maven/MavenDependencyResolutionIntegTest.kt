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

import ch.qos.logback.classic.Level
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.config.MeterFilter
import org.apache.maven.model.Model
import org.apache.maven.model.Repository
import org.apache.maven.model.building.DefaultModelBuilderFactory
import org.apache.maven.model.building.DefaultModelBuildingRequest
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.graph.DefaultDependencyNode
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.graph.DependencyNode
import org.eclipse.aether.graph.Exclusion
import org.eclipse.aether.internal.impl.DefaultRemoteRepositoryManager
import org.eclipse.aether.repository.RemoteRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Issue
import org.openrewrite.maven.cache.InMemoryMavenPomCache
import org.openrewrite.maven.tree.Maven
import org.openrewrite.maven.tree.Pom
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.util.function.Consumer

/**
 * Compare rewrite-maven with Maven Aether.
 */
class MavenDependencyResolutionIntegTest {
    @Suppress("unused")
    companion object {
//        private val meterRegistry = MetricsDestinations.prometheus()
        private val meterRegistry = Metrics.globalRegistry
//        private val mavenCache = MapdbCache(File(System.getProperty("user.home") + "/.m2/rewrite"), null)
//        private val mavenCache = NoopCache()
        private val mavenCache = InMemoryMavenPomCache()

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger).level =
                    Level.INFO

            meterRegistry.config().meterFilter(MeterFilter.ignoreTags("artifact.id"))
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            mavenCache.close()
        }
    }

    @Issue("#93")
    @Test
    fun snapshotVersion() {
        val pom = """
            <project>
              <modelVersion>4.0.0</modelVersion>

              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              
              <repositories>
                <repository>
                    <snapshots />
                    <id>OSS JFrog</id>
                    <name>OJO</name>
                    <url>https://oss.jfrog.org/artifactory/libs-snapshot</url>
                </repository>
              </repositories>
              
              <dependencies>
                <dependency>
                    <groupId>org.openrewrite</groupId>
                    <artifactId>rewrite-core</artifactId>
                    <version>6.2.0-SNAPSHOT</version>
                </dependency>
              </dependencies>
            </project>
        """.trimIndent()

        val maven = MavenParser.builder()
                .cache(mavenCache)
                .resolveOptional(false)
                .build()
                .parse(pom)
                .first()

        assertThat(maven.model.dependencies.first().version).isEqualTo("6.2.0-SNAPSHOT")
        assertThat(maven.model.dependencies.first().model.snapshotVersion).startsWith("6.2.0")
    }

    @Issue("#166")
    @Test
    fun testMultipleRepositories() {

        val pom = """
            <project>
                <modelVersion>4.0.0</modelVersion>
            
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <developers>
                    <developer>
                        <name>Trygve Laugst&oslash;l</name>
                    </developer>
                </developers>

                <repositories>
                    <repository>
                        <id>maven-central</id>
                        <name>Maven Central Repository</name>
                        <url>https://repo.maven.apache.org/maven2</url>
                        <releases>
                            <enabled>true</enabled>
                            <updatePolicy>always</updatePolicy>
                        </releases>                        
                    </repository>
                    <repository>
                        <id>apache-m2-snapshot</id>
                        <name>Apache Snapshot Repository</name>
                        <url>https://repository.apache.org/content/groups/snapshots</url>
                    </repository>
                    <repository>
                        <id>jboss-public-repository-group</id>
                        <name>JBoss Public Maven Repository Group</name>
                        <url>https://repository.jboss.org/nexus/content/repositories/releases/</url>
                        <layout>default</layout>
                        <releases>
                            <enabled>true</enabled>
                            <updatePolicy>always</updatePolicy>
                        </releases>
                        <snapshots>
                            <enabled>true</enabled>
                            <updatePolicy>always</updatePolicy>
                        </snapshots>
                    </repository>
                </repositories>
                <dependencies>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter</artifactId>
                    </dependency>
                </dependencies>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-dependencies</artifactId>
                            <version>2.3.4.RELEASE</version>
                            <type>pom</type>
                            <scope>import</scope>
                        </dependency>
                    </dependencies>
                </dependencyManagement>                
            </project>
        """.trimIndent()

        val parser = MavenParser.builder().build()
        parser.parse(pom)[0]
    }

    @Test
    fun springBootConfigurationProcessor(@TempDir tempDir: Path) {
        val pom = """
            <project>
              <modelVersion>4.0.0</modelVersion>
             
              <parent>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-netflix</artifactId>
                <version>3.0.0-SNAPSHOT</version>
              </parent>

              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              
              <repositories>
                <repository>
                    <id>spring-snapshot</id>
                    <name>Spring Snapshots</name>
                    <url>http://repo.spring.io/snapshot</url>
                    <snapshots/>
                </repository>
              </repositories>
              
              <dependencies>
                <dependency>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-configuration-processor</artifactId>
                </dependency>
              </dependencies>
            </project>
        """.trimIndent()

        assertDependencyResolutionEqualsAether(tempDir, pom)
    }

    @Test
    fun springBootParent(@TempDir tempDir: Path) {
        val pom = """
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
              
              <dependencies>
                <dependency>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter</artifactId>
                </dependency>
              </dependencies>
            </project>
        """.trimIndent()

        assertDependencyResolutionEqualsAether(tempDir, pom)
    }

    @Test
    fun missingAtEof(@TempDir tempDir: Path) {
        assertDependencyResolutionEqualsAether(tempDir,
            singleDependencyPom("com.fasterxml.jackson:jackson-base:2.12.0-rc2"))
    }

    @Test
    fun dearGoogleWhyDoesYourNameAppearWhenIHaveProblemsSoMuch(@TempDir tempDir: Path) {
        assertDependencyResolutionEqualsAether(tempDir,
                singleDependencyPom("com.google.cloud:google-cloud-shared-config:0.9.2"))
    }

    /**
     * Has a tag like this: {@code <replaceregexp match="/tags/[^ "'<]*"/>}
     */
    @Test
    fun malformedQuotedAttribute(@TempDir tempDir: Path) {
        assertDependencyResolutionEqualsAether(tempDir, singleDependencyPom("log4j:log4j:1.2.17"))
    }

    @Test
    fun springWeb(@TempDir tempDir: Path) {
        assertDependencyResolutionEqualsAether(tempDir, singleDependencyPom("org.springframework:spring-web:4.0.9.RELEASE"))
    }

    @Test
    fun springWebMvc(@TempDir tempDir: Path) {
        assertDependencyResolutionEqualsAether(tempDir, singleDependencyPom("org.springframework:spring-webmvc:4.3.6.RELEASE"))
    }

    @Test
    fun importDependencies(@TempDir tempDir: Path) {
        val pom = """
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
              <modelVersion>4.0.0</modelVersion>
              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>2.3.5.RELEASE</version>
                <relativePath/> <!-- lookup parent from repository -->
              </parent>
              <groupId>com.example</groupId>
              <artifactId>demo</artifactId>
              <version>0.0.1-SNAPSHOT</version>
              <name>demo</name>
              <description>Demo project for Spring Boot</description>
              <properties>
                <java.version>11</java.version>
                <spring-cloud.version>Hoxton.SR8</spring-cloud.version>
              </properties>
              <dependencies>
                <dependency>
                  <groupId>org.springframework.cloud</groupId>
                  <artifactId>spring-cloud-starter-oauth2</artifactId>
                </dependency>
              </dependencies>
              <dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>org.springframework.cloud</groupId>
                    <artifactId>spring-cloud-dependencies</artifactId>
                    <version>${'$'}{spring-cloud.version}</version>
                    <type>pom</type>
                    <scope>import</scope>
                  </dependency>
                </dependencies>
              </dependencyManagement>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                  </plugin>
                </plugins>
              </build>
            </project>
        """.trimIndent()

        assertDependencyResolutionEqualsAether(tempDir, pom)
    }

    @Test
    fun exclusions(@TempDir tempDir: Path) {
        val pom = """
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
                    <exclusions>
                      <exclusion>
                        <groupId>*</groupId>
                        <artifactId>*</artifactId>
                      </exclusion>
                    </exclusions>
                  </dependency>
                </dependencies>
            </project>
        """.trimIndent()

        assertDependencyResolutionEqualsAether(tempDir, pom)
    }

    @Test
    fun jacksonCoreAsl(@TempDir tempDir: Path) {
        // empty dependencies block with a comment
        assertDependencyResolutionEqualsAether(tempDir, singleDependencyPom("org.codehaus.jackson:jackson-core-asl:1.9.13"))
    }

    @Test
    fun springCloudSecurityOauth2(@TempDir tempDir: Path) {
        assertDependencyResolutionEqualsAether(tempDir, singleDependencyPom("org.springframework.cloud:spring-cloud-starter-oauth2:2.2.4.RELEASE"))
    }

    @Test
    fun springCloudStarterSecurity(@TempDir tempDir: Path) {
        assertDependencyResolutionEqualsAether(tempDir, singleDependencyPom("org.springframework.cloud:spring-cloud-starter-security:2.2.4.RELEASE"))
    }

    @Test
    fun springBootStarterSecurity(@TempDir tempDir: Path) {
        assertDependencyResolutionEqualsAether(tempDir, singleDependencyPom("org.springframework.boot:spring-boot-starter-security:2.3.2.RELEASE"))
    }

    @Test
    fun springBootStarterActuator(@TempDir tempDir: Path) {
        assertDependencyResolutionEqualsAether(tempDir, singleDependencyPom("org.springframework.boot:spring-boot-starter-actuator:2.3.2.RELEASE"))
    }

    @Test
    fun micrometerCore(@TempDir tempDir: Path) {
        assertDependencyResolutionEqualsAether(tempDir, singleDependencyPom("io.micrometer:micrometer-core:1.5.3"))
    }

    @Test
    fun bouncycastle(@TempDir tempDir: Path) {
        assertDependencyResolutionEqualsAether(tempDir, singleDependencyPom("org.bouncycastle:bcpkix-jdk15on:1.64"))
    }

    @Test
    fun istack(@TempDir tempDir: Path) {
        assertDependencyResolutionEqualsAether(tempDir, singleDependencyPom("com.sun.istack:istack-commons-runtime:3.0.11"))
    }

    @Test
    fun portlet(@TempDir tempDir: Path) {
        assertDependencyResolutionEqualsAether(tempDir, singleDependencyPom("javax.portlet:portlet-api:2.0"))
    }

    @Test
    fun springBoot(@TempDir tempDir: Path) {
        assertDependencyResolutionEqualsAether(tempDir, singleDependencyPom("org.springframework.boot:spring-boot:2.1.2.RELEASE"))
    }

    @Test
    fun junit(@TempDir tempDir: Path) {
        assertDependencyResolutionEqualsAether(tempDir, singleDependencyPom("org.junit.jupiter:junit-jupiter:5.7.0"))
    }

    @Test
    fun parentDependencyManagementReliesOnPropertyDefinedInChild(@TempDir tempDir: Path) {
        assertDependencyResolutionEqualsAether(tempDir, singleDependencyPom("org.springframework.security.oauth:spring-security-oauth2:2.3.4.RELEASE"))
    }

    /**
     * Specifically, the property ${jetty.npn.version} used in the dependencyManagement section of the
     * netty parent POM.
     */
    @Test
    fun propertiesDefinedInProfiles(@TempDir tempDir: Path) {
        assertDependencyResolutionEqualsAether(tempDir, singleDependencyPom("io.netty:netty-handler:4.0.32.Final"))
    }

    @Test
    fun comSunJava(@TempDir tempDir: Path) {
        // this pom includes a com.sun.java:tools dependency that is dependency managed onto the system scope
        // and includes a systemPath element:

        /*
        <dependency>
            <groupId>com.sun.java</groupId>
            <artifactId>tools</artifactId>
            <version>${java.version}</version>
            <scope>system</scope>
            <systemPath>${java.home}/../lib/tools.jar</systemPath>
        </dependency>
         */

        assertDependencyResolutionEqualsAether(tempDir, singleDependencyPom("net.openhft:lang:6.6.2"))
    }

    private fun assertDependencyResolutionEqualsAether(tempDir: Path, pom: String, ignoreScopes: Boolean = false) {
        val pomFile = tempDir.resolve("pom.xml").toFile().apply { writeText(pom) }

        val pomAst: Maven = MavenParser.builder()
                .cache(mavenCache)
                .resolveOptional(false)
                .build()
                .parse(listOf(pomFile.toPath()), null, InMemoryExecutionContext())
                .first()

        val rewrite = printTreeRecursive(pomAst.model, ignoreScopes)

//        println(rewrite)

        val aether = MavenAetherParser().dependencyTree(pomFile, ignoreScopes)

//        println(aether)

        assertThat(rewrite).isEqualTo(aether)
    }

    private fun printTreeRecursive(maven: Pom, ignoreScopes: Boolean): String {
        return maven.dependencies
                .filterNot { it.isOptional }
                .sortedWith { d1, d2 ->
                    if (d1.groupId == d2.groupId)
                        d1.artifactId.compareTo(d2.artifactId)
                    else
                        d1.groupId.compareTo(d2.groupId)
                }
                .joinToString("\n") { dep ->
                    dependencyString(dep, ignoreScopes) + printTreeRecursive(dep.model, ignoreScopes)
                            .let { if (it.isBlank()) "" else "\n${it.prependIndent(" ")}" }
                }
    }

    private fun dependencyString(dep: Pom.Dependency, ignoreScopes: Boolean): String =
            dep.run { "$groupId:$artifactId:$version${if (classifier?.isNotBlank() == true) ":${classifier}" else ""}" } +
                    (if (ignoreScopes) "" else "[${dep.scope.toString().toLowerCase()}]") +
                    dep.run { " https://repo1.maven.org/maven2/${groupId.replace(".", "/")}/$artifactId/$version/$artifactId-$version.pom" }

    class MavenAetherParser {
        private val repositorySystem = MavenRepositorySystemUtils.getRepositorySystem()
        private val localRepository = File(System.getProperty("user.home") + "/.m2/rewrite-aether")
        private val repositorySystemSession: RepositorySystemSession = MavenRepositorySystemUtils
                .getRepositorySystemSession(repositorySystem, localRepository)

        // The type might be called "RemoteRepository" but file:// urls are perfectly acceptable
        private val repositories = listOf(RemoteRepository.Builder("local", "default", localRepository.toURI().toString()).build())

        fun dependencyTree(pom: File, ignoreScopes: Boolean): String {
            val modelBuilder = DefaultModelBuilderFactory().newInstance()

            val modelBuildingRequest = DefaultModelBuildingRequest()
                    .setModelResolver(ParentModelResolver(
                            repositorySystemSession,
                            repositorySystem,
                            DefaultRemoteRepositoryManager(),
                            repositories + RemoteRepository.Builder("central", "default",
                                    "https://repo.maven.apache.org/maven2").build()
                    ))
                    .setSystemProperties(System.getProperties())
                    .setPomFile(pom)

            val modelBuildingResult = modelBuilder.build(modelBuildingRequest)

            val model = modelBuildingResult.effectiveModel

            val collectRequest = CollectRequest()
            collectRequest.repositories = remoteRepositoriesFromModel(model)
            collectRequest.dependencies = model.dependencies.map { d ->
                Dependency(
                        DefaultArtifact(d.groupId, d.artifactId, d.classifier, "jar", d.version),
                        d.scope,
                        d.optional == "true",
                        d.exclusions.map { e -> Exclusion(e.groupId, e.artifactId, "*", "*") }
                )
            }

            val collectResult = repositorySystem.collectDependencies(repositorySystemSession, collectRequest)
            return printTreeRecursive(collectResult.root, ignoreScopes).trimIndent()
        }

        private fun printTreeRecursive(node: DependencyNode, ignoreScopes: Boolean, originalScope: String = "compile"): String =
                dependencyString(node, ignoreScopes, originalScope) + (node.children
                        .filter { n -> n.dependency.scope != "system" }
                        .sortedWith { n1, n2 ->
                            val d1 = n1.dependency.artifact
                            val d2 = n2.dependency.artifact
                            if (d1.groupId == d2.groupId)
                                d1.artifactId.compareTo(d2.artifactId)
                            else
                                d1.groupId.compareTo(d2.groupId)
                        }
                        .joinToString("\n") {
                            val scope =
                                    if (it.data["conflict.originalScope"] != it.dependency.scope)
                                        it.data["conflict.originalScope"] as String
                                    else it.dependency.scope

                            printTreeRecursive(it.data["conflict.winner"] as DefaultDependencyNode?
                                    ?: it, ignoreScopes, scope)
                        }
                        .let { if (it.isBlank()) "" else "\n${it.prependIndent(" ")}" })

        private fun dependencyString(node: DependencyNode, ignoreScopes: Boolean, originalScope: String): String =
                node.dependency?.run {
                    artifact.run { "$groupId:$artifactId:$version${if (classifier.isNotBlank()) ":${classifier}" else ""}" } +
                            (if (ignoreScopes) "" else "[$originalScope]") +
                            " https://repo1.maven.org/maven2/${artifact.run { "${groupId.replace(".", "/")}/$artifactId/$version/$artifactId-$version.pom" }}"
                } ?: ""

        private fun remoteRepositoriesFromModel(model: Model): List<RemoteRepository> {
            val remotes: MutableList<RemoteRepository> = mutableListOf()
            model.repositories.forEach(Consumer { repo: Repository ->
                remotes.add(RemoteRepository.Builder(repo.id, "default",
                        repo.url).build())
                if (repo.url.contains("http://")) {
                    remotes.add(
                            RemoteRepository.Builder(repo.id, "default",
                                    repo.url.replace("http:", "https:")).build())
                }
            })
            return remotes
        }
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/95")
    @Test
    fun bomImportedFromParent() {
        val maven = MavenParser.builder()
                .build()
                .parse("""
                    <project>
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-app</artifactId>
                      <version>1</version>
                      
                      <parent>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-dependencies</artifactId>
                        <version>2.1.6.RELEASE</version>
                      </parent>
                      
                      <dependencies>
                        <dependency>
                          <groupId>org.springframework</groupId>
                          <artifactId>spring-messaging</artifactId>
                        </dependency>
                      </dependencies>
                    </project>
                """.trimIndent()).first()

        assertThat(maven.model.dependencies.first().version).isEqualTo("5.1.8.RELEASE")
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/124")
    @Test
    fun indirectBomImportedFromParent() {
        val maven = MavenParser.builder()
            .build()
            .parse(
                """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    
                    <parent>
                        <groupId>org.springframework.cloud</groupId>
                        <artifactId>spring-cloud-netflix-eureka-server</artifactId>
                        <version>3.0.0-SNAPSHOT</version>
                        <relativePath/>
                    </parent>
                    
                    <dependencies>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-web</artifactId>
                        </dependency>
                    </dependencies>
                    
                    <repositories>
                      <repository>
                        <snapshots />
                        <id>Spring Snapshots</id>
                        <name>SpringSnapshots</name>
                        <url>https://repo.spring.io/libs-snapshot</url>
                      </repository>
                    </repositories>
                </project>
                """.trimIndent())
            .find { it.model.artifactId == "my-app" }!!

        assertThat(maven.model.dependencies.first().version).isNotBlank
    }
}
