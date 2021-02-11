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
import org.junit.jupiter.api.io.TempDir
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.maven.cache.InMemoryMavenPomCache
import org.openrewrite.maven.tree.Maven
import org.openrewrite.maven.tree.Pom
import org.openrewrite.maven.tree.Scope
import java.nio.file.Path

class MavenLicenseParsingIntegTest {
    companion object {
        private val mavenCache = InMemoryMavenPomCache()
    }

    @Test
    fun springCloudStarterSecurity(@TempDir tempDir: Path) {
        assertLicensesRecognized(tempDir,
                singleDependencyPom("org.springframework.cloud:spring-cloud-starter-security:2.2.4.RELEASE"),
                "Bouncy Castle Licence")
    }

    @Test
    fun dearGoogleWhyDoesYourNameAppearWhenIHaveProblemsSoMuch(@TempDir tempDir: Path) {
        assertLicensesRecognized(tempDir,
                singleDependencyPom("com.google.cloud:google-cloud-shared-config:0.9.2"))
    }

    @Test
    fun springWeb(@TempDir tempDir: Path) {
        assertLicensesRecognized(tempDir, singleDependencyPom("org.springframework:spring-web:4.0.9.RELEASE"),
                "Bouncy Castle Licence")
    }

    @Test
    fun springWebMvc(@TempDir tempDir: Path) {
        assertLicensesRecognized(tempDir, singleDependencyPom("org.springframework:spring-webmvc:4.3.6.RELEASE"))
    }

    @Test
    fun springBootStarterActuator(@TempDir tempDir: Path) {
        assertLicensesRecognized(tempDir, singleDependencyPom("org.springframework.boot:spring-boot-starter-actuator:2.3.2.RELEASE"),
                "Bouncy Castle Licence",
                "Day Specification License, Day Specification License addendum")
    }

    private fun assertLicensesRecognized(tempDir: Path, pom: String, vararg exceptions: String) {
        val pomFile = tempDir.resolve("pom.xml").toFile().apply { writeText(pom) }

        val pomAst: Maven = MavenParser.builder()
                .pomCache(mavenCache)
                .resolveOptional(false)
                .build()
                .parse(listOf(pomFile.toPath()), null, InMemoryExecutionContext())
                .first()

        val unknownLicenses = pomAst.model.getDependencies(Scope.Test)
                .filter { it.model.licenses.any { l -> l.type == Pom.LicenseType.Unknown && !exceptions.contains(l.name) } }
                .map {
                    "${it.model.groupId}:${it.model.artifactId}:${it.model.version} contains unknown licenses " +
                            "[${
                                it.model.licenses.filter { l -> l.type == Pom.LicenseType.Unknown }
                                        .joinToString { l -> l.name }
                            }]"
                }

        assertThat(unknownLicenses).isEmpty()
    }
}
