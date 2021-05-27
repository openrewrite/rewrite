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
class InsertTagTest : XmlRecipeTest {
    override val recipe: Recipe
        get() = InsertTag("/", "")

    @Test
    @Issue("https://github.com/openrewrite/rewrite-quarkus/issues/6")
    fun addPluginConfigurationByPath() = assertChanged(
        recipe = InsertTag(
            "/project/build/plugins/plugin[artifactId=\"quarkus-maven-plugin\"]/executions/execution/goals/goal",
            // or maybe "key" in this case would be "goal"? todo
            "generate-code"
        ),
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>

              <groupId>org.openrewrite.example</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>

              <properties>
                <quarkus-plugin.version>1.13.5.Final</quarkus-plugin.version>
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
                <quarkus-plugin.version>1.13.5.Final</quarkus-plugin.version>
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
                          <goal>generate-code</goal>
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
        var recipe = InsertTag(null, null)
        var valid = recipe.validate()
        assertThat(valid.isValid).isFalse
        assertThat(valid.failures()).hasSize(2)
        assertThat(valid.failures()[0].property).isEqualTo("value")
        assertThat(valid.failures()[1].property).isEqualTo("xpathExpression")

        recipe = InsertTag(null, "someValue")
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("xpathExpression")

        recipe = InsertTag("/", null)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("value")

        recipe = InsertTag("/", "someValue")
        valid = recipe.validate()
        assertThat(valid.isValid).isTrue
    }
}
