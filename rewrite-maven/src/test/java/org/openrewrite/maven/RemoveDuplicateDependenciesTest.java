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

import static org.openrewrite.maven.Assertions.pomXml;

class RemoveDuplicateDependenciesTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveDuplicateDependencies());
    }

    @DocumentExample
    @Test
    void removeSingleDuplicate() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                  <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.13.1</version>
                    <scope>test</scope>
                  </dependency>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                  <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.13.1</version>
                    <scope>test</scope>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void notApplicable() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
          )
        );
    }

    @Test
    void preservesComments() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <!-- comment 1 -->
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                  <!-- comment 2 -->
                  <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.13.1</version>
                    <scope>test</scope>
                  </dependency>
                  <!-- comment 3 -->
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <!-- comment 1 -->
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                  <!-- comment 2 -->
                  <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.13.1</version>
                    <scope>test</scope>
                  </dependency>
                  <!-- comment 3 -->
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void removeMultipleDuplicates() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                  <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.13.1</version>
                    <scope>test</scope>
                  </dependency>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                  <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.13.1</version>
                    <scope>test</scope>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void removeDependencyWithDifferentVersion() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>com.google.inject</groupId>
                    <artifactId>guice</artifactId>
                    <version>4.2.1</version>
                  </dependency>
                  <dependency>
                    <groupId>com.google.inject</groupId>
                    <artifactId>guice</artifactId>
                    <version>4.2.2</version>
                  </dependency>
                </dependencies>
              </project>
              """,
                """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>com.google.inject</groupId>
                    <artifactId>guice</artifactId>
                    <version>4.2.2</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void keepDependencyWithClassifier() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>com.google.inject</groupId>
                    <artifactId>guice</artifactId>
                    <version>4.2.2</version>
                  </dependency>
                  <dependency>
                    <groupId>com.google.inject</groupId>
                    <artifactId>guice</artifactId>
                    <version>4.2.2</version>
                    <classifier>no_aop</classifier>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void keepDependencyWithType() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                    <type>war</type>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void keepDependencyManagementWithType() {
        rewriteRun(
          pomXml(
            """
                <project>
              <modelVersion>4.0.0</modelVersion>

              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>

              <dependencyManagement>
                <dependencies>
              	<dependency>
              	  <groupId>com.acme</groupId>
              	  <artifactId>example-dependency</artifactId>
                    <version>1.0.0</version>
              	</dependency>
              	<dependency>
                    <groupId>com.acme</groupId>
                    <artifactId>example-dependency</artifactId>
                    <version>1.0.0</version>
                    <type>test-jar</type>
              	</dependency>
                </dependencies>
              </dependencyManagement>
               </project>
              """
          )
        );
    }

    @Test
    void keepDependencyWithDifferentScope() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                    <scope>compile</scope>
                  </dependency>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                    <scope>test</scope>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void removeDuplicatedDependencyWithImportScope() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>

                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>

                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>org.apache.logging.log4j</groupId>
                              <artifactId>log4j-bom</artifactId>
                              <version>2.24.0</version>
                              <scope>import</scope>
                              <type>pom</type>
                          </dependency>
                          <dependency>
                              <groupId>org.apache.logging.log4j</groupId>
                              <artifactId>log4j-bom</artifactId>
                              <version>2.24.0</version>
                              <scope>import</scope>
                              <type>pom</type>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """,
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>

                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>

                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>org.apache.logging.log4j</groupId>
                              <artifactId>log4j-bom</artifactId>
                              <version>2.24.0</version>
                              <scope>import</scope>
                              <type>pom</type>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void removeDependencyWithDefaultType() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                    <type>jar</type>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3832")
    @Test
    void retainDuplicateManagedDependenciesWithDifferentClassifier() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <properties>
                  <version.chromedriver>94.0.4606.61</version.chromedriver>
                </properties>

                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>selenium-tools</groupId>
                      <artifactId>chromedriver</artifactId>
                      <classifier>w64</classifier>
                      <version>${version.chromedriver}</version>
                      <type>tar.gz</type>
                    </dependency>
                    <dependency>
                      <groupId>selenium-tools</groupId>
                      <artifactId>chromedriver</artifactId>
                      <classifier>x64</classifier>
                      <version>${version.chromedriver}</version>
                      <type>tar.gz</type>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4868")
    @Test
    void retainWithAndWithoutClassifier() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>com.adobe.aio.cloudmanager</groupId>
                              <artifactId>aio-lib-cloudmanager</artifactId>
                              <classifier>java8</classifier>
                              <version>2.0.0</version>
                          </dependency>
                          <dependency>
                              <groupId>com.adobe.aio.cloudmanager</groupId>
                              <artifactId>aio-lib-cloudmanager</artifactId>
                              <version>2.0.0</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void retainLaterDependencyDeclaration() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.13.1</version>
                  </dependency>
                  <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.13.2</version>
                  </dependency>

                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                    <optional>false</optional>
                  </dependency>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                    <optional>true</optional>
                  </dependency>

                  <dependency>
                    <groupId>org.apache.httpcomponents</groupId>
                    <artifactId>httpclient</artifactId>
                    <version>4.5.13</version>
                  </dependency>
                  <dependency>
                    <groupId>org.apache.httpcomponents</groupId>
                    <artifactId>httpclient</artifactId>
                    <version>4.5.13</version>
                    <exclusions>
                      <exclusion>
                        <groupId>commons-codec</groupId>
                        <artifactId>commons-codec</artifactId>
                      </exclusion>
                    </exclusions>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.13.2</version>
                  </dependency>

                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                    <optional>true</optional>
                  </dependency>

                  <dependency>
                    <groupId>org.apache.httpcomponents</groupId>
                    <artifactId>httpclient</artifactId>
                    <version>4.5.13</version>
                    <exclusions>
                      <exclusion>
                        <groupId>commons-codec</groupId>
                        <artifactId>commons-codec</artifactId>
                      </exclusion>
                    </exclusions>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void retainLaterDependencyDeclarationInReverseOrder() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.13.2</version>
                  </dependency>
                  <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.13.1</version>
                  </dependency>

                  <dependency>
                    <groupId>org.apache.httpcomponents</groupId>
                    <artifactId>httpclient</artifactId>
                    <version>4.5.13</version>
                    <exclusions>
                      <exclusion>
                        <groupId>commons-codec</groupId>
                        <artifactId>commons-codec</artifactId>
                      </exclusion>
                    </exclusions>
                  </dependency>
                  <dependency>
                    <groupId>org.apache.httpcomponents</groupId>
                    <artifactId>httpclient</artifactId>
                    <version>4.5.13</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.13.1</version>
                  </dependency>

                  <dependency>
                    <groupId>org.apache.httpcomponents</groupId>
                    <artifactId>httpclient</artifactId>
                    <version>4.5.13</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    /**
     * `<dependencyManagement>` resolves differently from `<dependencies>`: duplicates merge field-wise, with each
     * field taken from the first declaration that sets it. Both duplicates here set the same fields, so the later
     * declaration contributes nothing to the effective entry and can be removed, whatever its values.
     */
    @Test
    void retainFirstManagedDependencyDeclaration() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.acme</groupId>
                      <artifactId>versioned</artifactId>
                      <version>1</version>
                    </dependency>
                    <dependency>
                      <groupId>com.acme</groupId>
                      <artifactId>versioned</artifactId>
                      <version>2</version>
                    </dependency>

                    <dependency>
                      <groupId>com.acme</groupId>
                      <artifactId>scoped</artifactId>
                      <version>1</version>
                      <scope>compile</scope>
                    </dependency>
                    <dependency>
                      <groupId>com.acme</groupId>
                      <artifactId>scoped</artifactId>
                      <version>1</version>
                      <scope>runtime</scope>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.acme</groupId>
                      <artifactId>versioned</artifactId>
                      <version>1</version>
                    </dependency>

                    <dependency>
                      <groupId>com.acme</groupId>
                      <artifactId>scoped</artifactId>
                      <version>1</version>
                      <scope>compile</scope>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """
          )
        );
    }

    /**
     * The field-wise merge means the effective managed entry can be made up of several declarations: here Maven
     * takes the version from the second declaration because the first does not set one. Removing the second
     * declaration would leave the dependency with no managed version at all.
     */
    @Test
    void retainManagedDuplicateSettingAFieldTheFirstLeavesUnset() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                    </dependency>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>32.1.3-jre</version>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """
          )
        );
    }

    /**
     * Scope comes from the first declaration that sets it and exclusions accumulate across all duplicates, so
     * those later declarations contribute to the effective entry and have to stay. Maven 3.9 does not inject
     * {@code <optional>} from {@code <dependencyManagement>} at all, so a duplicate that only adds it changes
     * nothing either way; the recipe keeps it rather than reasoning about a field the resolved model does not
     * carry.
     */
    @Test
    void retainManagedDuplicateContributingScopeOptionalOrExclusions() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.acme</groupId>
                      <artifactId>scoped</artifactId>
                      <version>1</version>
                    </dependency>
                    <dependency>
                      <groupId>com.acme</groupId>
                      <artifactId>scoped</artifactId>
                      <version>1</version>
                      <scope>test</scope>
                    </dependency>

                    <dependency>
                      <groupId>com.acme</groupId>
                      <artifactId>optional</artifactId>
                      <version>1</version>
                    </dependency>
                    <dependency>
                      <groupId>com.acme</groupId>
                      <artifactId>optional</artifactId>
                      <version>1</version>
                      <optional>true</optional>
                    </dependency>

                    <dependency>
                      <groupId>com.acme</groupId>
                      <artifactId>excluded</artifactId>
                      <version>1</version>
                    </dependency>
                    <dependency>
                      <groupId>com.acme</groupId>
                      <artifactId>excluded</artifactId>
                      <version>1</version>
                      <exclusions>
                        <exclusion>
                          <groupId>commons-logging</groupId>
                          <artifactId>commons-logging</artifactId>
                        </exclusion>
                      </exclusions>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """
          )
        );
    }

    /**
     * A later duplicate that sets no field the earlier declarations leave unset and carries no exclusion they do
     * not already carry contributes nothing to the effective entry, so it can still be removed.
     */
    @Test
    void removeRedundantManagedDependencyDuplicates() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.acme</groupId>
                      <artifactId>scoped</artifactId>
                      <version>1</version>
                      <scope>runtime</scope>
                    </dependency>
                    <dependency>
                      <groupId>com.acme</groupId>
                      <artifactId>scoped</artifactId>
                      <version>2</version>
                    </dependency>

                    <dependency>
                      <groupId>com.acme</groupId>
                      <artifactId>excluded</artifactId>
                      <version>1</version>
                      <exclusions>
                        <exclusion>
                          <groupId>commons-logging</groupId>
                          <artifactId>commons-logging</artifactId>
                        </exclusion>
                      </exclusions>
                    </dependency>
                    <dependency>
                      <groupId>com.acme</groupId>
                      <artifactId>excluded</artifactId>
                      <version>1</version>
                      <exclusions>
                        <exclusion>
                          <groupId>commons-logging</groupId>
                          <artifactId>commons-logging</artifactId>
                        </exclusion>
                      </exclusions>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.acme</groupId>
                      <artifactId>scoped</artifactId>
                      <version>1</version>
                      <scope>runtime</scope>
                    </dependency>

                    <dependency>
                      <groupId>com.acme</groupId>
                      <artifactId>excluded</artifactId>
                      <version>1</version>
                      <exclusions>
                        <exclusion>
                          <groupId>commons-logging</groupId>
                          <artifactId>commons-logging</artifactId>
                        </exclusion>
                      </exclusions>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """
          )
        );
    }

    /**
     * A repeated BOM import at a different version is not redundant: for entries both versions manage the first
     * import wins (see `ResolvedPomTest#firstUniqueManagedDependencyWins`), but the second version may manage
     * entries the first does not, and those still take effect. Only an import of the same version again changes
     * nothing, see `removeDuplicatedDependencyWithImportScope`.
     */
    @Test
    void retainRepeatedBomImportWithDifferentVersion() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>

                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>

                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>org.apache.logging.log4j</groupId>
                              <artifactId>log4j-bom</artifactId>
                              <version>2.24.0</version>
                              <scope>import</scope>
                              <type>pom</type>
                          </dependency>
                          <dependency>
                              <groupId>org.apache.logging.log4j</groupId>
                              <artifactId>log4j-bom</artifactId>
                              <version>2.24.1</version>
                              <scope>import</scope>
                              <type>pom</type>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }

    /**
     * The resolved model orders duplicated dependencies by their first declaration, so the surviving declaration
     * has to stay in that position rather than move to where it was written. Comments keep the position they were
     * written in, exactly as they do when a duplicate is removed outright, see `preservesComments`.
     */
    @Test
    void retainLaterDeclarationInTheFirstPosition() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <!-- comment 1 -->
                  <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.13.1</version>
                  </dependency>
                  <!-- comment 2 -->
                  <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.13.2</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <!-- comment 1 -->
                  <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.13.2</version>
                  </dependency>
                  <!-- comment 2 -->
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void removeDuplicateDeclaredThroughProperty() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <properties>
                  <guava.version>29.0-jre</guava.version>
                </properties>

                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>${guava.version}</version>
                  </dependency>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <properties>
                  <guava.version>29.0-jre</guava.version>
                </properties>

                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>${guava.version}</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    /**
     * The surviving declaration is taken over whole, so it brings its own indentation with it and only the
     * indentation of the `<dependency>` tag itself is taken from the declaration it replaces. Duplicates that were
     * written at different indentation levels therefore need a formatting recipe afterwards.
     */
    @Test
    void retainLaterDeclarationIndentedDifferently() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>com.google.inject</groupId>
                    <artifactId>guice</artifactId>
                    <version>4.2.1</version>
                  </dependency>
                      <dependency>
                          <groupId>com.google.inject</groupId>
                          <artifactId>guice</artifactId>
                          <version>4.2.2</version>
                      </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                          <groupId>com.google.inject</groupId>
                          <artifactId>guice</artifactId>
                          <version>4.2.2</version>
                      </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void retainLaterDeclarationWhenAValueContainsAComment() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                    <optional>false<!-- not yet --></optional>
                  </dependency>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                    <optional>true<!-- since 1.2 --></optional>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                    <optional>true<!-- since 1.2 --></optional>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }
}
