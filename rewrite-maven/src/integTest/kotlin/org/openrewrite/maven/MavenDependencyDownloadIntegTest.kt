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
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Parser
import org.openrewrite.maven.tree.Maven
import org.openrewrite.maven.tree.Scope
import java.nio.file.Paths

class MavenDependencyDownloadIntegTest {
    @Test
    fun springWebMvc() {
        val maven: Maven = MavenParser.builder()
                .resolveOptional(false)
                .build()
                .parse(singleDependencyPom("org.springframework:spring-webmvc:4.3.6.RELEASE"))
                .first()

        val compileDependencies = maven.model.getDependencies(Scope.Compile)

        compileDependencies.forEach { dep ->
            println("${dep.repository} ${dep.coordinates}")
        }
    }

    @Test
    fun rewriteCore() {
        val maven: Maven = MavenParser.builder()
                .resolveOptional(false)
                .build()
                .parse(singleDependencyPom("org.openrewrite:rewrite-core:6.0.1"))
                .first()

        val compileDependencies = maven.model.getDependencies(Scope.Runtime)
                .sortedBy { d -> d.coordinates }

        compileDependencies.forEach { dep ->
            println("${dep.repository} ${dep.coordinates}")
        }
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/135")
    @Test
    fun selfRecursiveParent() {
        MavenParser.builder()
            .resolveOptional(false)
            .build()
            .parse("""
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
            """)
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/135")
    @Test
    fun selfRecursiveDependency() {
        val maven = MavenParser.builder()
            .resolveOptional(false)
            .build()
            .parse("""
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
            """)
            .first()
        // Maven itself would respond to this pom with a fatal error.
        // So long as we don't produce an AST with cycles it's OK
        assertThat(maven.model.dependencies)
            .hasSize(1)
        assertThat(maven.model.dependencies.first().model.dependencies)
            .hasSize(0)
    }

    @Disabled("This requires a full instance of artifactory with particular configuration to be running locally")
    @Test
    fun withAuth() {
        // Don't worry about the credentials stored in here being useful for any other purpose or worth protecting
        val maven: Maven = MavenParser.builder()
            .mavenSettings(Parser.Input(Paths.get("settings.xml")) {
                """
                <settings>
                    <mirrors>
                        <mirror>
                            <mirrorOf>*</mirrorOf>
                            <name>repo</name>
                            <url>http://localhost:8081/artifactory/jcenter-authenticated/</url>
                            <id>repo</id>
                        </mirror>
                    </mirrors>
                    <servers>
                        <server>
                            <id>repo</id>
                            <username>admin</username>
                            <password>E0Sl0n85N0DK</password>
                        </server>
                    </servers>
                </settings>
                """.trimIndent().byteInputStream()
            })
            .build()
            .parse(
                """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                
                    <groupId>org.openrewrite.test</groupId>
                    <artifactId>foo</artifactId>
                    <version>0.1.0-SNAPSHOT</version>
                
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>29.0-jre</version>
                        </dependency>
                    </dependencies>
                </project>
                """)
            .first()

        assertThat(maven.model.dependencies)
            .hasSize(1)
            .matches { it.first().artifactId == "guava" }
    }
}
