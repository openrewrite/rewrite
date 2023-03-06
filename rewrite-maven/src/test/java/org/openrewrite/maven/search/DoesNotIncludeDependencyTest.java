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
package org.openrewrite.maven.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.maven.AddProperty;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class DoesNotIncludeDependencyTest implements RewriteTest {
    @Test
    void dependencyPresent() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty("foo", "bar", null, null)
            .addApplicableTest(new DoesNotIncludeDependency("org.springframework", "spring-beans", true))),
          pomXml("""
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                    <dependency>
                      <groupId>org.springframework</groupId>
                      <artifactId>spring-beans</artifactId>
                      <version>6.0.0</version>
                    </dependency>
                  </dependencies>
                </project>
                """));
    }

    @Test
    void dependencyPresentTransitively() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty("foo", "bar", null, null)
            .addApplicableTest(new DoesNotIncludeDependency("org.springframework", "spring-beans", true))),
          pomXml("""
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                    <dependency>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-actuator</artifactId>
                      <version>3.0.0</version>
                    </dependency>
                  </dependencies>
                </project>
                """));
    }

    @Test
    void dependencyPresentTransitivelyButNotCheckingTransitive() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty("foo", "bar", null, null)
            .addApplicableTest(new DoesNotIncludeDependency("org.springframework", "spring-beans", false))),
          pomXml("""
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                    <dependency>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-actuator</artifactId>
                      <version>3.0.0</version>
                    </dependency>
                  </dependencies>
                </project>
                """, """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>
                  <properties>
                    <foo>bar</foo>
                  </properties>
                  <dependencies>
                    <dependency>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-actuator</artifactId>
                      <version>3.0.0</version>
                    </dependency>
                  </dependencies>
                </project>
                """));
    }

    @Test
    void multimodule() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty("foo", "bar", null, null)
            .addSingleSourceApplicableTest(new DoesNotIncludeDependency("org.springframework", "spring-beans", true))),
          pomXml("""
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>a</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                    <dependency>
                      <groupId>org.springframework</groupId>
                      <artifactId>spring-beans</artifactId>
                      <version>6.0.0</version>
                    </dependency>
                  </dependencies>
                </project>
                """, spec -> spec.path("a/pom.xml")),
          pomXml("""
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>b</artifactId>
                  <version>1.0.0</version>
                </project>
                """, """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>b</artifactId>
                  <version>1.0.0</version>
                  <properties>
                    <foo>bar</foo>
                  </properties>
                </project>
                """, spec -> spec.path("b/pom.xml"))
          );
    }
}