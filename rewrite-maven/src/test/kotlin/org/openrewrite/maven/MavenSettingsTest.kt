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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.Parser
import java.nio.file.Paths

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
}
