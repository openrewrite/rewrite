/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class RemoveRepositoryTest implements RewriteTest {

    @Test
    void removeSimpleRepoWithIdAndUrl() {

        rewriteRun(
                spec -> spec.recipe(new RemoveRepository("myRepo", "http://myrepo.maven.com/repo", null, null,
                        null, null, null,
                        null, null, null)),
                pomXml(
                        """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <repositories>
                  <repository>
                    <id>myRepo</id>
                    <url>http://myrepo.maven.com/repo</url>
                  </repository>
                  <repository>
                    <id>identicalRepo</id>
                    <url>http://myrepo.maven.com/repo</url>
                  </repository>
                </repositories>
              </project>
              """,
                        """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <repositories>
                </repositories>
              </project>
              """
                )
        );
    }

    @Test
    void removeSimpleRepoWithUrlOnly() {

        rewriteRun(
                spec -> spec.recipe(new RemoveRepository(null, "http://myrepo.maven.com/repo", null, null,
                        null, null, null,
                        null, null, null)),
                pomXml(
                        """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <repositories>
                  <repository>
                    <id>myRepo</id>
                    <url>http://myrepo.maven.com/repo</url>
                  </repository>
                </repositories>
              </project>
              """,
                        """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <repositories>
                </repositories>
              </project>
              """
                )
        );
    }
}