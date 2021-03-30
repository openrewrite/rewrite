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
import org.openrewrite.Issue

class ExcludeDependencyTest : MavenRecipeTest {
    override val recipe = ExcludeDependency("org.junit.vintage","junit-vintage-engine", null)

    @Test
    fun excludeJUnitVintageEngineSpringBoot2_3() = assertChanged(
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>2.3.6.RELEASE</version>
              </parent>
              
              <groupId>com.example</groupId>
              <artifactId>demo</artifactId>
              <version>0.0.1-SNAPSHOT</version>
              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter</artifactId>
                </dependency>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-test</artifactId>
                  <scope>test</scope>
                </dependency>
              </dependencies>
            </project>
        """,
        after = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>2.3.6.RELEASE</version>
              </parent>
              
              <groupId>com.example</groupId>
              <artifactId>demo</artifactId>
              <version>0.0.1-SNAPSHOT</version>
              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter</artifactId>
                </dependency>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-test</artifactId>
                  <scope>test</scope>
                  <exclusions>
                    <exclusion>
                      <groupId>org.junit.vintage</groupId>
                      <artifactId>junit-vintage-engine</artifactId>
                    </exclusion>
                  </exclusions>
                </dependency>
              </dependencies>
            </project>
        """
    )

    @Test
    fun jUnitVintageEngineDoesntNeedExclusionFromSpringBoot2_4() = assertUnchanged(
        before = """
            <project>
              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>2.4.0</version>
              </parent>
              
              <groupId>com.example</groupId>
              <artifactId>demo</artifactId>
              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-test</artifactId>
                  <scope>test</scope>
                </dependency>
              </dependencies>
            </project>
        """
    )

    @Test
    fun excludeJUnitInCompileScope() = assertChanged(
        recipe = ExcludeDependency("junit","junit", "compile"),
        before = """
            <project>
                <groupId>com.example</groupId>
                <artifactId>demo</artifactId>
                <version>0.0.1-SNAPSHOT</version>
                <dependencies>
                    <dependency>
                        <groupId>org.neo4j</groupId>
                        <artifactId>neo4j-ogm-core</artifactId>
                        <version>3.2.21</version>
                    </dependency>
                </dependencies>
            </project>
        """,
        after = """
            <project>
                <groupId>com.example</groupId>
                <artifactId>demo</artifactId>
                <version>0.0.1-SNAPSHOT</version>
                <dependencies>
                    <dependency>
                        <groupId>org.neo4j</groupId>
                        <artifactId>neo4j-ogm-core</artifactId>
                        <version>3.2.21</version>
                        <exclusions>
                            <exclusion>
                                <groupId>junit</groupId>
                                <artifactId>junit</artifactId>
                            </exclusion>
                        </exclusions>
                    </dependency>
                </dependencies>
            </project>
        """
    )

    @Issue("#92")
    @Test
    fun playsNiceWithAddDependency() = assertChanged(
        recipe = recipe.doNext(
            AddDependency(
                "org.junit.jupiter",
                "junit-jupiter-engine",
                "5.3.0"
            ).withScope("test")
        ),
        before = """
            <project>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>integration-testing</artifactId>
                <version>1.0</version>
            </project>
        """,
        after =  """
            <project>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>integration-testing</artifactId>
                <version>1.0</version>
                <dependencies>
                    <dependency>
                        <groupId>org.junit.jupiter</groupId>
                        <artifactId>junit-jupiter-engine</artifactId>
                        <version>5.3.0</version>
                        <scope>test</scope>
                    </dependency>
                </dependencies>
            </project>
        """
    )

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = ExcludeDependency(null, null, null)
        var valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(2)
        assertThat(valid.failures()[0].property).isEqualTo("artifactId")
        assertThat(valid.failures()[1].property).isEqualTo("groupId")

        recipe = ExcludeDependency(null, "rewrite-maven", null)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("groupId")

        recipe = ExcludeDependency("org.openrewrite", null, null)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("artifactId")

        recipe = ExcludeDependency("org.openrewrite", "rewrite-maven", null)
        valid = recipe.validate()
        assertThat(valid.isValid).isTrue()
    }
}
