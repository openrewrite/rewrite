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
package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

// Bonus: If there's a way to modify these assertions to do an XML-based comparison instead of a string compare please point that out. These are unnecessarily brittle as-is due to caring about whitespace.
class UpgradeTransitiveDependencyVersionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpgradeTransitiveDependencyVersion(
          "com.fasterxml*", "jackson-core", "2.12.5", null, null, null, null, null, null, null));
    }

    @DocumentExample
    @Test
    void singleProject() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>core</artifactId>
                  <version>0.1.0-SNAPSHOT</version>
                  <dependencies>
                      <dependency>
                          <groupId>org.openrewrite</groupId>
                          <artifactId>rewrite-java</artifactId>
                          <version>7.0.0</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>core</artifactId>
                  <version>0.1.0-SNAPSHOT</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>com.fasterxml.jackson.core</groupId>
                              <artifactId>jackson-core</artifactId>
                              <version>2.12.5</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>org.openrewrite</groupId>
                          <artifactId>rewrite-java</artifactId>
                          <version>7.0.0</version>
                      </dependency>
                  </dependencies>
              </project>
              """)
        );
    }

    @Test
    void leavesDirectDependencyUntouched() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>core</artifactId>
                  <version>0.1.0-SNAPSHOT</version>
                  <dependencies>
                      <dependency>
                          <groupId>com.fasterxml.jackson.core</groupId>
                          <artifactId>jackson-core</artifactId>
                          <version>2.12.0</version>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }

    /*
        Demonstrates that this recipe currently takes no "upgrade dependency" action when it observes that a BOM manages the dependency version and that BOM appears to already set it to the desired version number.
        The problem with this seems to be that it is looking at the BOM in isolation and doesn't realize that a maven property set in the master POM is overriding a property value in the BOM
          and thus causing Maven to use the version number from the parent POM's property value instead of the one seen in the BOM.

        See the XML comments in the "before" XML below.
    */
    @Test
    void upgradeTransitiveDependencyVersion_NewIssue_NotYetFixed() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("commons-codec", "commons-codec", "1.15", null,
            null, null, null, null, null, null))
/*            .executionContext(
              MavenExecutionContextView
                .view(new InMemoryExecutionContext())
                .setRepositories(List.of(
                  MavenRepository.builder().id("jenkins").uri(myOrganizationsInternalMavenRepoURL).build()
                ))

            )*/,
          pomXml(
            """
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

                  <modelVersion>4.0.0</modelVersion>

                  <parent> <!-- Sets Maven property commons-codec.version = 1.10 -->
                      <groupId>com.ijson.common</groupId>
                      <artifactId>ijson-parent-pom</artifactId>
                      <version>1.0.8</version>
                  </parent>

                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>

                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <!-- Uses Maven property commons-codec.version = 1.15  NOTE that Maven honors the property in the master pom so we end up with v1.10 on the classpath. -->
                              <groupId>org.springframework.boot</groupId>
                              <artifactId>spring-boot-dependencies</artifactId>
                              <version>2.5.15</version>
                              <type>pom</type>
                              <scope>import</scope>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>

                  <dependencies>
                      <!-- Pulls in commons-codec transitively -->
                      <dependency>
                          <groupId>org.apache.httpcomponents</groupId>
                          <artifactId>httpclient</artifactId>
                          <version>4.5.6</version>
                      </dependency>
                  </dependencies>
              </project>

              """,
            """
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

                  <modelVersion>4.0.0</modelVersion>

                  <parent> <!-- Sets Maven property commons-codec.version = 1.10 -->
                      <groupId>com.ijson.common</groupId>
                      <artifactId>ijson-parent-pom</artifactId>
                      <version>1.0.8</version>
                  </parent>

                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>

                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>commons-codec</groupId>
                              <artifactId>commons-codec</artifactId>
                              <version>1.15</version>
                          </dependency>
                          <dependency>
                              <!-- Uses Maven property commons-codec.version = 1.15  NOTE that Maven honors the property in the master pom so we end up with v1.10 on the classpath. -->
                              <groupId>org.springframework.boot</groupId>
                              <artifactId>spring-boot-dependencies</artifactId>
                              <version>2.5.15</version>
                              <type>pom</type>
                              <scope>import</scope>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>

                  <dependencies>
                      <!-- Pulls in commons-codec transitively -->
                      <dependency>
                          <groupId>org.apache.httpcomponents</groupId>
                          <artifactId>httpclient</artifactId>
                          <version>4.5.6</version>
                      </dependency>
                  </dependencies>
              </project>

              """
          )
        );
    }

    // Demonstrates that transitive dependency versions can be upgraded when their version is managed by a master POM.
    @Test
    void upgradeTransitiveDependencyVersion_WorksForMasterPomManagedDeps() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("org.apache.commons", "commons-lang3", "3.14.0", null,
            null, null, null, null, null, null))
            /*.executionContext(
              MavenExecutionContextView
                .view(new InMemoryExecutionContext())
                .setRepositories(List.of(
                  MavenRepository.builder().id("jenkins").uri(myOrganizationsInternalMavenRepoURL).build()
                ))

            )*/,
          pomXml(
            """
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                      <!-- This parent's dependencyManagement has an entry for commons-lang3 setting the version to 3.9  -->
                      <groupId>org.apache.logging.log4j</groupId>
                      <artifactId>log4j</artifactId>
                      <version>2.13.3</version>
                  </parent>

                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>

                  <dependencies>
                      <!-- Pulls in commons-lang3 transitively -->
                      <dependency>
                          <groupId>org.apache.commons</groupId>
                          <artifactId>commons-text</artifactId>
                          <version>1.12.0</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            """
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                      <!-- This parent's dependencyManagement has an entry for commons-lang3 setting the version to 3.9  -->
                      <groupId>org.apache.logging.log4j</groupId>
                      <artifactId>log4j</artifactId>
                      <version>2.13.3</version>
                  </parent>

                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>org.apache.commons</groupId>
                              <artifactId>commons-lang3</artifactId>
                              <version>3.14.0</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>

                  <dependencies>
                      <!-- Pulls in commons-lang3 transitively -->
                      <dependency>
                          <groupId>org.apache.commons</groupId>
                          <artifactId>commons-text</artifactId>
                          <version>1.12.0</version>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }

    // Demonstrates that transitive dependency versions can be upgraded when their version is managed by a BOM specified in the dependencyManagement section.
    @Test
    void upgradeTransitiveDependencyVersion_WorksForBillOfMaterialsManagedDeps() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("org.springframework.boot", "spring-boot-actuator", "2.7.0", null,
            null, null, null, null, null, null))
            /*.executionContext(
              MavenExecutionContextView
                .view(new InMemoryExecutionContext())
                .setRepositories(List.of(
                  MavenRepository.builder().id("jenkins").uri(myOrganizationsInternalMavenRepoURL).build()
                ))

            )*/,
          pomXml(
            """
               <project xmlns="http://maven.apache.org/POM/4.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

                   <modelVersion>4.0.0</modelVersion>

                   <groupId>com.mycompany.app</groupId>
                   <artifactId>my-app</artifactId>
                   <version>1.0.0</version>

                 <dependencyManagement>
                     <dependencies>
                         <dependency>
                             <groupId>org.springframework.boot</groupId>
                             <artifactId>spring-boot-dependencies</artifactId>
                             <version>2.5.15</version>
                             <type>pom</type>
                             <scope>import</scope>
                         </dependency>
                     </dependencies>
                 </dependencyManagement>

                 <dependencies>
                     <!-- Pulls in spring-boot-actuator transitively -->
                     <dependency>
                         <groupId>org.springframework.boot</groupId>
                         <artifactId>spring-boot-starter-actuator</artifactId>
                         <version>2.7.0</version>
                     </dependency>
                 </dependencies>
               </project>
              """,
            """
              <project xmlns="http://maven.apache.org/POM/4.0.0"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

                  <modelVersion>4.0.0</modelVersion>

                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>

                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-actuator</artifactId>
                      <version>2.7.0</version>
                    </dependency>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-dependencies</artifactId>
                            <version>2.5.15</version>
                            <type>pom</type>
                            <scope>import</scope>
                        </dependency>
                    </dependencies>
                </dependencyManagement>

                <dependencies>
                    <!-- Pulls in spring-boot-actuator transitively -->
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-actuator</artifactId>
                        <version>2.7.0</version>
                    </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }
}
