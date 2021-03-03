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
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Parser
import org.openrewrite.maven.cache.LocalMavenArtifactCache
import org.openrewrite.maven.cache.ReadOnlyLocalMavenArtifactCache
import org.openrewrite.maven.tree.Maven
import org.openrewrite.maven.tree.Scope
import org.openrewrite.maven.utilities.MavenArtifactDownloader
import java.nio.file.Path
import java.nio.file.Paths

class MavenDependencyDownloadIntegTest {
    private val ctx = InMemoryExecutionContext { t -> t.printStackTrace() }

    private fun downloader(path: Path) = MavenArtifactDownloader(
        ReadOnlyLocalMavenArtifactCache.MAVEN_LOCAL.orElse(
            LocalMavenArtifactCache(path)
        ),
        null
    ) { t -> throw t }

    @Test
    fun springWebMvc(@TempDir tempDir: Path) {
        val maven: Maven = MavenParser.builder()
            .resolveOptional(false)
            .build()
            .parse(ctx, singleDependencyPom("org.springframework:spring-webmvc:5.3.2"))
            .first()

        val compileDependencies = maven.model.getDependencies(Scope.Compile)

        compileDependencies.forEach { dep ->
            println("${dep.repository} ${dep.coordinates}")
        }

        val downloader = downloader(tempDir)
        compileDependencies.forEach { dep ->
            println(dep.coordinates + downloader.downloadArtifact(dep))
        }
    }

    @Test
    fun rewriteCore(@TempDir tempDir: Path) {
        val maven: Maven = MavenParser.builder()
            .resolveOptional(false)
            .build()
            .parse(ctx, singleDependencyPom("org.openrewrite:rewrite-core:6.0.1"))
            .first()

        val runtimeDependencies = maven.model.getDependencies(Scope.Runtime)
            .sortedBy { d -> d.coordinates }

        val downloader = downloader(tempDir)
        runtimeDependencies.forEach { dep ->
            println(dep.coordinates + downloader.downloadArtifact(dep))
        }
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
                    if(request.headers.find { it.first == "Authorization" && it.second == Credentials.basic(username, password) } == null) {
                        return MockResponse().apply {
                            setResponseCode(401)
                        }
                    }
                    else {
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

            // Don't worry about the credentials stored in here being useful for any other purpose or worth protecting
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
}
