/*
 * Copyright 2021 the original author or authors.
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
import org.assertj.core.api.Condition
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Issue
import org.openrewrite.Parser
import org.openrewrite.maven.tree.MavenRepository
import org.openrewrite.maven.tree.MavenRepositoryMirror
import java.io.File
import java.net.URI
import java.nio.file.Paths

@Suppress("HttpUrlsUsage")
class MavenSettingsTest {
    @Test
    fun parse() {
        val ctx = MavenExecutionContextView(InMemoryExecutionContext())
        ctx.setMavenSettings(MavenSettings.parse(Parser.Input(Paths.get("settings.xml")) {//language=xml
            """
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
            """.trimIndent().byteInputStream()
        }, ctx))

        assertThat(ctx.repositories).hasSize(1)
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/131")
    @Test
    fun defaultActiveWhenNoOthersAreActive() {
        val ctx = MavenExecutionContextView(InMemoryExecutionContext())
        ctx.setMavenSettings(MavenSettings.parse(Parser.Input(Paths.get("settings.xml")) {//language=xml
            """
            <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                <profiles>
                    <profile>
                        <id>default</id>
                        <activation>
                            <activeByDefault>true</activeByDefault>
                        </activation>
                        <repositories>
                            <repository>
                                <id>spring-milestones-default</id>
                                <name>Spring Milestones</name>
                                <url>https://activebydefault.com</url>
                            </repository>
                        </repositories>
                    </profile>
                    <profile>
                        <id>repo</id>
                        <repositories>
                            <repository>
                                <id>spring-milestones</id>
                                <name>Spring Milestones</name>
                                <url>https://actviebyactivationlist.com</url>
                            </repository>
                        </repositories>
                    </profile>
                </profiles>
            </settings>
            """.trimIndent().byteInputStream()
        }, ctx))

        assertThat(ctx.repositories.map { it.uri.toString() }).containsExactly("https://activebydefault.com")
    }

    @Disabled
    @Issue("https://github.com/openrewrite/rewrite/issues/131")
    @Test
    fun defaultOnlyActiveIfNoOthersAreActive() {
        val ctx = MavenExecutionContextView(InMemoryExecutionContext())
        ctx.setMavenSettings(MavenSettings.parse(Parser.Input(Paths.get("settings.xml")) {//language=xml
            """
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
                        <id>default</id>
                        <activation>
                            <activeByDefault>true</activeByDefault>
                        </activation>
                        <repositories>
                            <repository>
                                <id>spring-milestones-default</id>
                                <name>Spring Milestones</name>
                                <url>https://activebydefault.com</url>
                            </repository>
                        </repositories>
                    </profile>
                    <profile>
                        <id>repo</id>
                        <repositories>
                            <repository>
                                <id>spring-milestones</id>
                                <name>Spring Milestones</name>
                                <url>https://activebyactivationlist.com</url>
                            </repository>
                        </repositories>
                    </profile>
                </profiles>
            </settings>
            """.trimIndent().byteInputStream()
        }, ctx))

        assertThat(ctx.repositories.map { it.uri.toString() }).containsExactly("https://activebyactivationlist.com")
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/130")
    @Test
    fun mirrorReplacesRepository() {
        val ctx = MavenExecutionContextView(InMemoryExecutionContext())
        ctx.setMavenSettings(MavenSettings.parse(Parser.Input(Paths.get("settings.xml")) {//language=xml
            """
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
                                <url>https://externalrepository.com</url>
                            </repository>
                        </repositories>
                    </profile>
                </profiles>
                <mirrors>
                    <mirror>
                        <mirrorOf>*</mirrorOf>
                        <name>repo</name>
                        <url>https://internalartifactrepository.yourorg.com</url>
                        <id>repo</id>
                    </mirror>
                </mirrors>
            </settings>
            """.trimIndent().byteInputStream()
        }, ctx))

        assertThat(ctx.repositories
            .map { MavenRepositoryMirror.apply(ctx.mirrors, it) }
            .map { it.uri.toString() }).containsExactly("https://internalartifactrepository.yourorg.com")
    }

    @Test
    fun starredMirrorWithExclusion() {
        val ctx = MavenExecutionContextView(InMemoryExecutionContext())
        ctx.setMavenSettings(MavenSettings.parse(Parser.Input(Paths.get("settings.xml")) {//language=xml
            """
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
                                <id>should-be-mirrored</id>
                                <url>https://externalrepository.com</url>
                            </repository>
                            <repository>
                                <id>should-not-be-mirrored</id>
                                <url>https://externalrepository.com</url>
                            </repository>
                        </repositories>
                    </profile>
                </profiles>
                <mirrors>
                    <mirror>
                        <mirrorOf>*,!should-not-be-mirrored</mirrorOf>
                        <name>repo</name>
                        <url>https://internalartifactrepository.yourorg.com</url>
                        <id>repo</id>
                    </mirror>
                </mirrors>
            </settings>
            """.trimIndent().byteInputStream()
        }, ctx))

        assertThat(ctx.repositories.map { MavenRepositoryMirror.apply(ctx.mirrors, it) })
            .hasSize(2)
            .haveAtLeastOne(
                Condition(
                    { repo -> repo.uri.toString() == "https://internalartifactrepository.yourorg.com" },
                    "Repository should-be-mirrored should have had its URL changed to https://internalartifactrepository.yourorg.com"
                )
            )
            .haveAtLeastOne(
                Condition(
                    { repo -> repo.uri.toString() == "https://externalrepository.com" && repo.id == "should-not-be-mirrored" },
                    "Repository should-not-be-mirrored should have had its URL left unchanged"
                )
            )
    }

    @Test
    fun serverCredentials() {
        val ctx = MavenExecutionContextView(InMemoryExecutionContext())
        val settings = MavenSettings.parse(Parser.Input(Paths.get("settings.xml")) {//language=xml
            """
            <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                  <servers>
                    <server>
                      <id>server001</id>
                      <username>my_login</username>
                      <password>my_password</password>
                    </server>
                  </servers>
            </settings>
            """.trimIndent().byteInputStream()
        }, ctx)

        assertThat(settings!!.servers).isNotNull
        assertThat(settings.servers!!.servers).hasSize(1)
        assertThat(settings.servers!!.servers.first())
            .matches { it.id == "server001" }
            .matches { it.username == "my_login" }
            .matches { it.password == "my_password" }
    }

    @Nested
    @Issue("https://github.com/openrewrite/rewrite/issues/1688")
    inner class LocalRepository {
        @Test
        fun `parses localRepository path from settings xml`() {
            val localRepoPath = System.getProperty("java.io.tmpdir")
            val ctx = MavenExecutionContextView(InMemoryExecutionContext())
            val settings = MavenSettings.parse(Parser.Input(Paths.get("settings.xml")) {//language=xml
                """
            <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                  <localRepository>$localRepoPath</localRepository>
            </settings>
            """.trimIndent().byteInputStream()
            }, ctx)!!

            assertThat(settings.mavenLocal.uri).startsWith("file://").containsSubsequence(localRepoPath.split(File.separator))
            assertThat(ctx.localRepository).isSameAs(settings.mavenLocal)
        }

        @Test
        fun `parses localRepository uri from settings xml`() {
            val localRepoPath = Paths.get(System.getProperty("java.io.tmpdir")).toUri().toString()
            val ctx = MavenExecutionContextView(InMemoryExecutionContext())
            val settings = MavenSettings.parse(Parser.Input(Paths.get("settings.xml")) {//language=xml
                """
            <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                  <localRepository>$localRepoPath</localRepository>
            </settings>
            """.trimIndent().byteInputStream()
            }, ctx)

            assertThat(settings!!.mavenLocal.uri).startsWith("file://").containsSubsequence(localRepoPath.split("/"))
            assertThat(ctx.localRepository).isSameAs(settings.mavenLocal)
        }

        @Test
        fun `defaults to the maven default`() {
            val ctx = MavenExecutionContextView(InMemoryExecutionContext())
            val settings = MavenSettings.parse(Parser.Input(Paths.get("settings.xml")) {//language=xml
                """
            <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
            </settings>
            """.trimIndent().byteInputStream()
            }, ctx)

            assertThat(settings!!.mavenLocal.uri).isEqualTo(MavenRepository.MAVEN_LOCAL_DEFAULT.uri)
            assertThat(ctx.localRepository).isSameAs(settings.mavenLocal)
        }
    }
}
