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
package org.openrewrite.maven.search

import org.junit.jupiter.api.Test
import org.openrewrite.maven.MavenRecipeTest

class FindPluginTest : MavenRecipeTest {

    @Test
    fun findProperty() = assertChanged(
        recipe = FindPlugin("org.openrewrite.maven", "rewrite-maven-plugin"),
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>

              <groupId>org.openrewrite.example</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>

              <build>
                <plugins>
                  <plugin>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>rewrite-maven-plugin</artifactId>
                    <version>4.0.0</version>
                  </plugin>
                </plugins>
              </build>

              <reporting>
                <plugins>
                  <plugin>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>rewrite-maven-plugin</artifactId>
                    <version>4.0.0</version>
                  </plugin>
                </plugins>
              </reporting>
            </project>
        """,
        after = """
            <project>
              <modelVersion>4.0.0</modelVersion>

              <groupId>org.openrewrite.example</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>

              <build>
                <plugins>
                  <!--~~>--><plugin>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>rewrite-maven-plugin</artifactId>
                    <version>4.0.0</version>
                  </plugin>
                </plugins>
              </build>

              <reporting>
                <plugins>
                  <!--~~>--><plugin>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>rewrite-maven-plugin</artifactId>
                    <version>4.0.0</version>
                  </plugin>
                </plugins>
              </reporting>
            </project>
        """
    )
}
