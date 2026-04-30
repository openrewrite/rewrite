/*
 * Copyright 2024 the original author or authors.
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
import org.openrewrite.maven.table.DependenciesDeclared;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.Assertions.pomXml;

class FindDependencyTest implements RewriteTest {
    @DocumentExample
    @Test
    void simple() {
        rewriteRun(spec -> spec.recipe(new FindDependency("jakarta.activation", "jakarta.activation-api", null, null)),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <dependencies>
                  <dependency>
                    <groupId>jakarta.activation</groupId>
                    <artifactId>jakarta.activation-api</artifactId>
                    <version>2.1.2</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <dependencies>
                  <!--~~>--><dependency>
                    <groupId>jakarta.activation</groupId>
                    <artifactId>jakarta.activation-api</artifactId>
                    <version>2.1.2</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void version() {
        rewriteRun(spec -> spec.recipe(new FindDependency("jakarta.activation", "jakarta.activation-api", "2.1.2", null)),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <dependencies>
                  <dependency>
                    <groupId>jakarta.activation</groupId>
                    <artifactId>jakarta.activation-api</artifactId>
                    <version>2.1.2</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <dependencies>
                  <!--~~>--><dependency>
                    <groupId>jakarta.activation</groupId>
                    <artifactId>jakarta.activation-api</artifactId>
                    <version>2.1.2</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void versionRange() {
        rewriteRun(spec -> spec.recipe(new FindDependency("jakarta.activation", "jakarta.activation-api", "^2", null)),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <dependencies>
                  <dependency>
                    <groupId>jakarta.activation</groupId>
                    <artifactId>jakarta.activation-api</artifactId>
                    <version>2.1.2</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <dependencies>
                  <!--~~>--><dependency>
                    <groupId>jakarta.activation</groupId>
                    <artifactId>jakarta.activation-api</artifactId>
                    <version>2.1.2</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void wrongVersion() {
        rewriteRun(spec -> spec.recipe(new FindDependency("jakarta.activation", "jakarta.activation-api", "1.0.0", null)),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <dependencies>
                  <dependency>
                    <groupId>jakarta.activation</groupId>
                    <artifactId>jakarta.activation-api</artifactId>
                    <version>2.1.2</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void wrongVersionRange() {
        rewriteRun(spec -> spec.recipe(new FindDependency("jakarta.activation", "jakarta.activation-api", "^1", null)),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <dependencies>
                  <dependency>
                    <groupId>jakarta.activation</groupId>
                    <artifactId>jakarta.activation-api</artifactId>
                    <version>2.1.2</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void multiModule() {
        rewriteRun(spec -> spec.recipe(new FindDependency("jakarta.activation", "jakarta.activation-api", null, null)),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <modules>
                  <module>sample-module</module>
                </modules>
                <dependencies>
                  <dependency>
                    <groupId>jakarta.servlet</groupId>
                    <artifactId>jakarta.servlet-api</artifactId>
                    <version>6.0.0</version>
                  </dependency>
                </dependencies>
              </project>
              """
            ,
                sourceSpecs -> sourceSpecs.path("pom.xml")),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample-module</artifactId>
                <version>1.0.0</version>
                <dependencies>
                  <dependency>
                    <groupId>jakarta.activation</groupId>
                    <artifactId>jakarta.activation-api</artifactId>
                    <version>2.1.2</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample-module</artifactId>
                <version>1.0.0</version>
                <dependencies>
                  <!--~~>--><dependency>
                    <groupId>jakarta.activation</groupId>
                    <artifactId>jakarta.activation-api</artifactId>
                    <version>2.1.2</version>
                  </dependency>
                </dependencies>
              </project>
              """
          ,
                sourceSpecs -> sourceSpecs.path("sample-module/pom.xml"))
        );
    }

    @Test
    void withProperties() {
        rewriteRun(spec -> spec.recipe(new FindDependency("jakarta.activation", "jakarta.activation-api", "2.1.2", null)),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <properties>
                  <groupId>jakarta.activation</groupId>
                  <artifactId>jakarta.activation-api</artifactId>
                  <version>2.1.2</version>
                </properties>
                <dependencies>
                  <dependency>
                    <groupId>${groupId}</groupId>
                    <artifactId>${artifactId}</artifactId>
                    <version>${version}</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <properties>
                  <groupId>jakarta.activation</groupId>
                  <artifactId>jakarta.activation-api</artifactId>
                  <version>2.1.2</version>
                </properties>
                <dependencies>
                  <!--~~>--><dependency>
                    <groupId>${groupId}</groupId>
                    <artifactId>${artifactId}</artifactId>
                    <version>${version}</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void versionInProperties() {
        rewriteRun(spec -> spec.recipe(new FindDependency("jakarta.activation", "jakarta.activation-api", "2.1.2", null)),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <properties>
                  <activation.version>2.1.2</activation.version>
                </properties>
                <dependencies>
                  <dependency>
                    <groupId>jakarta.activation</groupId>
                    <artifactId>jakarta.activation-api</artifactId>
                    <version>${activation.version}</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <properties>
                  <activation.version>2.1.2</activation.version>
                </properties>
                <dependencies>
                  <!--~~>--><dependency>
                    <groupId>jakarta.activation</groupId>
                    <artifactId>jakarta.activation-api</artifactId>
                    <version>${activation.version}</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void emitsDataTableRow() {
        rewriteRun(spec -> spec.recipe(new FindDependency("jakarta.activation", "jakarta.activation-api", null, null))
            .dataTable(DependenciesDeclared.Row.class, rows -> {
                assertThat(rows).hasSize(1);
                DependenciesDeclared.Row row = rows.get(0);
                assertThat(row.getGroupId()).isEqualTo("jakarta.activation");
                assertThat(row.getArtifactId()).isEqualTo("jakarta.activation-api");
                assertThat(row.getVersion()).isEqualTo("2.1.2");
                assertThat(row.getScope()).isEqualTo("compile");
                assertThat(row.getSourceSet()).isEqualTo("main");
            }),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <dependencies>
                  <dependency>
                    <groupId>jakarta.activation</groupId>
                    <artifactId>jakarta.activation-api</artifactId>
                    <version>2.1.2</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <dependencies>
                  <!--~~>--><dependency>
                    <groupId>jakarta.activation</groupId>
                    <artifactId>jakarta.activation-api</artifactId>
                    <version>2.1.2</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void declaredScopeRecordedInTable() {
        rewriteRun(spec -> spec.recipe(new FindDependency("jakarta.activation", "jakarta.activation-api", null, null))
            .dataTable(DependenciesDeclared.Row.class, rows -> {
                assertThat(rows).hasSize(1);
                assertThat(rows.get(0).getScope()).isEqualTo("test");
            }),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <dependencies>
                  <dependency>
                    <groupId>jakarta.activation</groupId>
                    <artifactId>jakarta.activation-api</artifactId>
                    <version>2.1.2</version>
                    <scope>test</scope>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <dependencies>
                  <!--~~>--><dependency>
                    <groupId>jakarta.activation</groupId>
                    <artifactId>jakarta.activation-api</artifactId>
                    <version>2.1.2</version>
                    <scope>test</scope>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void rowOnlyEmittedForDeclaredCoordinate() {
        rewriteRun(spec -> spec.recipe(new FindDependency("jakarta.activation", "jakarta.activation-api", null, null))
            .dataTable(DependenciesDeclared.Row.class, rows -> {
                assertThat(rows).hasSize(1);
                assertThat(rows.get(0).getArtifactId()).isEqualTo("jakarta.activation-api");
            }),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <dependencies>
                  <dependency>
                    <groupId>jakarta.activation</groupId>
                    <artifactId>jakarta.activation-api</artifactId>
                    <version>2.1.2</version>
                  </dependency>
                  <dependency>
                    <groupId>jakarta.servlet</groupId>
                    <artifactId>jakarta.servlet-api</artifactId>
                    <version>6.0.0</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <dependencies>
                  <!--~~>--><dependency>
                    <groupId>jakarta.activation</groupId>
                    <artifactId>jakarta.activation-api</artifactId>
                    <version>2.1.2</version>
                  </dependency>
                  <dependency>
                    <groupId>jakarta.servlet</groupId>
                    <artifactId>jakarta.servlet-api</artifactId>
                    <version>6.0.0</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }
}
