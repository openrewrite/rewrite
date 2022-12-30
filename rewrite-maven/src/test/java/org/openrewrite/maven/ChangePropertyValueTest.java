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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class ChangePropertyValueTest implements RewriteTest {
    @Test
    void property() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyValue("guava.version", "29.0-jre", false, false)),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                 
                <properties>
                  <guava.version>28.2-jre</guava.version>
                </properties>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                 
                <properties>
                  <guava.version>29.0-jre</guava.version>
                </properties>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
          )
        );
    }

    @Test
    void addFirstProperty() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyValue("key", "value", true, false)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              
                <dependencies>
                </dependencies>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <properties>
                  <key>value</key>
                </properties>
              
                <dependencies>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void changeExistingProperty() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyValue("key", "value", true, false)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <properties>
                  <key>v</key>
                </properties>
              
                <dependencies>
                </dependencies>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <properties>
                  <key>value</key>
                </properties>
              
                <dependencies>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void addPropertyInOrder() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyValue("key", "value", true, false)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <properties>
                  <abc>value</abc>
                  <other>value</other>
                </properties>
              
                <dependencies>
                </dependencies>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <properties>
                  <abc>value</abc>
                  <key>value</key>
                  <other>value</other>
                </properties>
              
                <dependencies>
                </dependencies>
              </project>
              """
          )
        );
    }
}
