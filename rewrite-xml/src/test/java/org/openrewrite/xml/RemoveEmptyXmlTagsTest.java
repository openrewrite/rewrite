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
package org.openrewrite.xml;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

class RemoveEmptyXmlTagsTest implements RewriteTest {
    @Test
    void removeEmptyPluginRepositories() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyXmlTags()),
          xml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>sample-app</artifactId>
                  <version>1.0-SNAPSHOT</version>
                  <pluginRepositories>
                  </pluginRepositories>
              </project>
              """,
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>sample-app</artifactId>
                  <version>1.0-SNAPSHOT</version>
              </project>
              """
          )
        );
    }

    @Test
    void removeNestedEmptyTags() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyXmlTags()),
          xml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>sample-app</artifactId>
                  <version>1.0-SNAPSHOT</version>
                  <build>
                      <pluginManagement>
                          <plugins>
                              <plugin/>
                          </plugins>
                      </pluginManagement>
                  </build>
              </project>
              """,
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>sample-app</artifactId>
                  <version>1.0-SNAPSHOT</version>
              </project>
              """
          )
        );
    }

    @Test
    void retainWhenThereAttributes() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyXmlTags()),
          xml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>sample-app</artifactId>
                  <version>1.0-SNAPSHOT</version>
                  <build attr="true">
                  </build>
                  <dependencies attr="false" />
              </project>
              """
          )
        );
    }
}