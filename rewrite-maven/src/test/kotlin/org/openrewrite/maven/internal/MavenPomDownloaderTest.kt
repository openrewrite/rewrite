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

import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.maven.MavenParser
import org.openrewrite.maven.tree.MavenRepository
import java.util.concurrent.atomic.AtomicInteger

class MavenPomDownloaderTest {

    @Test
    fun normalizeAccept500() {
        val downloader = MavenPomDownloader(emptyMap(), InMemoryExecutionContext())
        val originalRepo = MavenRepository("id", "https://httpstat.us/500", true, true, false, null, null)
        val normalizedRepo = downloader.normalizeRepository(originalRepo, null)
        assertThat(normalizedRepo).isEqualTo(originalRepo)
    }

    @Test
    fun normalizeAccept404() {
        val downloader = MavenPomDownloader(emptyMap(), InMemoryExecutionContext())
        val originalRepo = MavenRepository("id", "https://httpstat.us/400", true, true, false, null, null)
        val normalizedRepo = downloader.normalizeRepository(originalRepo, null)
        assertThat(normalizedRepo).isEqualTo(originalRepo)
    }

    @Test
    fun normalizeRejectConnectException() {
        val downloader = MavenPomDownloader(emptyMap(), InMemoryExecutionContext())
        val normalizedRepository = downloader.normalizeRepository(
            MavenRepository("id", "https://localhost", true, true, false, null, null),
            null
        )
        assertThat(normalizedRepository).isEqualTo(null)
    }

    @Test
    @Disabled
    fun dontFetchSnapshotsFromReleaseRepos() {
        val snapshotRepo = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    return MockResponse().apply {
                        println("snapshot repo: " + request.path)
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
                        if(request.path!!.contains("maven-metadata")) {
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
                MavenParser.builder().build().parse(
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
