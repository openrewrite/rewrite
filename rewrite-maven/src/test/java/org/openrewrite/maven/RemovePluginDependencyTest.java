/*
 * Copyright 2020 the original author or authors.
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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class RemovePluginDependencyTest implements RewriteTest {

    @DocumentExample
    @Test
    void removeDependency() {
        rewriteRun(
          spec -> spec.recipe(new RemovePluginDependency("org.apache.maven.plugins", "maven-surefire-plugin", "org.apache.maven.surefire", "surefire-junit*")),
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
              
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-surefire-plugin</artifactId>
                      <version>2.22.0</version>
                      <dependencies>
                        <dependency>
                          <groupId>org.apache.maven.surefire</groupId>
                          <artifactId>surefire-junit47</artifactId>
                          <version>2.22.0</version>
                        </dependency>
                        <dependency>
                          <groupId>org.antlr</groupId>
                          <artifactId>antlr4</artifactId>
                          <version>4.9.3</version>
                        </dependency>
                      </dependencies>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """,
              """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
              
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-surefire-plugin</artifactId>
                      <version>2.22.0</version>
                      <dependencies>
                        <dependency>
                          <groupId>org.antlr</groupId>
                          <artifactId>antlr4</artifactId>
                          <version>4.9.3</version>
                        </dependency>
                      </dependencies>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """
          )
        );
    }

    @Test
    void removeLastDependency() {
        rewriteRun(
          spec -> spec.recipe(new RemovePluginDependency("org.apache.maven.plugins", "maven-surefire-plugin", "org.apache.maven.surefire", "surefire-junit*")),
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
              
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-surefire-plugin</artifactId>
                      <version>2.22.0</version>
                      <dependencies>
                        <dependency>
                          <groupId>org.apache.maven.surefire</groupId>
                          <artifactId>surefire-junit47</artifactId>
                          <version>2.22.0</version>
                        </dependency>
                      </dependencies>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """,
              """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-surefire-plugin</artifactId>
                      <version>2.22.0</version>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """
          )
        );
    }

    @Test
    void wrongPlugin() {
        rewriteRun(
          spec -> spec.recipe(new RemovePluginDependency("org.foo", "foo", "org.apache.maven.surefire", "surefire-junit*")),
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-surefire-plugin</artifactId>
                      <version>2.22.0</version>
                      <dependencies>
                        <dependency>
                          <groupId>org.apache.maven.surefire</groupId>
                          <artifactId>surefire-junit47</artifactId>
                          <version>2.22.0</version>
                        </dependency>
                      </dependencies>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """
          )
        );
    }
}
