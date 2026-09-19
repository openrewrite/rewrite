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

import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

class RemoveEmptyXmlTagsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveEmptyXmlTags());
    }

    @DocumentExample
    @Test
    void removeEmptyPluginRepositories() {
        rewriteRun(
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

    @Test
    void deletesFileWhenRootCollapses() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyXmlTags(
            List.of("/server/featureManager", "/server[@*]"),
            "**/mp-telemetry.xml",
            true)),
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <server description="DO NOT MODIFY.">
                  <featureManager>
                  </featureManager>
              </server>
              """,
            (String) null,
            spec -> spec.path("src/main/liberty/config/configDropins/defaults/mp-telemetry.xml")
          )
        );
    }

    @Test
    void deleteFileIfEmptyDefaultsToTrue() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyXmlTags(null, null, null)),
          xml(
            """
              <root>
                  <child></child>
              </root>
              """,
            (String) null
          )
        );
    }

    @Test
    void preservesFileWhenRootHasAttributes() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyXmlTags(
            List.of("/server/featureManager"),
            null,
            true)),
          xml(
            """
              <server description="DO NOT MODIFY.">
                  <featureManager>
                  </featureManager>
              </server>
              """,
            """
              <server description="DO NOT MODIFY.">
              </server>
              """
          )
        );
    }

    @Test
    void xPathWhitelistScopesRemoval() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyXmlTags(
            List.of("//featureManager"),
            null,
            false)),
          xml(
            """
              <server>
                  <featureManager>
                  </featureManager>
                  <httpEndpoint>
                  </httpEndpoint>
              </server>
              """,
            """
              <server>
                  <httpEndpoint>
                  </httpEndpoint>
              </server>
              """
          )
        );
    }

    @Test
    void tagsWithAttributesArePreserved() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyXmlTags(
            List.of("//feature"),
            null,
            false)),
          xml(
            """
              <featureManager>
                  <feature></feature>
                  <feature version="1.0"></feature>
              </featureManager>
              """,
            """
              <featureManager>
                  <feature version="1.0"></feature>
              </featureManager>
              """
          )
        );
    }

    @Test
    void attributeWildcardAllowsAnyAttributes() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyXmlTags(
            List.of("//feature[@*]"),
            null,
            false)),
          xml(
            """
              <featureManager>
                  <feature></feature>
                  <feature version="1.0"></feature>
                  <feature version="2.0" scope="runtime"></feature>
              </featureManager>
              """,
            """
              <featureManager>
              </featureManager>
              """
          )
        );
    }

    @Test
    void singleAttributeAllowlist() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyXmlTags(
            List.of("//feature[@description]"),
            null,
            false)),
          xml(
            """
              <featureManager>
                  <feature></feature>
                  <feature description="X"></feature>
                  <feature description="X" version="1.0"></feature>
              </featureManager>
              """,
            """
              <featureManager>
                  <feature description="X" version="1.0"></feature>
              </featureManager>
              """
          )
        );
    }

    @Test
    void multipleAttributeAllowlist() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyXmlTags(
            List.of("//feature[@description or @other]"),
            null,
            false)),
          xml(
            """
              <featureManager>
                  <feature></feature>
                  <feature description="X"></feature>
                  <feature other="Y"></feature>
                  <feature description="X" other="Y"></feature>
                  <feature description="X" version="1.0"></feature>
              </featureManager>
              """,
            """
              <featureManager>
                  <feature description="X" version="1.0"></feature>
              </featureManager>
              """
          )
        );
    }

    @Test
    void fileMatcherSkipsNonMatchingFile() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyXmlTags(
            List.of("//featureManager"),
            "**/server.xml",
            false)),
          xml(
            """
              <server>
                  <featureManager>
                  </featureManager>
              </server>
              """,
            spec -> spec.path("src/main/other.xml")
          )
        );
    }

    @Test
    void deleteFileIfEmptyFalseLeavesEmptyRoot() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyXmlTags(
            List.of("/server/featureManager"),
            null,
            false)),
          xml(
            """
              <server>
                  <featureManager>
                  </featureManager>
              </server>
              """,
            """
              <server>
              </server>
              """
          )
        );
    }

    @Test
    void nestedEmptyTagsCollapseInOneRun() {
        rewriteRun(
          spec -> spec.recipe(new RemoveEmptyXmlTags(
            List.of("//plugin", "//plugins", "//pluginManagement", "//build"),
            null,
            false)),
          xml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <build>
                      <pluginManagement>
                          <plugins>
                              <plugin></plugin>
                          </plugins>
                      </pluginManagement>
                  </build>
              </project>
              """,
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
              </project>
              """
          )
        );
    }
}
