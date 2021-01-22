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

import com.ctc.wstx.stax.WstxInputFactory
import com.ctc.wstx.stax.WstxOutputFactory
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlFactory
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Condition
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Parser
import java.nio.file.Paths
import javax.xml.stream.XMLInputFactory

class MavenSettingsTest {
    @Test
    fun parse() {
        val settings = MavenSettings.parse(Parser.Input(Paths.get("settings.xml")) {
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
        })

        assertThat(settings.getActiveRepositories(emptyList())).hasSize(1)
    }

    @Disabled
    @Issue("https://github.com/openrewrite/rewrite/issues/131")
    @Test
    fun defaultActiveWhenNoOthersAreActive() {
        val settings = MavenSettings.parse(Parser.Input(Paths.get("settings.xml")) {
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
        })

        assertThat(settings.getActiveRepositories(emptyList()))
            .hasSize(1)
            .allMatch { it.url == "https://activebydefault.com" }
    }

    @Disabled
    @Issue("https://github.com/openrewrite/rewrite/issues/131")
    @Test
    fun defaultOnlyActiveIfNoOthersAreActive() {
        val settings = MavenSettings.parse(Parser.Input(Paths.get("settings.xml")) {
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
                                <url>https://actviebyactivationlist.com</url>
                            </repository>
                        </repositories>
                    </profile>
                </profiles>
            </settings>
            """.trimIndent().byteInputStream()
        })

        assertThat(settings.getActiveRepositories(emptyList()))
            .hasSize(1)
            .allMatch { it.url == "https://actviebyactivationlist.com" }
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/130")
    @Test
    fun mirrorReplacesRepository() {
        val settings = MavenSettings.parse(Parser.Input(Paths.get("settings.xml")) {
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
        })

        assertThat(settings.getActiveRepositories(emptyList()))
            .hasSize(1)
            .allMatch { it.url == "https://internalartifactrepository.yourorg.com" }
    }

    @Test
    fun starredMirrorWithExclusion() {
        val settings = MavenSettings.parse(Parser.Input(Paths.get("settings.xml")) {
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
        })
        assertThat(settings.getActiveRepositories(emptyList()))
            .hasSize(2)
            .haveAtLeastOne(Condition({repo -> repo.url == "https://internalartifactrepository.yourorg.com"}, "Repository should-be-mirrored should have had its URL changed to https://internalartifactrepository.yourorg.com"))
            .haveAtLeastOne(Condition({repo -> repo.url == "https://externalrepository.com" && repo.id == "should-not-be-mirrored"}, "Repository should-not-be-mirrored should have had its URL left unchanged"))
    }

    @Test
    fun serverCredentials() {
        val settings = MavenSettings.parse(Parser.Input(Paths.get("settings.xml")) {
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
        })
        assertThat(settings.servers)
                .isNotNull
        assertThat(settings.servers!!.servers)
                .hasSize(1)
        assertThat(settings.servers!!.servers.first())
                .matches { it.id == "server001" }
                .matches { it.username == "my_login" }
                .matches { it.password == "my_password" }

        // Configuration of this mapper copied from the one internal to MavenSettings
        val input: XMLInputFactory = WstxInputFactory()
        input.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false)
        input.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false)
        val xmlMapper = XmlMapper(XmlFactory(input, WstxOutputFactory()))
                .disable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
                .disable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

        val serversXml = xmlMapper.writeValueAsString(settings.servers)
        assertThat(serversXml)
                .isEqualTo("<Servers><server><id>server001</id></server></Servers>")
                .`as`("To avoid accidental publication of sensitive credentials, username and password should be omitted")
    }
}
