/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.recipes;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class UpdateMovedRecipeXmlTest implements RewriteTest {

    @DocumentExample("Update referencing places in pom.xml.")
    @Test
    void changePomXmlConfiguration() {
        rewriteRun(
          spec -> spec.recipe(new UpdateMovedRecipeXml("org.openrewrite.java.cleanup.AddSerialVersionUidToSerializable",
            "org.openrewrite.staticanalysis.AddSerialVersionUidToSerializable")),
          pomXml(
                """
            <project>
              <groupId>org.example</groupId>
              <artifactId>foo</artifactId>
              <version>1.0</version>

              <build>
                <plugins>
                  <plugin>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>rewrite-maven-plugin</artifactId>
                    <version>4.45.0</version>
                    <configuration>
                      <activeRecipes>
                        <recipe>org.openrewrite.java.cleanup.AddSerialVersionUidToSerializable</recipe>
                      </activeRecipes>
                    </configuration>
                  </plugin>
                </plugins>
              </build>
            </project>
            """,
            """
            <project>
              <groupId>org.example</groupId>
              <artifactId>foo</artifactId>
              <version>1.0</version>

              <build>
                <plugins>
                  <plugin>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>rewrite-maven-plugin</artifactId>
                    <version>4.45.0</version>
                    <configuration>
                      <activeRecipes>
                        <recipe>org.openrewrite.staticanalysis.AddSerialVersionUidToSerializable</recipe>
                      </activeRecipes>
                    </configuration>
                  </plugin>
                </plugins>
              </build>
            </project>"""));
    }
}
