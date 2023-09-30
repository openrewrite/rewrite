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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class PrefixlessExpressionsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResource("/META-INF/rewrite/maven.yml", "org.openrewrite.maven.cleanup.PrefixlessExpressions");
    }

    @DocumentExample
    @Test
    void renamePrefixlessExpressions() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1.0</version>
                <properties>
                  <other.version>1.0.0</other.version>
                </properties>
                <dependencies>
                  <dependency>
                    <groupId>commons-lang</groupId>
                    <artifactId>commons-lang</artifactId>
                    <version>${version}</version>
                  </dependency>
                  <dependency>
                    <groupId>commons-lang</groupId>
                    <artifactId>commons-lang</artifactId>
                    <version>${pom.version}</version>
                  </dependency>
                  <dependency>
                    <groupId>commons-lang</groupId>
                    <artifactId>commons-lang</artifactId>
                    <version>${project.version}</version>
                  </dependency>
                  <dependency>
                    <groupId>commons-lang</groupId>
                    <artifactId>commons-lang</artifactId>
                    <version>${other.version}</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1.0</version>
                <properties>
                  <other.version>1.0.0</other.version>
                </properties>
                <dependencies>
                  <dependency>
                    <groupId>commons-lang</groupId>
                    <artifactId>commons-lang</artifactId>
                    <version>${project.version}</version>
                  </dependency>
                  <dependency>
                    <groupId>commons-lang</groupId>
                    <artifactId>commons-lang</artifactId>
                    <version>${project.version}</version>
                  </dependency>
                  <dependency>
                    <groupId>commons-lang</groupId>
                    <artifactId>commons-lang</artifactId>
                    <version>${project.version}</version>
                  </dependency>
                  <dependency>
                    <groupId>commons-lang</groupId>
                    <artifactId>commons-lang</artifactId>
                    <version>${other.version}</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }
}
