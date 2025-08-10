/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.maven.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class FindScmTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindScm());
    }

    @DocumentExample
    @Test
    void findsScmTag() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>demo</artifactId>
                <version>1.0</version>
                <scm>
                  <url>https://github.com/example/demo</url>
                </scm>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>demo</artifactId>
                <version>1.0</version>
                <!--~~>--><scm>
                  <url>https://github.com/example/demo</url>
                </scm>
              </project>
              """
          )
        );
    }

    @Test
    void doesNotFindScmNotAtRoot() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>demo</artifactId>
                <version>1.0</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.example</groupId>
                      <artifactId>example-plugin</artifactId>
                      <version>1.0</version>
                      <configuration>
                        <scm>github</scm>
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
    void doesNotFindScmTag() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>demo</artifactId>
                <version>1.0</version>
              </project>
              """
          )
        );
    }
}
