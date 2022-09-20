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

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.openrewrite.HttpSenderExecutionContextView
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.ipc.http.HttpUrlConnectionSender
import org.openrewrite.maven.MavenParser
import org.openrewrite.maven.tree.GroupArtifactVersion
import org.openrewrite.maven.tree.MavenRepository
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger


@Suppress("HttpUrlsUsage")
class MavenPomDownloaderTest {
    private val ctx = HttpSenderExecutionContextView.view(InMemoryExecutionContext())
        .setHttpSender(HttpUrlConnectionSender(Duration.ofMillis(100), Duration.ofMillis(100)))

    private fun mockServer(responseCode: Int, block: (MockWebServer) -> Unit) {
        MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    return MockResponse()
                        .setResponseCode(responseCode)
                        .setBody("")
                }
            }
        }.use { mockRepo ->
            mockRepo.start()
            block(mockRepo)
            assertThat(mockRepo.requestCount)
                .isGreaterThan(0)
                .`as`("The mock repository received no requests. The test is not using it.d")
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [500, 400])
    fun normalizeAcceptErrorStatuses(status: Int) {
        val downloader = MavenPomDownloader(emptyMap(), ctx)
        mockServer(status) { mockRepo ->
            val originalRepo = MavenRepository(
                "id",
                "http://${mockRepo.hostName}:${mockRepo.port}/maven",
                true,
                true,
                false,
                null,
                null,
                false
            )
            val normalizedRepo = downloader.normalizeRepository(originalRepo, null)
            assertThat(normalizedRepo).isEqualTo(originalRepo)
        }
    }

    @Test
    fun normalizeRejectConnectException() {
        val downloader = MavenPomDownloader(emptyMap(), ctx)
        val normalizedRepository = downloader.normalizeRepository(
            MavenRepository("id", "https://localhost", true, true, false, null, null, false),
            null
        )
        assertThat(normalizedRepository).isEqualTo(null)
    }

    @Test
    fun invalidArtifact() {
        val downloader = MavenPomDownloader(emptyMap(), ctx)
        val gav = GroupArtifactVersion("fred", "fred", "1.0.0")

        mockServer(500) { repo1 ->
            mockServer(400) { repo2 ->
                val repositories = listOf(
                    MavenRepository(
                        "id",
                        "http://${repo1.hostName}:${repo1.port}/maven",
                        true,
                        true,
                        false,
                        null,
                        null,
                        false
                    ),
                    MavenRepository(
                        "id2",
                        "http://${repo2.hostName}:${repo2.port}/maven",
                        true,
                        true,
                        false,
                        null,
                        null,
                        false
                    )
                )

                assertThatThrownBy { downloader.download(gav, null, null, repositories) }
                    .isInstanceOf(MavenDownloadingException::class.java)
                    .hasMessageContaining("http://${repo1.hostName}:${repo1.port}/maven")
                    .hasMessageContaining("http://${repo2.hostName}:${repo2.port}/maven")
            }
        }
    }

    @Test
    @Disabled
    fun dontFetchSnapshotsFromReleaseRepos() {
        val snapshotRepo = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    return MockResponse().apply {
                        setResponseCode(200)
                        if (request.path!!.contains("maven-metadata")) {
                            setBody(
                                //language=xml
                                """
                                <metadata modelVersion="1.1.0">
                                  <groupId>org.springframework.cloud</groupId>
                                  <artifactId>spring-cloud-dataflow-build</artifactId>
                                  <version>2.10.0-SNAPSHOT</version>
                                  <versioning>
                                    <snapshot>
                                      <timestamp>20220201.001946</timestamp>
                                      <buildNumber>85</buildNumber>
                                    </snapshot>
                                    <lastUpdated>20220201001950</lastUpdated>
                                    <snapshotVersions>
                                      <snapshotVersion>
                                        <extension>pom</extension>
                                        <value>2.10.0-20220201.001946-85</value>
                                        <updated>20220201001946</updated>
                                      </snapshotVersion>
                                    </snapshotVersions>
                                  </versioning>
                                </metadata>
                            """
                            )
                        } else if (request.path!!.endsWith(".pom")) {
                            setBody(
                                //language=xml
                                """
                                    <project>
                                        <groupId>org.springframework.cloud</groupId>
                                        <artifactId>spring-cloud-dataflow-build</artifactId>
                                        <version>2.10.0-SNAPSHOT</version>
                                    </project>
                                """
                            )
                        }
                    }
                }
            }
        }

        val metadataPaths = AtomicInteger(0)
        val releaseRepo = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    return MockResponse().apply {
                        if (request.path!!.contains("maven-metadata")) {
                            metadataPaths.incrementAndGet()
                        }
                        setResponseCode(404)
                    }
                }
            }
        }

        releaseRepo.use { releaseRepoServer ->
            releaseRepoServer.start()
            return snapshotRepo.use { snapshotRepoServer ->
                snapshotRepoServer.start()
                MavenParser.builder().build().parse(ctx,
                    """
                        <project>
                            <modelVersion>4.0.0</modelVersion>
                        
                            <groupId>org.openrewrite.test</groupId>
                            <artifactId>foo</artifactId>
                            <version>0.1.0-SNAPSHOT</version>
                            
                            <repositories>
                              <repository>
                                <id>snapshot</id>
                                <snapshots>
                                  <enabled>true</enabled>
                                </snapshots>
                                <url>http://${snapshotRepoServer.hostName}:${snapshotRepoServer.port}</url>
                              </repository>
                              <repository>
                                <id>release</id>
                                <snapshots>
                                  <enabled>false</enabled>
                                </snapshots>
                                <url>http://${releaseRepoServer.hostName}:${releaseRepoServer.port}</url>
                              </repository>
                            </repositories>
                            
                            <dependencies>
                                <dependency>
                                    <groupId>org.springframework.cloud</groupId>
                                    <artifactId>spring-cloud-dataflow-build</artifactId>
                                    <version>2.10.0-SNAPSHOT</version>
                                </dependency>
                            </dependencies>
                        </project>
                    """
                ).first()

                assertThat(snapshotRepoServer.requestCount).isGreaterThan(1)
                assertThat(metadataPaths.get()).isEqualTo(0)
            }
        }
    }
}
