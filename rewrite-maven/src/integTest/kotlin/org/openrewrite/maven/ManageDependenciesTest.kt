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

import org.junit.jupiter.api.Test
import org.openrewrite.*
import org.openrewrite.maven.cache.InMemoryCache
import org.openrewrite.maven.tree.Maven

class ManageDependenciesTest : RecipeTest {
    companion object {
        val mavenCache = InMemoryCache()
    }

    override val parser: Parser<Maven> = MavenParser.builder()
        .resolveOptional(false)
        .cache(mavenCache)
        .build()

    @Test
    fun createDependencyManagementWithDependencyWhenNoneExists() = assertChanged(
        recipe = ManageDependencies().apply {
            setGroupPattern("org.junit.jupiter")
        },
        before = """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                    <dependency>
                        <groupId>org.junit.jupiter</groupId>
                        <artifactId>junit-jupiter-api</artifactId>
                        <version>5.6.2</version>
                        <scope>test</scope>
                    </dependency>
                </dependencies>
            </project>
        """,
        after = """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>org.junit.jupiter</groupId>
                            <artifactId>junit-jupiter-api</artifactId>
                            <version>5.6.2</version>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
                <dependencies>
                    <dependency>
                        <groupId>org.junit.jupiter</groupId>
                        <artifactId>junit-jupiter-api</artifactId>
                        <scope>test</scope>
                    </dependency>
                </dependencies>
            </project>
        """
    )

    @Test
    fun deferToDependencyManagementWhenDependencyIsAlreadyManaged() = assertChanged(
        recipe = ManageDependencies().apply {
            setGroupPattern("org.junit.jupiter")
        },
        before = """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>2.4.0</version>
                </parent>
                <dependencies>
                    <dependency>
                        <groupId>org.junit.jupiter</groupId>
                        <artifactId>junit-jupiter-api</artifactId>
                        <version>5.6.2</version>
                        <scope>test</scope>
                    </dependency>
                </dependencies>
            </project>
        """,
        after = """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>2.4.0</version>
                </parent>
                <dependencies>
                    <dependency>
                        <groupId>org.junit.jupiter</groupId>
                        <artifactId>junit-jupiter-api</artifactId>
                        <scope>test</scope>
                    </dependency>
                </dependencies>
            </project>
        """
    )

    @Test
    fun updateVersionIfDifferent() = assertChanged(
        recipe = ManageDependencies().apply {
            setGroupPattern("org.junit.jupiter")
            setArtifactPattern("junit-jupiter-api")
            setVersion("10.100")
        },
        before = """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>org.junit.jupiter</groupId>
                            <artifactId>junit-jupiter-api</artifactId>
                            <version>5.6.2</version>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
            </project>
        """,
        after = """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>org.junit.jupiter</groupId>
                            <artifactId>junit-jupiter-api</artifactId>
                            <version>10.100</version>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
            </project>
        """
    )
}
