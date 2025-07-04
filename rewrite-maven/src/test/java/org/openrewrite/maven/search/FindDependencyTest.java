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
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class FindDependencyTest implements RewriteTest {
    @DocumentExample
    @Test
    void simple() {
        rewriteRun(spec -> spec.recipe(new FindDependency("jakarta.activation", "jakarta.activation-api", null, null)),
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
    void generatesDataTableForFoundDependencies() {
        rewriteRun(
          spec -> {
              spec.dataTable(FindDependency.Row.class, rows -> {
                  assertThat(rows).containsExactly(
                    new FindDependency.Row("project/pom.xml", "org.openrewrite", "rewrite-core", "8.56.0"),
                    new FindDependency.Row("project/pom.xml", "org.openrewrite.recipe", "rewrite-spring", "6.9.0"),
                    new FindDependency.Row("otherproject/pom.xml", "org.openrewrite", "rewrite-core", "8.55.0"),
                    new FindDependency.Row("otherproject/pom.xml", "org.openrewrite.recipe", "rewrite-spring", "6.8.0")
                  );
              });
              spec.recipe(new FindDependency("org.openrewrite*", "*", null, null));
          },
          mavenProject("project",
            pomXml(
              //language=xml
              """
                <project>
                  <groupId>org.sample</groupId>
                  <artifactId>project</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                    <dependency>
                      <groupId>org.openrewrite</groupId>
                      <artifactId>rewrite-core</artifactId>
                      <version>8.56.0</version>
                    </dependency>
                    <dependency>
                      <groupId>org.openrewrite.recipe</groupId>
                      <artifactId>rewrite-spring</artifactId>
                      <version>6.9.0</version>
                    </dependency>
                    <dependency>
                      <groupId>org.junit.jupiter</groupId>
                      <artifactId>junit-jupiter</artifactId>
                      <version>5.13.2</version>
                    </dependency>
                  </dependencies>
                </project>
                """,
              """
                <project>
                  <groupId>org.sample</groupId>
                  <artifactId>project</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                    <!--~~>--><dependency>
                      <groupId>org.openrewrite</groupId>
                      <artifactId>rewrite-core</artifactId>
                      <version>8.56.0</version>
                    </dependency>
                    <!--~~>--><dependency>
                      <groupId>org.openrewrite.recipe</groupId>
                      <artifactId>rewrite-spring</artifactId>
                      <version>6.9.0</version>
                    </dependency>
                    <dependency>
                      <groupId>org.junit.jupiter</groupId>
                      <artifactId>junit-jupiter</artifactId>
                      <version>5.13.2</version>
                    </dependency>
                  </dependencies>
                </project>
                """
            )
          ),
          mavenProject("otherproject",
            pomXml(
              //language=xml
              """
                <project>
                  <groupId>org.sample</groupId>
                  <artifactId>otherproject</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                    <dependency>
                      <groupId>org.openrewrite</groupId>
                      <artifactId>rewrite-core</artifactId>
                      <version>8.55.0</version>
                    </dependency>
                    <dependency>
                      <groupId>org.openrewrite.recipe</groupId>
                      <artifactId>rewrite-spring</artifactId>
                      <version>6.8.0</version>
                    </dependency>
                    <dependency>
                      <groupId>org.junit.jupiter</groupId>
                      <artifactId>junit-jupiter</artifactId>
                      <version>5.13.1</version>
                    </dependency>
                  </dependencies>
                </project>
                """,
              """
                <project>
                  <groupId>org.sample</groupId>
                  <artifactId>otherproject</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                    <!--~~>--><dependency>
                      <groupId>org.openrewrite</groupId>
                      <artifactId>rewrite-core</artifactId>
                      <version>8.55.0</version>
                    </dependency>
                    <!--~~>--><dependency>
                      <groupId>org.openrewrite.recipe</groupId>
                      <artifactId>rewrite-spring</artifactId>
                      <version>6.8.0</version>
                    </dependency>
                    <dependency>
                      <groupId>org.junit.jupiter</groupId>
                      <artifactId>junit-jupiter</artifactId>
                      <version>5.13.1</version>
                    </dependency>
                  </dependencies>
                </project>
                """
            )
          )
        );
    }

    @Test
    void withProperties() {
        rewriteRun(spec -> spec.recipe(new FindDependency("jakarta.activation", "jakarta.activation-api", "2.1.2", null)),
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
}
