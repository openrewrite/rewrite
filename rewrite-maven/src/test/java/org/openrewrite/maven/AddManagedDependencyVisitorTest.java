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
import org.openrewrite.ExecutionContext;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.xml.tree.Xml;

import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.test.RewriteTest.toRecipe;

class AddManagedDependencyVisitorTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new AddManagedDependencyVisitor("org.apache.logging.log4j", "log4j-bom", "2.17.2",
          "import","pom",null, null)));
    }

    @DocumentExample
    @Test
    void newDependencyManagementTag() {
        rewriteRun(
          pomXml(
            """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
            </project>
            """,
            """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>org.apache.logging.log4j</groupId>
                            <artifactId>log4j-bom</artifactId>
                            <version>2.17.2</version>
                            <type>pom</type>
                            <scope>import</scope>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
            </project>
            """
          )
        );
    }

    @Test
    void alreadyExists() {
        rewriteRun(
          pomXml(
            """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>org.apache.logging.log4j</groupId>
                            <artifactId>log4j-bom</artifactId>
                            <version>2.17.0</version>
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
    void dependencyManagementTagExists() {
        rewriteRun(
          pomXml(
            """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencyManagement/>
            </project>
            """,
            """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>org.apache.logging.log4j</groupId>
                            <artifactId>log4j-bom</artifactId>
                            <version>2.17.2</version>
                            <type>pom</type>
                            <scope>import</scope>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
            </project>
            """
          )
        );
    }

    @Test
    void multipleDependenciesWithBecauseComments() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new MavenIsoVisitor<>() {
              @Override
              public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                  doAfterVisit(new AddManagedDependencyVisitor("org.apache.commons", "commons-compress", "1.26.0",
                    null, null, null, "CVE-2023-42503"));
                  doAfterVisit(new AddManagedDependencyVisitor("org.jetbrains.kotlin", "kotlin-stdlib", "1.6.21",
                    null, null, null, "CVE-2022-24329"));
                  doAfterVisit(new AddManagedDependencyVisitor("org.xerial.snappy", "snappy-java", "1.1.10.4",
                    null, null, null, "CVE-2023-34455"));
                  return document;
              }
          })),
          pomXml(
            """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
            </project>
            """,
            """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencyManagement>
                    <dependencies>
                        <!-- CVE-2023-42503 -->
                        <dependency>
                            <groupId>org.apache.commons</groupId>
                            <artifactId>commons-compress</artifactId>
                            <version>1.26.0</version>
                        </dependency>
                        <!-- CVE-2022-24329 -->
                        <dependency>
                            <groupId>org.jetbrains.kotlin</groupId>
                            <artifactId>kotlin-stdlib</artifactId>
                            <version>1.6.21</version>
                        </dependency>
                        <!-- CVE-2023-34455 -->
                        <dependency>
                            <groupId>org.xerial.snappy</groupId>
                            <artifactId>snappy-java</artifactId>
                            <version>1.1.10.4</version>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
            </project>
            """
          )
        );
    }

    @Test
    void multipleDependenciesWithBecauseCommentsSortedAlphabetically() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new MavenIsoVisitor<>() {
              @Override
              public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                  doAfterVisit(new AddManagedDependencyVisitor("org.xerial.snappy", "snappy-java", "1.1.10.4",
                    null, null, null, "CVE-2023-34455"));
                  doAfterVisit(new AddManagedDependencyVisitor("org.apache.commons", "commons-compress", "1.26.0",
                    null, null, null, "CVE-2023-42503"));
                  doAfterVisit(new AddManagedDependencyVisitor("org.jetbrains.kotlin", "kotlin-stdlib", "1.6.21",
                    null, null, null, "CVE-2022-24329"));
                  return document;
              }
          })),
          pomXml(
            """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
            </project>
            """,
            """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencyManagement>
                    <dependencies>
                        <!-- CVE-2023-42503 -->
                        <dependency>
                            <groupId>org.apache.commons</groupId>
                            <artifactId>commons-compress</artifactId>
                            <version>1.26.0</version>
                        </dependency>
                        <!-- CVE-2022-24329 -->
                        <dependency>
                            <groupId>org.jetbrains.kotlin</groupId>
                            <artifactId>kotlin-stdlib</artifactId>
                            <version>1.6.21</version>
                        </dependency>
                        <!-- CVE-2023-34455 -->
                        <dependency>
                            <groupId>org.xerial.snappy</groupId>
                            <artifactId>snappy-java</artifactId>
                            <version>1.1.10.4</version>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
            </project>
            """
          )
        );
    }
}
