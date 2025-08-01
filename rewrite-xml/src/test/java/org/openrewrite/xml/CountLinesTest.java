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
package org.openrewrite.xml;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.xml.Assertions.xml;

class CountLinesTest implements RewriteTest {

    @Test
    void lineCount() {
        rewriteRun(
          xml(
            """
              <project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xmlns="http://maven.apache.org/POM/4.0.0"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>

                  <groupId>org.openrewrite.maven</groupId>
                  <artifactId>round_trip_serialization</artifactId>
                  <version>1.0</version>
                  <packaging>jar</packaging>
                  <name>BasicIT#round_trip_serialization</name>

                  <properties>
                      <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                  </properties>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>@project.groupId@</groupId>
                              <artifactId>@project.artifactId@</artifactId>
                              <version>@project.version@</version>
                              <executions>
                                  <execution>
                                      <phase>package</phase>
                                      <goals><goal>ast</goal></goals>
                                  </execution>
                              </executions>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            spec -> spec.afterRecipe(xml -> assertThat(CountLinesVisitor.countLines(xml)).isEqualTo(28))
          )
        );
    }
}
