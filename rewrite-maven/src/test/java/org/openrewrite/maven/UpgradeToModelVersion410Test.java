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
package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class UpgradeToModelVersion410Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpgradeToModelVersion410());
    }

    @DocumentExample
    @Test
    void upgradeModelVersion() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
              </project>
              """,
            """
              <project>
                  <modelVersion>4.1.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
              </project>
              """
          )
        );
    }

    @Test
    void upgradeModelVersionWithNamespace() {
        rewriteRun(
          pomXml(
            """
              <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
              </project>
              """,
            """
              <project xmlns="http://maven.apache.org/POM/4.1.0">
                  <modelVersion>4.1.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
              </project>
              """
          )
        );
    }

    @Test
    void upgradeModelVersionWithSchemaLocation() {
        rewriteRun(
          pomXml(
            """
              <project xmlns="http://maven.apache.org/POM/4.0.0"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
              </project>
              """,
            """
              <project xmlns="http://maven.apache.org/POM/4.1.0"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.1.0 http://maven.apache.org/xsd/maven-4.1.0.xsd">
                  <modelVersion>4.1.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
              </project>
              """
          )
        );
    }

    @Test
    void upgradeParentPom() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1.0.0</version>
                  <packaging>pom</packaging>
                  <modules>
                      <module>module-a</module>
                  </modules>
              </project>
              """,
            """
              <project>
                  <modelVersion>4.1.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1.0.0</version>
                  <packaging>pom</packaging>
                  <modules>
                      <module>module-a</module>
                  </modules>
              </project>
              """
          )
        );
    }

    @Test
    void noChangeWhenAlreadyVersion410() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.1.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
              </project>
              """
          )
        );
    }

    @Test
    void upgradeWithDependencies() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                      <dependency>
                          <groupId>org.junit.jupiter</groupId>
                          <artifactId>junit-jupiter</artifactId>
                          <version>5.9.0</version>
                          <scope>test</scope>
                      </dependency>
                  </dependencies>
              </project>
              """,
            """
              <project>
                  <modelVersion>4.1.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                      <dependency>
                          <groupId>org.junit.jupiter</groupId>
                          <artifactId>junit-jupiter</artifactId>
                          <version>5.9.0</version>
                          <scope>test</scope>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }
}
