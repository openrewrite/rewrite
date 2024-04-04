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
package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class AddPropertyTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddProperty("key", "value", null, false));
    }

    @Test
    void notIfParentHasDefined() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-parent</artifactId>
                <version>1</version>
                <properties>
                  <key>value</key>
                </properties>
              </project>
              """
          ),
          mavenProject("my-app",
            pomXml(
       """
              <project>
                <parent>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-parent</artifactId>
                  <version>1</version>
                </parent>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
            )
          )
        );
    }


    @Issue("https://github.com/openrewrite/rewrite/issues/3895")
    @Test
    void prefersToUpdateParent() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-parent</artifactId>
                <version>1</version>
                <modules>
                  <module>my-app</module>
                </modules>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-parent</artifactId>
                <version>1</version>
                <modules>
                  <module>my-app</module>
                </modules>
                <properties>
                  <key>value</key>
                </properties>
              </project>
              """
          ),
          mavenProject("my-app",
            pomXml(
        """
               <project>
                 <parent>
                   <groupId>com.mycompany.app</groupId>
                   <artifactId>my-parent</artifactId>
                   <version>1</version>
                   <relativePath>../pom.xml</relativePath>
                 </parent>
                 <artifactId>my-app</artifactId>
                 <version>1</version>
                 <properties>
                  <key>oldValue</key>
                 </properties>
               </project>
               """,
          """
                 <project>
                   <parent>
                     <groupId>com.mycompany.app</groupId>
                     <artifactId>my-parent</artifactId>
                     <version>1</version>
                     <relativePath>../pom.xml</relativePath>
                   </parent>
                   <artifactId>my-app</artifactId>
                   <version>1</version>
                 </project>
                 """
            )
          )
        );
    }

    @Test
    void trustParent() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty("key", "value", null, true)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-parent</artifactId>
                <version>1</version>
                <properties>
                  <key>v</key>
                </properties>
              </project>
              """,
            SourceSpec::skip
          ),
          mavenProject("my-app",
            pomXml(
        """
              <project>
                <parent>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-parent</artifactId>
                  <version>1</version>
                </parent>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
            )
          )
        );
    }

    @DocumentExample
    @Test
    void addFirstProperty() {
        rewriteRun(
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
    void addPropertyInOrder() {
        rewriteRun(
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
              </project>
              """
          )
        );
    }

    @Test
    void changeExistingPropertyWithDifferentValue() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <properties>
                  <key>v</key>
                </properties>
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
              </project>
              """
          )
        );
    }

    @Test
    void preserveExistingPropertyWithDifferentValue() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty("key", "value", true, false)),
          pomXml(
     """
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <properties>
                <key>v</key>
              </properties>
            </project>
            """
          )
        );
    }
}
