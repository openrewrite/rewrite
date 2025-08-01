/*
 * Copyright 2025 the original author or authors.
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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class RemoveLog4JBomTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromYaml(
          """
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.maven.RemoveLog4JBom
            description: >-
              Remove log4j-bom dependency because it's already defined in SB's BOM.
            recipeList:
              - org.openrewrite.maven.RemoveManagedDependency:
                  groupId: org.apache.logging.log4j
                  artifactId: log4j-bom
              - org.openrewrite.maven.RemoveDependency:
                  groupId: org.apache.logging.log4j
                  artifactId: log4j-bom
              - org.openrewrite.maven.RemoveProperty:
                  propertyName: log4j2.version
            """,
          "org.openrewrite.maven.RemoveLog4JBom");
    }

    @Test
    void removeLog4JBom() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>dummyGroupId</groupId>
                  <artifactId>parentArtifactId</artifactId>
                  <version>1.0.0-SNAPSHOT</version>
                  <packaging>pom</packaging>
              
                  <properties>
                      <log4j2.version>2.17.1</log4j2.version>
                  </properties>
              
                  <modules>
                      <module>child-module</module>
                  </modules>
              
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>org.apache.logging.log4j</groupId>
                               <artifactId>log4j-bom</artifactId>
                              <version>${log4j2.version}</version>
                              <scope>import</scope>
                              <type>pom</type>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """,
            """
              <project>
                  <groupId>dummyGroupId</groupId>
                  <artifactId>parentArtifactId</artifactId>
                  <version>1.0.0-SNAPSHOT</version>
                  <packaging>pom</packaging>
              
                  <modules>
                      <module>child-module</module>
                  </modules>
              </project>
              """,
            s -> s.path("pom.xml")
          ),
          pomXml(
            """
              <project>
                  <parent>
                    <groupId>dummyGroupId</groupId>
                    <artifactId>parentArtifactId</artifactId>
                    <version>1.0.0-SNAPSHOT</version>
                    <relativePath>../pom.xml</relativePath>
                  </parent>
              
                  <artifactId>child-artifact</artifactId>
                  <packaging>jar</packaging>
                  <name>child-module</name>
              
              </project>
              """,
            s -> s.path("child-module/pom.xml")
          )
        );
    }
}
