/*
 * Copyright 2021 the original author or authors.
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
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class ManagedDependencyRequiresVersionTest implements RewriteTest {
    @Issue("https://github.com/openrewrite/rewrite/issues/1084")
    @Test
    void dependencyManagementDependencyRequiresVersion() {
        rewriteRun(
          spec -> spec.recipe(new DependencyManagementDependencyRequiresVersion()),
          pomXml(
            """
              <project>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0-SNAPSHOT</version>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.fasterxml.jackson.core</groupId>
                      <artifactId>jackson-core</artifactId>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """,
            """
              <project>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0-SNAPSHOT</version>
              </project>
              """
          )
        );
    }
}
