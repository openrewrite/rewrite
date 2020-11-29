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
import org.openrewrite.RefactorVisitorTestForParser
import org.openrewrite.maven.cache.InMemoryCache
import org.openrewrite.maven.tree.Maven

class AddDependencyTest : RefactorVisitorTestForParser<Maven> {
    companion object {
        private val mavenCache = InMemoryCache()
    }

    override val parser: MavenParser = MavenParser.builder()
            .resolveOptional(false)
            .cache(mavenCache)
            .build()

    private val addDependency = AddDependency().apply {
        setGroupId("org.springframework.boot")
        setArtifactId("spring-boot")
        setVersion("1.5.22.RELEASE")
        setSkipIfPresent(false)
    }

    @Test
    fun addToExistingDependencies() = assertRefactored(
            visitors = listOf(addDependency),
            before = """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <dependencies>
                    <dependency>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-actuator</artifactId>
                      <version>1.5.22.RELEASE</version>
                    </dependency>
                  </dependencies>
                </project>
            """,
            after = """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <dependencies>
                    <dependency>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot</artifactId>
                      <version>1.5.22.RELEASE</version>
                    </dependency>
                    <dependency>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-actuator</artifactId>
                      <version>1.5.22.RELEASE</version>
                    </dependency>
                  </dependencies>
                </project>
            """
    )

    @Test
    fun addWhenNoDependencies() = assertRefactored(
            visitors = listOf(addDependency),
            before = """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                </project>
            """,
            after = """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencies>
                    <dependency>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot</artifactId>
                      <version>1.5.22.RELEASE</version>
                    </dependency>
                  </dependencies>
                </project>
            """
    )

    @Test
    fun addBySemver() = assertRefactored(
            visitors = listOf(addDependency.apply {
                setVersion("1.4.X")
            }),
            before = """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                </project>
            """,
            after = """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencies>
                    <dependency>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot</artifactId>
                      <version>1.4.7.RELEASE</version>
                    </dependency>
                  </dependencies>
                </project>
            """
    )

    @Test
    fun addTestDependenciesAfterCompile() = assertRefactored(
            visitors = listOf(AddDependency().apply {
                setGroupId("org.junit.jupiter")
                setArtifactId("junit-jupiter-api")
                setVersion("5.7.0")
                setScope("test")
                setSkipIfPresent(false)
            }),
            before = """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <dependencies>
                    <dependency>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot</artifactId>
                      <version>1.5.22.RELEASE</version>
                    </dependency>
                  </dependencies>
                </project>
            """,
            after = """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <dependencies>
                    <dependency>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot</artifactId>
                      <version>1.5.22.RELEASE</version>
                    </dependency>
                    <dependency>
                      <groupId>org.junit.jupiter</groupId>
                      <artifactId>junit-jupiter-api</artifactId>
                      <version>5.7.0</version>
                      <scope>test</scope>
                    </dependency>
                  </dependencies>
                </project>
            """
    )

    @Test
    fun maybeAddDependencyDoesntAddWhenExistingDependency() = assertUnchanged(
            visitors = listOf(addDependency.apply { setSkipIfPresent(true) }),
            before = """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencies>
                    <dependency>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot</artifactId>
                      <version>1.5.22.RELEASE</version>
                    </dependency>
                  </dependencies>
                </project>
            """
    )

    @Test
    fun maybeAddDependencyDoesntAddWhenExistingAsTransitiveDependency() = assertUnchanged(
            visitors = listOf(addDependency.apply { setSkipIfPresent(true) }),
            before = """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencies>
                    <dependency>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-actuator</artifactId>
                      <version>1.5.22.RELEASE</version>
                    </dependency>
                  </dependencies>
                </project>
            """
    )

    @Test
    fun useManagedDependency() = assertRefactored(
            visitors = listOf(AddDependency().apply {
                setGroupId("com.fasterxml.jackson.core")
                setArtifactId("jackson-databind")
                setVersion("2.12.0-rc2") // will defer instead to dependency management
            }),
            before = """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>com.fasterxml.jackson.core</groupId>
                        <artifactId>jackson-databind</artifactId>
                        <version>latest.release</version>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                </project>
            """,
            after = """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>com.fasterxml.jackson.core</groupId>
                        <artifactId>jackson-databind</artifactId>
                        <version>latest.release</version>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.fasterxml.jackson.core</groupId>
                      <artifactId>jackson-databind</artifactId>
                    </dependency>
                  </dependencies>
                </project>
            """
    )

    @Test
    fun useRequestedVersionInUseByOtherMembersOfTheFamily() = assertRefactored(
            visitors = listOf(AddDependency().apply {
                setGroupId("com.fasterxml.jackson.core")
                setArtifactId("jackson-databind")
                setVersion("2.12.0-rc2") // will be overridden by family alignment
                setFamilyPattern("com.fasterxml.jackson*")
                setSkipIfPresent(false)
            }),
            before = """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <properties>
                    <jackson.version>latest.release</jackson.version>
                  </properties>
                  
                  <dependencies>
                    <dependency>
                      <groupId>com.fasterxml.jackson.module</groupId>
                      <artifactId>jackson-module-afterburner</artifactId>
                      <version>${'$'}{jackson.version}</version>
                    </dependency>
                  </dependencies>
                </project>
            """,
            after = """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <properties>
                    <jackson.version>latest.release</jackson.version>
                  </properties>
                  
                  <dependencies>
                    <dependency>
                      <groupId>com.fasterxml.jackson.core</groupId>
                      <artifactId>jackson-databind</artifactId>
                      <version>${'$'}{jackson.version}</version>
                    </dependency>
                    <dependency>
                      <groupId>com.fasterxml.jackson.module</groupId>
                      <artifactId>jackson-module-afterburner</artifactId>
                      <version>${'$'}{jackson.version}</version>
                    </dependency>
                  </dependencies>
                </project>
            """
    )
}
