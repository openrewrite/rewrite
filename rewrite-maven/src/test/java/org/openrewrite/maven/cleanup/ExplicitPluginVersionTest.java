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
package org.openrewrite.maven.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.regex.Pattern;

import static org.openrewrite.maven.Assertions.pomXml;

class ExplicitPluginVersionTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ExplicitPluginVersion());
    }

    @Test
    @DocumentExample
    @Issue("https://github.com/openrewrite/rewrite/issues/2735")
    void shouldAddLatest() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-compiler-plugin</artifactId>
                      <configuration>
                        <source>1.8</source>
                        <target>1.8</target>
                      </configuration>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """,
            spec -> spec.after(actual -> """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-compiler-plugin</artifactId>
                      <version>%s</version>
                      <configuration>
                        <source>1.8</source>
                        <target>1.8</target>
                      </configuration>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """.formatted(
              Pattern.compile("<version>(3\\.\\d+\\.\\d+)</version>")
                .matcher(actual).results().reduce((a, b) -> b).map(m -> m.group(1)).get()))
          )
        );
    }

    @Test
    void shouldNotUpgradeExisting() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-compiler-plugin</artifactId>
                      <!-- version already present, not upgraded -->
                      <version>3.8.0</version>
                      <configuration>
                        <source>1.8</source>
                        <target>1.8</target>
                      </configuration>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """
          )
        );
    }

    @Test
    void shouldNotOverrideParentPluginVersion() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <parent>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-parent</artifactId>
                      <version>2.5.4</version>
                  </parent>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.springframework.boot</groupId>
                              <artifactId>spring-boot-maven-plugin</artifactId>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """
          )
        );
    }
}