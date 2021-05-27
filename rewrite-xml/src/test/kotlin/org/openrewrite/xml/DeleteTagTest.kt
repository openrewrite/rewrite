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
package org.openrewrite.xml

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe

/**
 * Hypothetical + demonstration
 *
 * todo
 */
class DeleteTagTest : XmlRecipeTest {
    override val recipe: Recipe
        get() = DeleteTag("/")

    @Test
    @Issue("https://github.com/openrewrite/rewrite-quarkus/issues/3")
    fun removePluginGoalConfigurationByPath() = assertChanged(
        recipe = DeleteTag("/project/*/plugins/plugin[artifactId=\"quarkus-maven-plugin\"]/executions/goals/goal[. = \"native-image\"]"),
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>

              <groupId>org.openrewrite.example</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>

              <properties>
                <quarkus-plugin.version>1.11.1.Final</quarkus-plugin.version>
              </properties>

              <build>
                <plugins>
                  <plugin>
                    <groupId>io.quarkus</groupId>
                    <artifactId>quarkus-maven-plugin</artifactId>
                    <version>${"$"}{quarkus-plugin.version}</version>
                    <extensions>true</extensions>
                    <executions>
                      <execution>
                        <goals>
                          <goal>build</goal>
                          <goal>native-image</goal>
                        </goals>
                      </execution>
                    </executions>
                  </plugin>
                </plugins>
              </build>
            </project>
        """,
        after = """
            <project>
              <modelVersion>4.0.0</modelVersion>

              <groupId>org.openrewrite.example</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>

              <properties>
                <quarkus-plugin.version>1.11.1.Final</quarkus-plugin.version>
              </properties>

              <build>
                <plugins>
                  <plugin>
                    <groupId>io.quarkus</groupId>
                    <artifactId>quarkus-maven-plugin</artifactId>
                    <version>${"$"}{quarkus-plugin.version}</version>
                    <executions>
                      <execution>
                        <goals>
                          <goal>build</goal>
                        </goals>
                      </execution>
                    </executions>
                  </plugin>
                </plugins>
              </build>
            </project>
        """
    )

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = DeleteTag(null)
        var valid = recipe.validate()
        assertThat(valid.isValid).isFalse
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("xpathExpression")

        recipe = DeleteTag("/")
        valid = recipe.validate()
        assertThat(valid.isValid).isTrue
    }
}
