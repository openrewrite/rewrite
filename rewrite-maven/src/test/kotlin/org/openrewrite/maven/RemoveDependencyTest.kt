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
import org.openrewrite.maven.Assertions.pomXml
import org.openrewrite.maven.tree.MavenResolutionResult
import org.openrewrite.maven.tree.Scope
import org.openrewrite.test.RewriteTest

class RemoveDependencyTest : MavenRecipeTest, RewriteTest {

    @Test
    fun removeDependency() = rewriteRun(
        { spec ->
            spec.recipe(RemoveDependency("junit","junit", null))
        },
        pomXml(
            """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>29.0-jre</version>
                    </dependency>
                    <dependency>
                      <groupId>junit</groupId>
                      <artifactId>junit</artifactId>
                      <version>4.13.1</version>
                      <scope>test</scope>
                    </dependency>
                  </dependencies>
                </project>
            """,
            """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>29.0-jre</version>
                    </dependency>
                  </dependencies>
                </project>
            """
        )
    )

    @Test
    fun noDependencyToRemove() = rewriteRun(
        { spec ->
            spec.recipe(RemoveDependency("junit","junit", null))
        },
        pomXml(
            """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>29.0-jre</version>
                    </dependency>
                  </dependencies>
                </project>
            """
        )
    )

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = RemoveDependency(null, null, null)
        var valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(2)
        assertThat(valid.failures()[0].property).isEqualTo("artifactId")
        assertThat(valid.failures()[1].property).isEqualTo("groupId")

        recipe = RemoveDependency(null, "rewrite-maven", null)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("groupId")

        recipe = RemoveDependency("org.openrewrite", null, null)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("artifactId")

        recipe = RemoveDependency("org.openrewrite", "rewrite-maven", null)
        valid = recipe.validate()
        assertThat(valid.isValid).isTrue()
    }

    @Test
    fun shouldRemoveScopedDependency() = rewriteRun(
        { spec ->
          spec.recipe(RemoveDependency("org.junit.jupiter","junit-jupiter", "compile"))
        },
        pomXml(
            """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <dependencyManagement>
                        <dependencies>
                           <dependency>
                                <groupId>org.junit.jupiter</groupId>
                                <artifactId>junit-jupiter</artifactId>
                                <version>5.7.1</version>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>org.junit.jupiter</groupId>
                            <artifactId>junit-jupiter</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.junit.jupiter</groupId>
                            <artifactId>junit-jupiter</artifactId>
                            <scope>test</scope>
                        </dependency>
                        <dependency>
                            <groupId>org.junit.jupiter</groupId>
                            <artifactId>junit-jupiter-api</artifactId>
                            <version>5.6.3</version>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>
                </project>
            """,
            """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <dependencyManagement>
                        <dependencies>
                           <dependency>
                                <groupId>org.junit.jupiter</groupId>
                                <artifactId>junit-jupiter</artifactId>
                                <version>5.7.1</version>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>org.junit.jupiter</groupId>
                            <artifactId>junit-jupiter</artifactId>
                            <scope>test</scope>
                        </dependency>
                        <dependency>
                            <groupId>org.junit.jupiter</groupId>
                            <artifactId>junit-jupiter-api</artifactId>
                            <version>5.6.3</version>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>
                </project>
            """
        )
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/422")
    @Test
    fun removeDependencyByEffectiveScope() = rewriteRun(
        { spec ->
            spec.recipe(RemoveDependency("junit", "junit", "runtime"))
        },
        pomXml(
            """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>29.0-jre</version>
                    </dependency>
                    <dependency>
                      <groupId>junit</groupId>
                      <artifactId>junit</artifactId>
                      <version>4.13.1</version>
                    </dependency>
                  </dependencies>
                </project>
            """,
            """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>29.0-jre</version>
                    </dependency>
                  </dependencies>
                </project>
            """
        )
    )

    @Test
    fun updateModelWhenAllDependenciesRemoved() = rewriteRun(
        { spec ->
            spec.recipe(RemoveDependency("com.google.guava", "guava", null))
        },
        pomXml(
            """
                <project>
                   <modelVersion>4.0.0</modelVersion>
                   <groupId>foo</groupId>
                   <artifactId>bar</artifactId>
                   <version>0.0.1-SNAPSHOT</version>
                   <dependencies>
                       <dependency>
                           <groupId>com.google.guava</groupId>
                           <artifactId>guava</artifactId>
                           <version>29.0-jre</version>
                           <type>pom</type>
                       </dependency>
                   </dependencies>
               </project>
            """,
            """
                <project>
                   <modelVersion>4.0.0</modelVersion>
                   <groupId>foo</groupId>
                   <artifactId>bar</artifactId>
                   <version>0.0.1-SNAPSHOT</version>
               </project>
            """
        ) { spec ->
            spec.afterRecipe { doc ->
                val mavenModel = doc.markers.findFirst(MavenResolutionResult::class.java)
                    .orElseThrow { java.lang.IllegalStateException("The maven must should exist on the document.") }
                assertThat(mavenModel.dependencies[Scope.Compile]).isEmpty()
            }
        }
    )
}
