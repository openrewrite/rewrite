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
import org.openrewrite.Recipe

class AddPluginTest : MavenRecipeTest {
    override val recipe: Recipe
        get() = AddPlugin("org.openrewrite.maven", "rewrite-maven-plugin", "100.0", null, null, null)

    @Test
    fun addPluginWithConfiguration() = assertChanged(
        recipe = AddPlugin("org.openrewrite.maven", "rewrite-maven-plugin", "100.0",
            "<configuration>\n<activeRecipes>\n<recipe>io.moderne.FindTest</recipe>\n</activeRecipes>\n</configuration>",
            null, null),
        before = """
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
            </project>
        """,
        after = """
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>rewrite-maven-plugin</artifactId>
                    <version>100.0</version>
                    <configuration>
                      <activeRecipes>
                        <recipe>io.moderne.FindTest</recipe>
                      </activeRecipes>
                    </configuration>
                  </plugin>
                </plugins>
              </build>
            </project>
        """
    )

    @Test
    fun addPluginWithDependencies() = assertChanged(
        recipe = AddPlugin("org.openrewrite.maven", "rewrite-maven-plugin", "100.0", null, """
            <dependencies>
                <dependency>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>java-8-migration</artifactId>
                    <version>1.0.0</version>
                </dependency>
            </dependencies> 
        """.trimIndent(), null),
        before = """
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
            </project>
        """,
        after = """
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>rewrite-maven-plugin</artifactId>
                    <version>100.0</version>
                    <dependencies>
                      <dependency>
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>java-8-migration</artifactId>
                        <version>1.0.0</version>
                      </dependency>
                    </dependencies>
                  </plugin>
                </plugins>
              </build>
            </project>
        """
    )

    @Test
    fun addPluginWithExecutions() = assertChanged(
        recipe = AddPlugin("org.openrewrite.maven", "rewrite-maven-plugin", "100.0", null, null, """
            <executions>
              <execution>
                <id>xjc</id>
                <goals>
                  <goal>xjc</goal>
                </goals>
                <configuration>
                  <sources>
                    <source>/src/main/resources/countries.xsd</source>
                    <source>/src/main/resources/countries1.xsd</source>
                  </sources>
                  <outputDirectory>/src/main/generated-java</outputDirectory>
                </configuration>
              </execution>
            </executions>
        """.trimIndent()),
        before = """
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
            </project>
        """,
        after = """
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>rewrite-maven-plugin</artifactId>
                    <version>100.0</version>
                    <executions>
                      <execution>
                        <id>xjc</id>
                        <goals>
                          <goal>xjc</goal>
                        </goals>
                        <configuration>
                          <sources>
                            <source>/src/main/resources/countries.xsd</source>
                            <source>/src/main/resources/countries1.xsd</source>
                          </sources>
                          <outputDirectory>/src/main/generated-java</outputDirectory>
                        </configuration>
                      </execution>
                    </executions>
                  </plugin>
                </plugins>
              </build>
            </project>
        """
    )

    @Test
    fun addPlugin() = assertChanged(
        before = """
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
            </project>
        """,
        after = """
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>rewrite-maven-plugin</artifactId>
                    <version>100.0</version>
                  </plugin>
                </plugins>
              </build>
            </project>
        """
    )

    @Test
    fun updatePluginVersion() = assertChanged(
        before = """
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>rewrite-maven-plugin</artifactId>
                    <version>99.0</version>
                  </plugin>
                </plugins>
              </build>
            </project>
        """,
        after = """
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>rewrite-maven-plugin</artifactId>
                    <version>100.0</version>
                  </plugin>
                </plugins>
              </build>
            </project>
        """
    )

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = AddPlugin(null, null, null, null, null, null)
        var valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(3)
        assertThat(valid.failures()[0].property).isEqualTo("artifactId")
        assertThat(valid.failures()[1].property).isEqualTo("groupId")
        assertThat(valid.failures()[2].property).isEqualTo("version")

        recipe = AddPlugin(null, "rewrite-maven", null, null, null, null)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(2)
        assertThat(valid.failures()[0].property).isEqualTo("groupId")
        assertThat(valid.failures()[1].property).isEqualTo("version")

        recipe = AddPlugin("org.openrewrite", null, null, null, null, null)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(2)
        assertThat(valid.failures()[0].property).isEqualTo("artifactId")
        assertThat(valid.failures()[1].property).isEqualTo("version")

        recipe = AddPlugin("org.openrewrite", "rewrite-maven", "1.0.0", null, null, null)
        valid = recipe.validate()
        assertThat(valid.isValid).isTrue()
    }
}
