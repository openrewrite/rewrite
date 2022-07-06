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
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Issue
import org.openrewrite.Parser
import org.openrewrite.maven.tree.MavenRepository
import org.openrewrite.maven.tree.MavenRepositoryMirror
import java.io.File
import java.lang.reflect.Field
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
            ctx.setMavenSettings(MavenSettings.parse(Parser.Input(Paths.get("settings.xml")) {//language=xml
                """
                <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                      <localRepository>$localRepoPath</localRepository>
                </settings>
            """.trimIndent().byteInputStream()
            }, ctx))
            assertThat(ctx.localRepository.uri).startsWith("file://").containsSubsequence(localRepoPath.split(File.separator))
        }

        @Test
        fun `parses localRepository uri from settings xml`() {
            val localRepoPath = Paths.get(System.getProperty("java.io.tmpdir")).toUri().toString()
            val ctx = MavenExecutionContextView(InMemoryExecutionContext())
            ctx.setMavenSettings(MavenSettings.parse(Parser.Input(Paths.get("settings.xml")) {//language=xml
                """
                <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                      <localRepository>$localRepoPath</localRepository>
                </settings>
            """.trimIndent().byteInputStream()
            }, ctx))

            assertThat(ctx.localRepository.uri).startsWith("file://").containsSubsequence(localRepoPath.split("/"))
        }

        @Test
        fun `defaults to the maven default`() {
            val ctx = MavenExecutionContextView(InMemoryExecutionContext())
            ctx.setMavenSettings(MavenSettings.parse(Parser.Input(Paths.get("settings.xml")) {//language=xml
                """
                <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                </settings>
            """.trimIndent().byteInputStream()
            }, ctx))

            assertThat(ctx.localRepository.uri).isEqualTo(MavenRepository.MAVEN_LOCAL_DEFAULT.uri)
        }
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1801")
    @Nested
    inner class Interpolation {
        @Test
        fun properties() {
            System.setProperty("rewrite.test.custom.location", "/tmp")
            val settings = MavenSettings.parse(Parser.Input(Paths.get("settings.xml")) {//language=xml
                """
            <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                <servers>
                     <server>
                         <id>private-repo</id>
                         <username>my_username</username>
                         <password>my_pass</password>
                    </server>
                </servers>
                <localRepository>${'$'}{rewrite.test.custom.location}/maven/local/repository/</localRepository>
                <activeProfiles>
                    <activeProfile>
                        my-profile
                    </activeProfile>
                </activeProfiles>
                <profiles>
                    <profile>
                        <id>my-profile</id>
                        <repositories>
                            <repository>
                                <id>private-repo</id>
                                <name>Private Repo</name>
                                <url>https://repo.company.net/maven</url>
                            </repository>
                        </repositories>
                    </profile>
                </profiles>
            </settings>
            """.trimIndent().byteInputStream()
            }, InMemoryExecutionContext())

            assertThat(settings!!.localRepository).isEqualTo("/tmp/maven/local/repository/")
        }

        @Test
        fun `unresolved placeholders remain unchanged`() {
            val settings = MavenSettings.parse(Parser.Input(Paths.get("settings.xml")) {//language=xml
                """
            <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                <servers>
                     <server>
                         <id>private-repo</id>
                         <username>${'$'}{env.PRIVATE_REPO_USERNAME_ZZ}</username>
                         <password>${'$'}{env.PRIVATE_REPO_PASSWORD_ZZ}</password>
                    </server>
                </servers>
                <localRepository>${'$'}{custom.location.zz}/maven/local/repository/</localRepository>
                <activeProfiles>
                    <activeProfile>
                        my-profile
                    </activeProfile>
                </activeProfiles>
                <profiles>
                    <profile>
                        <id>my-profile</id>
                        <repositories>
                            <repository>
                                <id>private-repo</id>
                                <name>Private Repo</name>
                                <url>https://repo.company.net/maven</url>
                            </repository>
                        </repositories>
                    </profile>
                </profiles>
            </settings>
            """.trimIndent().byteInputStream()
            }, InMemoryExecutionContext())

            assertThat(settings!!.localRepository).isEqualTo("\${custom.location.zz}/maven/local/repository/")
            assertThat(settings.servers!!.servers.first().username).isEqualTo("\${env.PRIVATE_REPO_USERNAME_ZZ}")
            assertThat(settings.servers!!.servers.first().password).isEqualTo("\${env.PRIVATE_REPO_PASSWORD_ZZ}")
        }

        @Test
        @Disabled("Depends on methods which are unstable in CI build")
        fun env() {
            updateEnvMap("REWRITE_TEST_PRIVATE_REPO_USERNAME", "user")
            updateEnvMap("REWRITE_TEST_PRIVATE_REPO_PASSWORD", "pass")
            val settings = MavenSettings.parse(Parser.Input(Paths.get("settings.xml")) {//language=xml
                """
            <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                <servers>
                     <server>
                         <id>private-repo</id>
                         <username>${'$'}{env.REWRITE_TEST_PRIVATE_REPO_USERNAME}</username>
                         <password>${'$'}{env.REWRITE_TEST_PRIVATE_REPO_PASSWORD}</password>
                    </server>
                </servers>
                <localRepository>/tmp/maven/local/repository/</localRepository>
                <activeProfiles>
                    <activeProfile>
                        my-profile
                    </activeProfile>
                </activeProfiles>
                <profiles>
                    <profile>
                        <id>my-profile</id>
                        <repositories>
                            <repository>
                                <id>private-repo</id>
                                <name>Private Repo</name>
                                <url>https://repo.company.net/maven</url>
                            </repository>
                        </repositories>
                    </profile>
                </profiles>
            </settings>
            """.trimIndent().byteInputStream()
            }, InMemoryExecutionContext())

            assertThat(settings?.servers!!.servers).hasSize(1)
            assertThat(settings.servers!!.servers.first().username).isEqualTo("user")
            assertThat(settings.servers!!.servers.first().password).isEqualTo("pass")
        }

        /**
         * Unusable with Java 17 (and therefore in CI builds)
         */
        @Suppress("UNCHECKED_CAST")
        @Throws(ReflectiveOperationException::class)
        private fun updateEnvMap(name: String, value: String) {
            val env = System.getenv()
            val field: Field = env.javaClass.getDeclaredField("m")
            field.isAccessible = true
            (field.get(env) as MutableMap<String, String>)[name] = value
        }
    }

    @Nested
    inner class Merging {
        @Language("xml")
        private val installationSettings = """
            <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                <servers>
                     <server>
                         <id>private-repo</id>
                         <username>user</username>
                         <password>secret</password>
                    </server>
                </servers>
                <localRepository>${'$'}{user.home}/maven/local/repository/</localRepository>
                <activeProfiles>
                    <activeProfile>
                        my-profile
                    </activeProfile>
                </activeProfiles>
                <profiles>
                    <profile>
                        <id>my-profile</id>
                        <repositories>
                            <repository>
                                <id>private-repo</id>
                                <name>Private Repo</name>
                                <url>https://repo.company.net/maven</url>
                            </repository>
                        </repositories>
                    </profile>
                </profiles>
                <mirrors>
                    <mirror>
                        <id>planetmirror.com</id>
                        <name>PlanetMirror Australia</name>
                        <url>http://downloads.planetmirror.com/pub/maven2</url>
                        <mirrorOf>central</mirrorOf>
                    </mirror>
                </mirrors>
            </settings>
            """.trimIndent()

        @Test
        fun `concatenates elements with unique ids`() {
            val baseSettings = MavenSettings.parse(Parser.Input(Paths.get("settings.xml")) {
                installationSettings.byteInputStream()
            }, InMemoryExecutionContext())
            val userSettings = MavenSettings.parse(Parser.Input(Paths.get("settings.xml")) { //language=xml
                """
            <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                <servers>
                     <server>
                         <id>private-repo-2</id>
                         <username>user</username>
                         <password>secret</password>
                    </server>
                </servers>
                <activeProfiles>
                    <activeProfile>
                        my-profile-2
                    </activeProfile>
                </activeProfiles>
                <profiles>
                    <profile>
                        <id>my-profile-2</id>
                        <repositories>
                            <repository>
                                <id>private-repo-2</id>
                                <name>Private Repo</name>
                                <url>https://repo.company.net/maven</url>
                                <snapshots>
                                    <enabled>true</enabled>
                                    <updatePolicy>never</updatePolicy>
                                    <checksumPolicy>fail</checksumPolicy>
                                </snapshots>
                            </repository>
                        </repositories>
                    </profile>
                </profiles>
                <mirrors>
                    <mirror>
                        <id>planetmirror.com-2</id>
                        <name>PlanetMirror Australia</name>
                        <url>http://downloads.planetmirror.com/pub/maven2</url>
                        <mirrorOf>central</mirrorOf>
                    </mirror>
                </mirrors>
            </settings>
                """.trimIndent().byteInputStream()
            }, InMemoryExecutionContext())

            val mergedSettings = userSettings!!.merge(baseSettings)

            assertThat(mergedSettings.profiles!!.profiles.size).isEqualTo(2)
            assertThat(mergedSettings.activeProfiles!!.activeProfiles.size).isEqualTo(2)
            assertThat(mergedSettings.mirrors!!.mirrors.size).isEqualTo(2)
            assertThat(mergedSettings.servers!!.servers.size).isEqualTo(2)
        }

        @Test
        fun `replaces elements with matching ids`() {
            val baseSettings = MavenSettings.parse(Parser.Input(Paths.get("settings.xml")) {
                installationSettings.byteInputStream()
            }, InMemoryExecutionContext())
            val userSettings = MavenSettings.parse(Parser.Input(Paths.get("settings.xml")) { //language=xml
                """
            <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                <servers>
                     <server>
                         <id>private-repo</id>
                         <username>foo</username>
                    </server>
                </servers>
                <profiles>
                    <profile>
                        <id>my-profile</id>
                        <repositories>
                            <repository>
                                <id>private-repo</id>
                                <name>Private Repo</name>
                                <url>https://repo.company.net/maven</url>
                            </repository>
                        </repositories>
                    </profile>
                </profiles>
                <mirrors>
                    <mirror>
                        <id>planetmirror.com</id>
                        <name>PlanetMirror Australia</name>
                        <url>http://downloads.planetmirror.com/pub/maven3000</url>
                        <mirrorOf>central</mirrorOf>
                    </mirror>
                </mirrors>
            </settings>
                """.trimIndent().byteInputStream()
            }, InMemoryExecutionContext())

            val mergedSettings = userSettings!!.merge(baseSettings)

            assertThat(mergedSettings.profiles!!.profiles.size).isEqualTo(1)
            assertThat(mergedSettings.profiles!!.profiles.first().repositories!!.repositories.first().snapshots).isNull()
            assertThat(mergedSettings.activeProfiles!!.activeProfiles.size).isEqualTo(1)
            assertThat(mergedSettings.mirrors!!.mirrors.size).isEqualTo(1)
            assertThat(mergedSettings.mirrors!!.mirrors.first().url)
                .isEqualTo("http://downloads.planetmirror.com/pub/maven3000")
            assertThat(mergedSettings.servers!!.servers.size).isEqualTo(1)
            assertThat(mergedSettings.servers!!.servers.first())
                .hasFieldOrPropertyWithValue("username", "foo")
                .hasFieldOrPropertyWithValue("password", null)
        }
    }
}
