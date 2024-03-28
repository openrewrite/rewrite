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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class FindDependencyTest implements RewriteTest {
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
            ,sourceSpecs -> sourceSpecs.path("pom.xml")),
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
          ,sourceSpecs -> sourceSpecs.path("sample-module/pom.xml"))
        );
    }
}