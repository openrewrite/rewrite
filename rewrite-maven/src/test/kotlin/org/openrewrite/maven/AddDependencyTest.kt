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
import org.openrewrite.maven.tree.Maven

class AddDependencyTest : MavenRecipeTest {
    override val recipe: AddDependency
        get() = AddDependency(
            "org.springframework.boot",
            "spring-boot",
            "1.5.22.RELEASE"
        ).apply {
            isSkipIfPresent = false
        }

    @Test
    fun addToExistingDependencies() = assertChanged(
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
        """,
        afterConditions = { maven: Maven ->
            assertThat(maven.model.findDependencies("org.springframework.boot", "spring-boot")).isNotEmpty()
        }
    )

    @Test
    fun addWhenNoDependencies() = assertChanged(
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
    fun addBySemver() = assertChanged(
        recipe = AddDependency(
            "org.springframework.boot",
            "spring-boot",
            "1.4.X"
        ).apply {
            isSkipIfPresent = false
        },
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
    fun addTestDependenciesAfterCompile() = assertChanged(
        recipe = AddDependency("org.junit.jupiter", "junit-jupiter-api", "5.7.0").apply {
            scope = "test"
            isSkipIfPresent = false
        },
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
        recipe = recipe.apply { isSkipIfPresent = true },
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
        recipe = recipe.apply { isSkipIfPresent = true },
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
    fun useManagedDependency() = assertChanged(
        recipe = AddDependency(
            "com.fasterxml.jackson.core",
            "jackson-databind",
            "2.12.0"
        ),
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
                    <version>2.12.0</version>
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
                    <version>2.12.0</version>
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
    fun useRequestedVersionInUseByOtherMembersOfTheFamily() = assertChanged(
        recipe = AddDependency(
            "com.fasterxml.jackson.core",
            "jackson-databind",
            "2.12.0"
        ).apply {
            familyPattern = "com.fasterxml.jackson*"
            isSkipIfPresent = false
        },
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              
              <properties>
                <jackson.version>2.12.0</jackson.version>
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
                <jackson.version>2.12.0</jackson.version>
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
        """,
        afterConditions = { maven: Maven ->
            assertThat(maven.model.findDependencies("com.fasterxml.jackson.core", "jackson-databind")).isNotEmpty()
        }
    )

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = AddDependency(null, null, null)
        var valid = recipe.validate()
        assertThat(valid.isValid).isFalse
        assertThat(valid.failures()).hasSize(3)
        assertThat(valid.failures()[0].property).isEqualTo("artifactId")
        assertThat(valid.failures()[1].property).isEqualTo("groupId")
        assertThat(valid.failures()[2].property).isEqualTo("version")

        recipe = AddDependency(null, "rewrite-maven", null)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse
        assertThat(valid.failures()).hasSize(2)
        assertThat(valid.failures()[0].property).isEqualTo("groupId")
        assertThat(valid.failures()[1].property).isEqualTo("version")

        recipe = AddDependency("org.openrewrite", null, null)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse
        assertThat(valid.failures()).hasSize(2)
        assertThat(valid.failures()[0].property).isEqualTo("artifactId")
        assertThat(valid.failures()[1].property).isEqualTo("version")

        recipe = AddDependency("org.openrewrite", "rewrite-maven", "1.0.0")
        valid = recipe.validate()
        assertThat(valid.isValid).isTrue
    }
}
