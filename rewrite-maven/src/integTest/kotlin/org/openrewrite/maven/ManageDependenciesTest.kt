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
import org.openrewrite.maven.cache.InMemoryMavenPomCache

class ManageDependenciesTest : MavenRecipeTest {
    companion object {
        val mavenCache = InMemoryMavenPomCache()
    }

    override val parser: MavenParser = MavenParser.builder()
        .cache(mavenCache)
        .build()

    @Test
    fun createDependencyManagementWithDependencyWhenNoneExists() = assertChanged(
        recipe = ManageDependencies(
            "org.junit.jupiter",
            null,
            null),
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
        recipe = ManageDependencies(
            "org.junit.jupiter",
            null,
            null),
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
        recipe = ManageDependencies(
            "org.junit.jupiter",
            "junit-jupiter-api",
            "10.100"),
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

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = ManageDependencies(null, null, null)
        var valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("groupPattern")

        recipe = ManageDependencies("org.openrewrite", "rewrite-maven", "7.0.0")
        valid = recipe.validate()
        assertThat(valid.isValid).isTrue()
    }
}
