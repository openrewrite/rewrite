/*
 * Copyright 2026 the original author or authors.
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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class AddSourceEncodingPropertyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResource("/META-INF/rewrite/maven.yml",
                "org.openrewrite.maven.cleanup.AddSourceEncodingProperty");
    }

    @DocumentExample
    @Test
    void addsBothEncodingProperties() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <properties>
                  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                  <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
                </properties>
              </project>
              """
          )
        );
    }

    @Test
    void preservesExistingNonUtf8Value() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <properties>
                  <project.build.sourceEncoding>ISO-8859-1</project.build.sourceEncoding>
                </properties>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <properties>
                  <project.build.sourceEncoding>ISO-8859-1</project.build.sourceEncoding>
                  <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
                </properties>
              </project>
              """
          )
        );
    }

    @Test
    void noopWhenAlreadyConfigured() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <properties>
                  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                  <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
                </properties>
              </project>
              """
          )
        );
    }
}
