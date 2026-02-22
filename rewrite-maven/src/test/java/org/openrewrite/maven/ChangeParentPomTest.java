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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedPom;

class ChangeParentPomTest implements RewriteTest {

    @DocumentExample
    @Test
    void changeParent() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.springframework.boot",
            "com.fasterxml.jackson",
            "spring-boot-starter-parent",
            "jackson-parent",
            "2.12",
            null,
            null,
            null,
            false,
            null
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>1.5.12.RELEASE</version>
                </parent>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <parent>
                  <groupId>com.fasterxml.jackson</groupId>
                  <artifactId>jackson-parent</artifactId>
                  <version>2.12</version>
                </parent>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
          )
        );
    }

    @Test
    void changeParentShouldResolveDependenciesManagementWithMavenProperties() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.jenkins-ci.plugins",
            "org.jenkins-ci.plugins",
            "plugin",
            "plugin",
            "5.3",
            null,
            null,
            null,
            false,
            null
          )),
          pomXml(
            """
              <project>
                  <parent>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>plugin</artifactId>
                      <version>4.86</version>
                      <relativePath/>
                  </parent>
                  <artifactId>example-plugin</artifactId>
                  <version>0.8-SNAPSHOT</version>
                  <properties>
                      <jenkins.baseline>2.462</jenkins.baseline>
                      <jenkins.version>${jenkins.baseline}.3</jenkins.version>
                  </properties>
                  <dependencyManagement>
                     <dependencies>
                         <dependency>
                             <groupId>io.jenkins.tools.bom</groupId>
                             <artifactId>bom-${jenkins.baseline}.x</artifactId>
                             <version>3722.vcc62e7311580</version>
                             <type>pom</type>
                             <scope>import</scope>
                         </dependency>
                     </dependencies>
                 </dependencyManagement>
                 <dependencies>
                     <dependency>
                         <groupId>org.jenkins-ci.plugins</groupId>
                         <artifactId>scm-api</artifactId>
                     </dependency>
                 </dependencies>
                  <repositories>
                      <repository>
                        <id>central</id>
                        <name>Central Repository</name>
                        <url>https://repo.maven.apache.org/maven2</url>
                      </repository>
                      <repository>
                          <id>repo.jenkins-ci.org</id>
                          <url>https://repo.jenkins-ci.org/public/</url>
                      </repository>
                  </repositories>
              </project>
              """,
            """
              <project>
                  <parent>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>plugin</artifactId>
                      <version>5.3</version>
                      <relativePath/>
                  </parent>
                  <artifactId>example-plugin</artifactId>
                  <version>0.8-SNAPSHOT</version>
                  <properties>
                      <jenkins.baseline>2.462</jenkins.baseline>
                      <jenkins.version>${jenkins.baseline}.3</jenkins.version>
                  </properties>
                  <dependencyManagement>
                     <dependencies>
                         <dependency>
                             <groupId>io.jenkins.tools.bom</groupId>
                             <artifactId>bom-${jenkins.baseline}.x</artifactId>
                             <version>3722.vcc62e7311580</version>
                             <type>pom</type>
                             <scope>import</scope>
                         </dependency>
                     </dependencies>
                 </dependencyManagement>
                 <dependencies>
                     <dependency>
                         <groupId>org.jenkins-ci.plugins</groupId>
                         <artifactId>scm-api</artifactId>
                     </dependency>
                 </dependencies>
                  <repositories>
                      <repository>
                        <id>central</id>
                        <name>Central Repository</name>
                        <url>https://repo.maven.apache.org/maven2</url>
                      </repository>
                      <repository>
                          <id>repo.jenkins-ci.org</id>
                          <url>https://repo.jenkins-ci.org/public/</url>
                      </repository>
                  </repositories>
              </project>
              """
          )
        );
    }

    @Test
    void changeParentWithRelativePath() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.springframework.boot",
            "com.fasterxml.jackson",
            "spring-boot-starter-parent",
            "jackson-parent",
            "2.12",
            "",
            "../../pom.xml",
            null,
            false,
            null
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>1.5.12.RELEASE</version>
                  <relativePath/>
                </parent>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <parent>
                  <groupId>com.fasterxml.jackson</groupId>
                  <artifactId>jackson-parent</artifactId>
                  <version>2.12</version>
                  <relativePath>../../pom.xml</relativePath>
                </parent>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
          )
        );
    }

    @Test
    void changeParentAddRelativePathEmptyValue() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.springframework.boot",
            "com.fasterxml.jackson",
            "spring-boot-starter-parent",
            "jackson-parent",
            "2.12",
            null,
            "",
            null,
            false,
            null
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>1.5.12.RELEASE</version>
                </parent>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <parent>
                  <groupId>com.fasterxml.jackson</groupId>
                  <artifactId>jackson-parent</artifactId>
                  <version>2.12</version>
                  <relativePath />
                </parent>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
          )
        );
    }

    @Test
    void changeParentAddRelativePathNonEmptyValue() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.springframework.boot",
            "com.fasterxml.jackson",
            "spring-boot-starter-parent",
            "jackson-parent",
            "2.12",
            null,
            "../pom.xml",
            null,
            false,
            null
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>1.5.12.RELEASE</version>
                </parent>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <parent>
                  <groupId>com.fasterxml.jackson</groupId>
                  <artifactId>jackson-parent</artifactId>
                  <version>2.12</version>
                  <relativePath>../pom.xml</relativePath>
                </parent>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
          )
        );
    }

    @RepeatedTest(10)
    void multiModuleRelativePath() {
        ChangeParentPom recipe = new ChangeParentPom(
          "org.springframework.boot",
          null,
          "spring-boot-starter-parent",
          null,
          "2.6.7",
          null,
          "",
          null,
          false,
          null);
        rewriteRun(
          spec -> spec.recipe(recipe),
          mavenProject("parent",
            pomXml(
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>

                  <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>2.5.0</version>
                  </parent>

                  <modules>
                    <module>module1</module>
                    <module>module2</module>
                  </modules>
                </project>
                """,
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>

                  <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>2.6.7</version>
                    <relativePath />
                  </parent>

                  <modules>
                    <module>module1</module>
                    <module>module2</module>
                  </modules>
                </project>
                """
            ),
            mavenProject("module1",
              pomXml(
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                      <groupId>org.sample</groupId>
                      <artifactId>sample</artifactId>
                      <version>1.0.0</version>
                    </parent>
                    <artifactId>module1</artifactId>
                  </project>
                  """,
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                      <groupId>org.sample</groupId>
                      <artifactId>sample</artifactId>
                      <version>1.0.0</version>
                    </parent>
                    <artifactId>module1</artifactId>
                  </project>
                  """,
                spec -> spec.afterRecipe(doc -> {
                    MavenResolutionResult mrr = doc.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                    MavenResolutionResult parentMrr = mrr.getParent();
                    assertThat(parentMrr).isNotNull();
                    assertThat(requireNonNull(parentMrr.getPom().getRequested().getParent()).getGav().getVersion())
                        .isEqualTo("2.6.7");
                })
              )),
            mavenProject("module2",
              pomXml(
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                      <groupId>org.sample</groupId>
                      <artifactId>sample</artifactId>
                      <version>1.0.0</version>
                    </parent>
                    <artifactId>module2</artifactId>
                  </project>
                  """,
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                      <groupId>org.sample</groupId>
                      <artifactId>sample</artifactId>
                      <version>1.0.0</version>
                    </parent>
                    <artifactId>module2</artifactId>
                  </project>
                  """,
                spec -> spec.afterRecipe(doc -> {
                    MavenResolutionResult mrr = doc.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                    MavenResolutionResult parentMrr = mrr.getParent();
                    assertThat(parentMrr).isNotNull();
                    assertThat(requireNonNull(parentMrr.getPom().getRequested().getParent()).getGav().getVersion())
                        .isEqualTo("2.6.7");
                })
              )
            )
          )
        );
    }

    @RepeatedTest(10)
    void multiModuleRelativePathChangeChildren() {
        ChangeParentPom recipe = new ChangeParentPom(
          "org.sample",
          "org.springframework.boot",
          "sample",
          "spring-boot-starter-parent",
          "2.5.0",
          null,
          "",
          null,
          true,
          null);
        rewriteRun(
          spec -> spec.recipe(recipe),
          mavenProject("parent",
            pomXml(
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>

                  <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>2.5.0</version>
                  </parent>

                  <modules>
                    <module>module1</module>
                    <module>module2</module>
                  </modules>
                </project>
                """
            ),
            mavenProject("module1",
              pomXml(
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                      <groupId>org.sample</groupId>
                      <artifactId>sample</artifactId>
                      <version>1.0.0</version>
                    </parent>
                    <artifactId>module1</artifactId>
                  </project>
                  """,
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-parent</artifactId>
                      <version>2.5.0</version>
                      <relativePath />
                    </parent>
                    <artifactId>module1</artifactId>
                  </project>
                  """
              )),
            mavenProject("module2",
              pomXml(
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                      <groupId>org.sample</groupId>
                      <artifactId>sample</artifactId>
                      <version>1.0.0</version>
                    </parent>
                    <artifactId>module2</artifactId>
                  </project>
                  """,
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-parent</artifactId>
                      <version>2.5.0</version>
                      <relativePath />
                    </parent>
                    <artifactId>module2</artifactId>
                  </project>
                  """
              )
            )
          )
        );
    }

    @CsvSource({
      "../pom.xml,'',,<relativePath />", // self-closing tag gets added
      "../pom.xml,,<relativePath>../pom.xml</relativePath>,<relativePath>../pom.xml</relativePath>", // no change, targetRelativePath is null
      "../pom.xml,../../pom.xml,,<relativePath>../../pom.xml</relativePath>", // tag gets added
      "../pom.xml,../../pom.xml,<relativePath>../pom.xml</relativePath>,<relativePath>../../pom.xml</relativePath>", // tag gets updated
      "../*,'',,<relativePath />", // self-closing tag gets added
      "../*,,<relativePath>../pom.xml</relativePath>,<relativePath>../pom.xml</relativePath>", // no change, targetRelativePath is null
      "../*,../../pom.xml,,<relativePath>../../pom.xml</relativePath>", // tag gets added
      "../*,../../pom.xml,<relativePath>../pom.xml</relativePath>,<relativePath>../../pom.xml</relativePath>", // tag gets updated
      "../../pom.xml,'',<relativePath>../../pom.xml</relativePath>,<relativePath />", // tag converted to self-closing
      "'','',<relativePath></relativePath>,<relativePath></relativePath>", // matches but new = old, left untouched
      "'','',<relativePath/>,<relativePath/>", // matches but new = old, left untouched
      "'','../pom.xml',<relativePath></relativePath>,<relativePath>../pom.xml</relativePath>", // tag gets updated
      "'','../pom.xml',<relativePath/>,<relativePath>../pom.xml</relativePath>", // tag gets expanded and updated
    })
    @ParameterizedTest
    void multiModuleChangeChildrenBasedOnRelativePath(String oldRelativePath, String newRelativePath, String oldRelativePathTag, String expectedNewRelativePathTag) {
        ChangeParentPom recipe = new ChangeParentPom(
          "org.sample", "org.springframework.boot",
          "sample", "spring-boot-starter-parent",
          "2.5.0",
          oldRelativePath, newRelativePath,
          null,
          true,
          null);
        rewriteRun(
          spec -> spec.recipe(recipe),
          mavenProject("parent",
            pomXml(
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>2.5.0</version>
                  </parent>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>

                  <modules>
                    <module>module1</module>
                    <module>module2</module>
                  </modules>
                </project>
                """
            ),
            mavenProject("module1",
              pomXml(
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                      <groupId>org.sample</groupId>
                      <artifactId>sample</artifactId>
                      <version>1.0.0</version>%s
                    </parent>
                    <artifactId>module1</artifactId>
                  </project>
                  """.formatted(StringUtils.isBlank(oldRelativePathTag) ? "" : "\n    " + oldRelativePathTag),
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-parent</artifactId>
                      <version>2.5.0</version>%s
                    </parent>
                    <artifactId>module1</artifactId>
                  </project>
                  """.formatted(StringUtils.isBlank(expectedNewRelativePathTag) ? "" : "\n    " + expectedNewRelativePathTag)
              )
            ),
            mavenProject("module2",
              // “control” module: same parent gav but different relativePath should not be affected
              pomXml(
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                      <groupId>org.sample</groupId>
                      <artifactId>sample</artifactId>
                      <version>1.0.0</version>
                      <relativePath>..</relativePath>
                    </parent>
                    <artifactId>module3</artifactId>
                  </project>
                  """
              )
            )
          )
        );
    }

    @CsvSource({
      "../pom.xml,<relativePath />", // self-closing means empty string
      "../pom.xml,<relativePath>../../pom.xml</relativePath>", // different value
      "..,,", // absent tag means '../pom.xml' not '..'
    })
    @ParameterizedTest
    void multiModuleRelativePathNotMatching(String oldRelativePath, String oldRelativePathTag) {
        ChangeParentPom recipe = new ChangeParentPom(
          "org.sample", "org.springframework.boot",
          "sample", "spring-boot-starter-parent",
          "2.5.0",
          oldRelativePath, "invalid",
          null,
          true,
          null);
        rewriteRun(
          spec -> spec.recipe(recipe),
          mavenProject("parent",
            pomXml(
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>2.5.0</version>
                  </parent>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>

                  <modules>
                    <module>module1</module>
                  </modules>
                </project>
                """
            ),
            mavenProject("module1",
              pomXml(
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                      <groupId>org.sample</groupId>
                      <artifactId>sample</artifactId>
                      <version>1.0.0</version>%s
                    </parent>
                    <artifactId>module1</artifactId>
                  </project>
                  """.formatted(StringUtils.isBlank(oldRelativePathTag) ? "" : "\n    " + oldRelativePathTag)
              )
            )
          )
        );
    }

    @Test
    void upgradeVersion() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.springframework.boot",
            null,
            "spring-boot-starter-parent",
            null,
            "~1.5",
            null,
            null,
            null,
            false,
            null
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>1.5.12.RELEASE</version>
                  <relativePath/> <!-- lookup parent from repository -->
                </parent>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>1.5.22.RELEASE</version>
                  <relativePath/> <!-- lookup parent from repository -->
                </parent>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
          )
        );
    }

    @Test
    void upgradeToExactVersion() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.springframework.boot",
            null,
            "spring-boot-starter-parent",
            null,
            "1.5.22.RELEASE",
            null,
            null,
            null,
            false,
            null
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>1.5.12.RELEASE</version>
                  <relativePath/> <!-- lookup parent from repository -->
                </parent>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>1.5.22.RELEASE</version>
                  <relativePath/> <!-- lookup parent from repository -->
                </parent>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
          )
        );
    }

    @Test
    void doNotDowngradeToLowerVersionWhenArtifactsAreTheSame() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.springframework.boot",
            null,
            "spring-boot-starter-parent",
            null,
            "1.5.12.RELEASE",
            null,
            null,
            null,
            false,
            null
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>1.5.22.RELEASE</version>
                  <relativePath/> <!-- lookup parent from repository -->
                </parent>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
          )
        );
    }

    @Test
    void downgradeToLowerVersionWhenFlagIsSet() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.springframework.boot",
            null,
            "spring-boot-starter-parent",
            null,
            "1.5.12.RELEASE",
            null,
            null,
            null,
            true,
            null
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>1.5.22.RELEASE</version>
                  <relativePath/> <!-- lookup parent from repository -->
                </parent>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>1.5.12.RELEASE</version>
                  <relativePath/> <!-- lookup parent from repository -->
                </parent>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
          )
        );
    }

    @Test
    void wildcardVersionUpdate() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "*",
            null,
            "*",
            null,
            "~1.5",
            null,
            null,
            null,
            false,
            null
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>1.5.12.RELEASE</version>
                  <relativePath/> <!-- lookup parent from repository -->
                </parent>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>1.5.22.RELEASE</version>
                  <relativePath/> <!-- lookup parent from repository -->
                </parent>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
          )
        );
    }

    @Test
    void removesRedundantExplicitVersionsMatchingOldParent() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.junit",
            null,
            "junit-bom",
            null,
            "5.9.1",
            null,
            null,
            null,
            false,
            null
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <parent>
                  <groupId>org.junit</groupId>
                  <artifactId>junit-bom</artifactId>
                  <version>5.9.0</version>
                  <relativePath/>
                </parent>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter-api</artifactId>
                    <version>5.9.0</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <parent>
                  <groupId>org.junit</groupId>
                  <artifactId>junit-bom</artifactId>
                  <version>5.9.1</version>
                  <relativePath/>
                </parent>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter-api</artifactId>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void removesRedundantExplicitVersionsMatchingNewParent() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.junit",
            null,
            "junit-bom",
            null,
            "5.9.1",
            null,
            null,
            null,
            false,
            null
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <parent>
                  <groupId>org.junit</groupId>
                  <artifactId>junit-bom</artifactId>
                  <version>5.9.0</version>
                  <relativePath/>
                </parent>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter-api</artifactId>
                    <version>5.9.1</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <parent>
                  <groupId>org.junit</groupId>
                  <artifactId>junit-bom</artifactId>
                  <version>5.9.1</version>
                  <relativePath/>
                </parent>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter-api</artifactId>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void takesNewVersionFromParent() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.junit",
            null,
            "junit-bom",
            null,
            "5.9.1",
            null,
            null,
            null,
            false,
            null
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <parent>
                  <groupId>org.junit</groupId>
                  <artifactId>junit-bom</artifactId>
                  <version>5.9.0</version>
                  <relativePath/>
                </parent>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter-api</artifactId>
                    <version>5.8.0</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <parent>
                  <groupId>org.junit</groupId>
                  <artifactId>junit-bom</artifactId>
                  <version>5.9.1</version>
                  <relativePath/>
                </parent>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter-api</artifactId>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void upgradeNonSemverVersion() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.springframework.cloud",
            null,
            "spring-cloud-starter-parent",
            null,
            "2021.0.5",
            null,
            null,
            null,
            false,
            null)),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>

                <parent>
                  <groupId>org.springframework.cloud</groupId>
                  <artifactId>spring-cloud-starter-parent</artifactId>
                  <version>Hoxton.SR12</version>
                </parent>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>

                <parent>
                  <groupId>org.springframework.cloud</groupId>
                  <artifactId>spring-cloud-starter-parent</artifactId>
                  <version>2021.0.5</version>
                </parent>
              </project>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2418")
    @Nested
    class RetainVersions {
        @DocumentExample
        @Test
        void dependencyWithExplicitVersionRemovedFromDepMgmt() {
            rewriteRun(
              spec -> spec.recipe(new ChangeParentPom(
                "org.springframework.cloud",
                null,
                "spring-cloud-config-dependencies",
                null,
                "3.1.4",
                null,
                null,
                null,
                null,
                null)),
              pomXml(
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>

                    <parent>
                      <groupId>org.springframework.cloud</groupId>
                      <artifactId>spring-cloud-config-dependencies</artifactId>
                      <version>3.1.2</version>
                    </parent>

                    <dependencies>
                      <dependency>
                        <groupId>com.jcraft</groupId>
                        <artifactId>jsch</artifactId>
                        <version>0.1.55</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """,
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>

                    <parent>
                      <groupId>org.springframework.cloud</groupId>
                      <artifactId>spring-cloud-config-dependencies</artifactId>
                      <version>3.1.4</version>
                    </parent>

                    <dependencies>
                      <dependency>
                        <groupId>com.jcraft</groupId>
                        <artifactId>jsch</artifactId>
                        <version>0.1.55</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """
              )
            );
        }

        @Test
        void preservesExplicitVersionIfNotRequested() {
            rewriteRun(
              spec -> spec.recipe(new ChangeParentPom(
                "org.springframework.cloud",
                null,
                "spring-cloud-dependencies",
                null,
                "2021.0.5",
                null,
                null,
                null,
                true,
                null)),
              pomXml(
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>

                    <parent>
                      <groupId>org.springframework.cloud</groupId>
                      <artifactId>spring-cloud-dependencies</artifactId>
                      <version>2020.0.1</version>
                    </parent>

                    <dependencies>
                      <dependency>
                        <groupId>org.springframework.cloud</groupId>
                        <artifactId>spring-cloud-schema-registry-core</artifactId>
                        <version>1.1.0</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """,
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>

                    <parent>
                      <groupId>org.springframework.cloud</groupId>
                      <artifactId>spring-cloud-dependencies</artifactId>
                      <version>2021.0.5</version>
                    </parent>

                    <dependencies>
                      <dependency>
                        <groupId>org.springframework.cloud</groupId>
                        <artifactId>spring-cloud-schema-registry-core</artifactId>
                        <version>1.1.0</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """
              )
            );
        }

        @Test
        void preservesOwnDefinedProperty() {
            rewriteRun(
              spec -> spec.recipe(new ChangeParentPom(
                "org.springframework.boot", "org.springframework.boot",
                "spring-boot-dependencies", "spring-boot-dependencies",
                "3.2.4",
                null, null, null, null, null)),
              pomXml(
                """
                  <project>
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>com.mycompany</groupId>
                      <artifactId>child</artifactId>
                      <version>1.0.0-SNAPSHOT</version>
                      <parent>
                          <groupId>org.springframework.boot</groupId>
                          <artifactId>spring-boot-dependencies</artifactId>
                          <version>2.4.0</version>
                      </parent>
                      <properties>
                          <servlet-api.version>4.0.0</servlet-api.version>
                      </properties>
                      <dependencies>
                        <dependency>
                          <groupId>javax.servlet</groupId>
                          <artifactId>javax.servlet-api</artifactId>
                          <version>${servlet-api.version}</version>
                        </dependency>
                      </dependencies>
                  </project>
                  """,
                """
                  <project>
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>com.mycompany</groupId>
                      <artifactId>child</artifactId>
                      <version>1.0.0-SNAPSHOT</version>
                      <parent>
                          <groupId>org.springframework.boot</groupId>
                          <artifactId>spring-boot-dependencies</artifactId>
                          <version>3.2.4</version>
                      </parent>
                      <properties>
                          <servlet-api.version>4.0.0</servlet-api.version>
                      </properties>
                      <dependencies>
                        <dependency>
                          <groupId>javax.servlet</groupId>
                          <artifactId>javax.servlet-api</artifactId>
                          <version>${servlet-api.version}</version>
                        </dependency>
                      </dependencies>
                  </project>
                  """
              )
            );
        }

        @Test
        void bringsDownRemovedProperty() {
            rewriteRun(
              spec -> spec.recipe(new ChangeParentPom(
                "org.springframework.boot", "org.springframework.boot",
                "spring-boot-dependencies", "spring-boot-dependencies",
                "3.2.4",
                null, null, null, null, null)),
              pomXml(
                """
                  <project>
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>com.mycompany</groupId>
                      <artifactId>child</artifactId>
                      <version>1.0.0-SNAPSHOT</version>
                      <parent>
                          <groupId>org.springframework.boot</groupId>
                          <artifactId>spring-boot-dependencies</artifactId>
                          <version>2.4.0</version>
                      </parent>
                      <dependencies>
                        <dependency>
                          <groupId>javax.servlet</groupId>
                          <artifactId>javax.servlet-api</artifactId>
                          <version>${servlet-api.version}</version>
                        </dependency>
                      </dependencies>
                  </project>
                  """,
                """
                  <project>
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>com.mycompany</groupId>
                      <artifactId>child</artifactId>
                      <version>1.0.0-SNAPSHOT</version>
                      <parent>
                          <groupId>org.springframework.boot</groupId>
                          <artifactId>spring-boot-dependencies</artifactId>
                          <version>3.2.4</version>
                      </parent>
                      <properties>
                          <servlet-api.version>4.0.1</servlet-api.version>
                      </properties>
                      <dependencies>
                        <dependency>
                          <groupId>javax.servlet</groupId>
                          <artifactId>javax.servlet-api</artifactId>
                          <version>${servlet-api.version}</version>
                        </dependency>
                      </dependencies>
                  </project>
                  """
              )
            );
        }

        @Test
        void doesNotBringInMavenArguments() {
            rewriteRun(
              spec -> spec.recipe(new ChangeParentPom(
                "org.springframework.boot",
                null,
                "spring-boot-starter-parent",
                null,
                "3.5.x",
                null,
                null,
                null,
                false,
                null
              )).parser(MavenParser.builder().property("maven.repo.local", "C:/blah")),
              pomXml(
                //language=xml
                """
                  <project>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    <properties>
                      <maven.repo.local>${user.home}/.m2/repository</maven.repo.local>
                      <owaspDb>${maven.repo.local}/blah</owaspDb>
                    </properties>
                    <parent>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-parent</artifactId>
                      <version>3.5.7</version>
                    </parent>
                  </project>
                  """,
                spec -> spec.after(actual ->
                    assertThat(actual)
                      .as("Does not bring in Maven arguments.")
                      .contains("<maven.repo.local>${user.home}/.m2/repository</maven.repo.local>")
                      .contains("<owaspDb>${maven.repo.local}/blah</owaspDb>")
                      .as("Any version of Spring boot parent above `3.5.7`")
                      .doesNotContain("3.5.7")
                      .containsPattern("<version>3\\.5\\.\\d+</version>")
                      .actual()
                )
              )
            );
        }

        @Test
        void bringsDownRemovedManagedVersion() {
            rewriteRun(
              spec -> spec.recipe(new ChangeParentPom(
                "org.springframework.boot", "org.springframework.boot",
                "spring-boot-dependencies", "spring-boot-dependencies",
                "3.2.4",
                null, null, null, null, null)),
              pomXml(
                """
                  <project>
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>com.mycompany</groupId>
                      <artifactId>child</artifactId>
                      <version>1.0.0-SNAPSHOT</version>
                      <parent>
                          <groupId>org.springframework.boot</groupId>
                          <artifactId>spring-boot-dependencies</artifactId>
                          <version>2.4.0</version>
                      </parent>
                      <dependencies>
                        <dependency>
                          <groupId>javax.servlet</groupId>
                          <artifactId>javax.servlet-api</artifactId>
                        </dependency>
                      </dependencies>
                  </project>
                  """,
                """
                  <project>
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>com.mycompany</groupId>
                      <artifactId>child</artifactId>
                      <version>1.0.0-SNAPSHOT</version>
                      <parent>
                          <groupId>org.springframework.boot</groupId>
                          <artifactId>spring-boot-dependencies</artifactId>
                          <version>3.2.4</version>
                      </parent>
                      <dependencyManagement>
                          <dependencies>
                              <dependency>
                                  <groupId>javax.servlet</groupId>
                                  <artifactId>javax.servlet-api</artifactId>
                                  <version>4.0.1</version>
                              </dependency>
                          </dependencies>
                      </dependencyManagement>
                      <dependencies>
                        <dependency>
                          <groupId>javax.servlet</groupId>
                          <artifactId>javax.servlet-api</artifactId>
                        </dependency>
                      </dependencies>
                  </project>
                  """
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite/issues/6770")
        @Test
        void bringsDownRemovedManagedVersionFromGrandparent() {
            rewriteRun(
              spec -> spec.recipe(new ChangeParentPom(
                "org.springframework.boot", "org.springframework.boot",
                "spring-boot-starter-parent", "spring-boot-starter-parent",
                "4.0.2",
                null, null, null, null, null)),
              pomXml(
                """
                  <project>
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>com.mycompany</groupId>
                      <artifactId>child</artifactId>
                      <version>1.0.0-SNAPSHOT</version>
                      <parent>
                          <groupId>org.springframework.boot</groupId>
                          <artifactId>spring-boot-starter-parent</artifactId>
                          <version>3.5.9</version>
                          <relativePath/>
                      </parent>
                      <dependencies>
                        <dependency>
                          <groupId>org.springframework.retry</groupId>
                          <artifactId>spring-retry</artifactId>
                        </dependency>
                      </dependencies>
                  </project>
                  """,
                """
                  <project>
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>com.mycompany</groupId>
                      <artifactId>child</artifactId>
                      <version>1.0.0-SNAPSHOT</version>
                      <parent>
                          <groupId>org.springframework.boot</groupId>
                          <artifactId>spring-boot-starter-parent</artifactId>
                          <version>4.0.2</version>
                          <relativePath/>
                      </parent>
                      <dependencyManagement>
                          <dependencies>
                              <dependency>
                                  <groupId>org.springframework.retry</groupId>
                                  <artifactId>spring-retry</artifactId>
                                  <version>2.0.12</version>
                              </dependency>
                          </dependencies>
                      </dependencyManagement>
                      <dependencies>
                        <dependency>
                          <groupId>org.springframework.retry</groupId>
                          <artifactId>spring-retry</artifactId>
                        </dependency>
                      </dependencies>
                  </project>
                  """
              )
            );
        }
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1753")
    @RepeatedTest(10)
    void multiModule() {
        ChangeParentPom recipe = new ChangeParentPom("org.springframework.boot",
          null,
          "spring-boot-starter-parent",
          null,
          "2.6.7",
          null,
          null,
          null,
          true,
          null);
        rewriteRun(
          spec -> spec.recipe(recipe),
          mavenProject("parent",
            mavenProject("module1",
              pomXml(
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                      <groupId>org.sample</groupId>
                      <artifactId>sample</artifactId>
                      <version>1.0.0</version>
                    </parent>
                    <artifactId>module1</artifactId>
                  </project>
                  """,
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                      <groupId>org.sample</groupId>
                      <artifactId>sample</artifactId>
                      <version>1.0.0</version>
                    </parent>
                    <artifactId>module1</artifactId>
                  </project>
                  """,
                spec -> spec.afterRecipe(doc -> {
                    MavenResolutionResult mrr = doc.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                    MavenResolutionResult parentMrr = mrr.getParent();
                    assertThat(parentMrr).isNotNull();
                    assertThat(requireNonNull(parentMrr.getPom().getRequested().getParent()).getVersion())
                        .isEqualTo("2.6.7");
                })
              )),
            mavenProject("module2",
              pomXml(
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                      <groupId>org.sample</groupId>
                      <artifactId>sample</artifactId>
                      <version>1.0.0</version>
                    </parent>
                    <artifactId>module2</artifactId>
                  </project>
                  """,
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                      <groupId>org.sample</groupId>
                      <artifactId>sample</artifactId>
                      <version>1.0.0</version>
                    </parent>
                    <artifactId>module2</artifactId>
                  </project>
                  """,
                spec -> spec.afterRecipe(doc -> {
                    MavenResolutionResult mrr = doc.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                    MavenResolutionResult parentMrr = mrr.getParent();
                    assertThat(parentMrr).isNotNull();
                    assertThat(requireNonNull(parentMrr.getPom().getRequested().getParent()).getGav().getVersion())
                        .isEqualTo("2.6.7");
                })
              )
            ),
            pomXml(
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>

                  <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>2.5.0</version>
                  </parent>

                  <modules>
                    <module>module1</module>
                    <module>module2</module>
                  </modules>
                </project>
                """,
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>

                  <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>2.6.7</version>
                  </parent>

                  <modules>
                    <module>module1</module>
                    <module>module2</module>
                  </modules>
                </project>
                """
            )
          )
        );
    }

    @Test
    void changeParentToSameVersion() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.springframework.boot",
            null,
            "spring-boot-dependencies",
            "spring-boot-starter-parent",
            "latest.patch",
            null,
            null,
            null,
            null,
            null)),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>

                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-dependencies</artifactId>
                  <version>2.7.18</version>
                </parent>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>

                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>2.7.18</version>
                </parent>
              </project>
              """
          )
        );
    }

    @Test
    void doNotAddUnnecessaryManagedVersion() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.springframework.boot",
            "org.springframework.boot",
            "spring-boot-starter-parent",
            "spring-boot-starter-parent",
            "2.3.12.RELEASE",
            null,
            null,
            null,
            null,
            null)),
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-parent</artifactId>
                      <version>2.2.13.RELEASE</version>
                  </parent>
                  <groupId>com.example</groupId>
                  <artifactId>acme</artifactId>
                  <version>0.0.1-SNAPSHOT</version>
                  <dependencies>
                      <dependency>
                          <groupId>org.springframework.boot</groupId>
                          <artifactId>spring-boot-starter-web</artifactId>
                      </dependency>
                  </dependencies>
              </project>
              """,
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-parent</artifactId>
                      <version>2.3.12.RELEASE</version>
                  </parent>
                  <groupId>com.example</groupId>
                  <artifactId>acme</artifactId>
                  <version>0.0.1-SNAPSHOT</version>
                  <dependencies>
                      <dependency>
                          <groupId>org.springframework.boot</groupId>
                          <artifactId>spring-boot-starter-web</artifactId>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void shouldNotAddToDependencyManagement() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.jenkins-ci.plugins",
            "org.jenkins-ci.plugins",
            "plugin",
            "plugin",
            "4.81",
            null,
            null,
            null,
            null,
            null)),
          // language=xml
          pomXml(
            """
              <project>
                  <artifactId>example</artifactId>
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>plugin</artifactId>
                      <version>4.75</version>
                      <relativePath/>
                  </parent>
                  <properties>
                      <jenkins.version>2.387.3</jenkins.version>
                  </properties>
                  <repositories>
                    <repository>
                        <id>central</id>
                        <name>Central Repository</name>
                        <url>https://repo.maven.apache.org/maven2</url>
                    </repository>
                      <repository>
                          <id>repo.jenkins-ci.org</id>
                          <url>https://repo.jenkins-ci.org/public/</url>
                      </repository>
                  </repositories>
                  <pluginRepositories>
                      <pluginRepository>
                          <id>repo.jenkins-ci.org</id>
                          <url>https://repo.jenkins-ci.org/public/</url>
                      </pluginRepository>
                  </pluginRepositories>
                  <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>io.jenkins.tools.bom</groupId>
                            <artifactId>bom-2.387.x</artifactId>
                            <version>2516.v113cb_3d00317</version>
                            <type>pom</type>
                            <scope>import</scope>
                        </dependency>
                    </dependencies>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>org.jenkins-ci.plugins</groupId>
                          <artifactId>junit</artifactId>
                      </dependency>
                  </dependencies>
              </project>
              """,
            """
              <project>
                  <artifactId>example</artifactId>
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>plugin</artifactId>
                      <version>4.81</version>
                      <relativePath/>
                  </parent>
                  <properties>
                      <jenkins.version>2.387.3</jenkins.version>
                  </properties>
                  <repositories>
                    <repository>
                        <id>central</id>
                        <name>Central Repository</name>
                        <url>https://repo.maven.apache.org/maven2</url>
                    </repository>
                      <repository>
                          <id>repo.jenkins-ci.org</id>
                          <url>https://repo.jenkins-ci.org/public/</url>
                      </repository>
                  </repositories>
                  <pluginRepositories>
                      <pluginRepository>
                          <id>repo.jenkins-ci.org</id>
                          <url>https://repo.jenkins-ci.org/public/</url>
                      </pluginRepository>
                  </pluginRepositories>
                  <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>io.jenkins.tools.bom</groupId>
                            <artifactId>bom-2.387.x</artifactId>
                            <version>2516.v113cb_3d00317</version>
                            <type>pom</type>
                            <scope>import</scope>
                        </dependency>
                    </dependencies>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>org.jenkins-ci.plugins</groupId>
                          <artifactId>junit</artifactId>
                      </dependency>
                  </dependencies>
              </project>
              """
          ));
    }

    @Test
    void doesNotAddGrandparentProperties() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.springframework.boot",
            null,
            "spring-boot-starter-parent",
            null,
            "2.7.18",
            null,
            null,
            null,
            null,
            null)),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>

                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>2.7.17</version>
                </parent>

                <properties>
                  <my-cool-prop>${junit.version}</my-cool-prop>
                </properties>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>

                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>2.7.18</version>
                </parent>

                <properties>
                  <my-cool-prop>${junit.version}</my-cool-prop>
                </properties>
              </project>
              """
          )
        );
    }

    @Test
    void doesNotAddGlobalProperties() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.springframework.boot",
            null,
            "spring-boot-starter-parent",
            null,
            "2.7.18",
            null,
            null,
            null,
            null,
            null)),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>

                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>2.7.17</version>
                </parent>

                <properties>
                  <my-basedir>${basedir}</my-basedir>
                  <my-project-basedir>${project.basedir}</my-project-basedir>
                  <my-project-build-directory>${project.build.directory}</my-project-build-directory>
                  <my-project-version>${project.version}</my-project-version>
                  <my-env-prop>${env.GIT_HOME}</my-env-prop>
                  <my-settings-prop>${settings.offline}</my-settings-prop>
                </properties>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>

                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>2.7.18</version>
                </parent>

                <properties>
                  <my-basedir>${basedir}</my-basedir>
                  <my-project-basedir>${project.basedir}</my-project-basedir>
                  <my-project-build-directory>${project.build.directory}</my-project-build-directory>
                  <my-project-version>${project.version}</my-project-version>
                  <my-env-prop>${env.GIT_HOME}</my-env-prop>
                  <my-settings-prop>${settings.offline}</my-settings-prop>
                </properties>
              </project>
              """
          )
        );
    }

    @Test
    void changeParentWithExceptDependencies() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.springframework.boot",
            "org.springframework.boot",
            "spring-boot-starter-parent",
            "spring-boot-starter-parent",
            "2.7.18",
            null,
            null,
            null,
            false,
            List.of("com.fasterxml.jackson.core:jackson-annotations")
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>2.7.17</version>
                </parent>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1.0.0</version>
                <dependencies>
                  <!-- This dependency is in except list and should keep its version -->
                  <dependency>
                    <groupId>com.fasterxml.jackson.core</groupId>
                    <artifactId>jackson-annotations</artifactId>
                    <version>2.13.5</version>
                  </dependency>
                  <!-- This dependency is not in except list and should have version removed -->
                  <dependency>
                    <groupId>org.springframework</groupId>
                    <artifactId>spring-context</artifactId>
                    <version>5.3.23</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>2.7.18</version>
                </parent>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1.0.0</version>
                <dependencies>
                  <!-- This dependency is in except list and should keep its version -->
                  <dependency>
                    <groupId>com.fasterxml.jackson.core</groupId>
                    <artifactId>jackson-annotations</artifactId>
                    <version>2.13.5</version>
                  </dependency>
                  <!-- This dependency is not in except list and should have version removed -->
                  <dependency>
                    <groupId>org.springframework</groupId>
                    <artifactId>spring-context</artifactId>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/6463")
    @Test
    void multiModuleDownstreamMavenResolutionResultUpdated() {
        // This test verifies that when the root pom's parent is upgraded,
        // the child modules' MavenResolutionResult markers are also updated
        // to reflect the new parent information
        ChangeParentPom recipe = new ChangeParentPom(
          "org.springframework.boot",
          null,
          "spring-boot-starter-parent",
          null,
          "2.6.7",
          null,
          null,
          null,
          true,
          null);
        rewriteRun(
          spec -> spec.recipe(recipe),
          mavenProject("parent",
            pomXml(
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>

                  <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>2.5.0</version>
                  </parent>

                  <modules>
                    <module>module1</module>
                  </modules>
                </project>
                """,
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>

                  <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>2.6.7</version>
                  </parent>

                  <modules>
                    <module>module1</module>
                  </modules>
                </project>
                """
            ),
            mavenProject("module1",
              pomXml(
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                      <groupId>org.sample</groupId>
                      <artifactId>sample</artifactId>
                      <version>1.0.0</version>
                    </parent>
                    <artifactId>module1</artifactId>
                  </project>
                  """,
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                      <groupId>org.sample</groupId>
                      <artifactId>sample</artifactId>
                      <version>1.0.0</version>
                    </parent>
                    <artifactId>module1</artifactId>
                  </project>
                  """,
                spec -> spec.afterRecipe(doc -> {
                    // Verify that the child module's MavenResolutionResult has updated parent information
                    MavenResolutionResult mrr = doc.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();

                    // The child module's parent should be the root pom (org.sample:sample:1.0.0)
                    MavenResolutionResult parentMrr = mrr.getParent();
                    assertThat(parentMrr).isNotNull();
                    assertThat(parentMrr.getPom().getGav().getGroupId()).isEqualTo("org.sample");
                    assertThat(parentMrr.getPom().getGav().getArtifactId()).isEqualTo("sample");

                    // The root pom's parent (grandparent of module1) should be the upgraded version
                    ResolvedPom parentPom = parentMrr.getPom();
                    assertThat(parentPom.getRequested().getParent()).isNotNull();
                    assertThat(parentPom.getRequested().getParent().getGav().getGroupId())
                      .isEqualTo("org.springframework.boot");
                    assertThat(parentPom.getRequested().getParent().getGav().getArtifactId())
                      .isEqualTo("spring-boot-starter-parent");
                    // This assertion checks that the parent version was properly updated
                    // in the child module's MavenResolutionResult
                    assertThat(parentPom.getRequested().getParent().getGav().getVersion())
                      .describedAs("Child module's MavenResolutionResult should reflect the updated parent version")
                      .isEqualTo("2.6.7");
                })
              )
            )
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/6463")
    @RepeatedTest(10)
    void multiModuleChainOfParentPoms() {
        // This test verifies that marker updates propagate through a chain of parent poms:
        // - root pom has spring-boot-starter-parent as its parent (upgraded from 2.5.0 to 2.6.7)
        // - intermediate module has root pom as its parent
        // - leaf module has intermediate module as its parent
        // All modules should have their MavenResolutionResult markers updated
        ChangeParentPom recipe = new ChangeParentPom(
          "org.springframework.boot",
          null,
          "spring-boot-starter-parent",
          null,
          "2.6.7",
          null,
          null,
          null,
          true,
          null);
        rewriteRun(
          spec -> spec.recipe(recipe),
          mavenProject("root",
            pomXml(
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>root</artifactId>
                  <version>1.0.0</version>

                  <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>2.5.0</version>
                  </parent>

                  <modules>
                    <module>intermediate</module>
                  </modules>
                </project>
                """,
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>root</artifactId>
                  <version>1.0.0</version>

                  <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>2.6.7</version>
                  </parent>

                  <modules>
                    <module>intermediate</module>
                  </modules>
                </project>
                """
            ),
            mavenProject("intermediate",
              pomXml(
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                      <groupId>org.sample</groupId>
                      <artifactId>root</artifactId>
                      <version>1.0.0</version>
                    </parent>
                    <artifactId>intermediate</artifactId>

                    <modules>
                      <module>leaf</module>
                    </modules>
                  </project>
                  """,
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                      <groupId>org.sample</groupId>
                      <artifactId>root</artifactId>
                      <version>1.0.0</version>
                    </parent>
                    <artifactId>intermediate</artifactId>

                    <modules>
                      <module>leaf</module>
                    </modules>
                  </project>
                  """,
                spec -> spec.afterRecipe(doc -> {
                    // Verify intermediate module's parent (root) has updated grandparent info
                    MavenResolutionResult mrr = doc.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                    MavenResolutionResult parentMrr = mrr.getParent();
                    assertThat(parentMrr).isNotNull();
                    assertThat(parentMrr.getPom().getGav().getArtifactId()).isEqualTo("root");
                    // The root pom's parent should be the upgraded spring-boot-starter-parent
                    assertThat(requireNonNull(parentMrr.getPom().getRequested().getParent()).getGav().getVersion())
                      .describedAs("Intermediate module's parent marker should reflect updated grandparent version")
                      .isEqualTo("2.6.7");
                })
              ),
              mavenProject("leaf",
                pomXml(
                  """
                    <project>
                      <modelVersion>4.0.0</modelVersion>
                      <parent>
                        <groupId>org.sample</groupId>
                        <artifactId>intermediate</artifactId>
                        <version>1.0.0</version>
                      </parent>
                      <artifactId>leaf</artifactId>
                    </project>
                    """,
                  """
                    <project>
                      <modelVersion>4.0.0</modelVersion>
                      <parent>
                        <groupId>org.sample</groupId>
                        <artifactId>intermediate</artifactId>
                        <version>1.0.0</version>
                      </parent>
                      <artifactId>leaf</artifactId>
                    </project>
                    """,
                  spec -> spec.afterRecipe(doc -> {
                      // Verify the leaf module has the entire updated hierarchy
                      MavenResolutionResult mrr = doc.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();

                      // Leaf's parent is intermediate
                      MavenResolutionResult intermediateMrr = mrr.getParent();
                      assertThat(intermediateMrr).isNotNull();
                      assertThat(intermediateMrr.getPom().getGav().getArtifactId()).isEqualTo("intermediate");

                      // Intermediate's parent is root
                      MavenResolutionResult rootMrr = intermediateMrr.getParent();
                      assertThat(rootMrr).isNotNull();
                      assertThat(rootMrr.getPom().getGav().getArtifactId()).isEqualTo("root");

                      // Root's parent should be the upgraded spring-boot-starter-parent
                      assertThat(requireNonNull(rootMrr.getPom().getRequested().getParent()).getGav().getVersion())
                        .describedAs("Leaf module's grandparent marker should reflect updated great-grandparent version")
                        .isEqualTo("2.6.7");
                  })
                )
              )
            )
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/6717")
    @Test
    void submoduleParentModelDependencyManagementUpdated() {
        rewriteRun(
            spec -> spec.recipe(new ChangeParentPom(
                "org.junit",
                null,
                "junit-bom",
                null,
                "6.0.1",
                null,
                null,
                null,
                false,
                null
            )),
            mavenProject(
                "sample-parent",
                pomXml(
                    """
                    <project>
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>com.example</groupId>
                      <artifactId>sample-parent</artifactId>
                      <version>${revision}</version>
                      <packaging>pom</packaging>

                      <modules>
                        <module>sample-rest</module>
                      </modules>

                      <parent>
                        <groupId>org.junit</groupId>
                        <artifactId>junit-bom</artifactId>
                        <version>5.10.3</version>
                        <relativePath/>
                      </parent>
                    </project>
                    """,
                    """
                    <project>
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>com.example</groupId>
                      <artifactId>sample-parent</artifactId>
                      <version>${revision}</version>
                      <packaging>pom</packaging>

                      <modules>
                        <module>sample-rest</module>
                      </modules>

                      <parent>
                        <groupId>org.junit</groupId>
                        <artifactId>junit-bom</artifactId>
                        <version>6.0.1</version>
                        <relativePath/>
                      </parent>
                    </project>
                    """,
                    spec -> spec.path("pom.xml")
                ),
                mavenProject(
                    "sample-rest",
                    pomXml(
                        """
                        <project>
                          <modelVersion>4.0.0</modelVersion>
                          <artifactId>sample-rest</artifactId>
                          <packaging>jar</packaging>

                          <parent>
                            <groupId>com.example</groupId>
                            <artifactId>sample-parent</artifactId>
                            <version>${revision}</version>
                            <relativePath>../pom.xml</relativePath>
                          </parent>
                        </project>
                        """,
                        """
                        <project>
                          <modelVersion>4.0.0</modelVersion>
                          <artifactId>sample-rest</artifactId>
                          <packaging>jar</packaging>

                          <parent>
                            <groupId>com.example</groupId>
                            <artifactId>sample-parent</artifactId>
                            <version>${revision}</version>
                            <relativePath>../pom.xml</relativePath>
                          </parent>
                        </project>
                        """,
                        spec -> spec.path("sample-rest/pom.xml").afterRecipe(doc -> {
                            MavenResolutionResult result = doc.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                            assertThat(result.getParent()).isNotNull();
                            assertThat(requireNonNull(result.getParent().getPom().getRequested().getParent()).getVersion())
                                .describedAs("Parent's parent version should be updated to 6.0.1")
                                .isEqualTo("6.0.1");
                            assertThat(result.getParent().getPom().getDependencyManagement())
                                .describedAs("Parent's dependency management should not contain old junit 5.10.3 entries")
                                .noneMatch(dep -> dep.getGav().toString().equals("org.junit.jupiter:junit-jupiter:5.10.3"));
                            assertThat(result.getParent().getPom().getDependencyManagement())
                                .describedAs("Parent's dependency management should contain new junit 6.0.1 entries")
                                .anyMatch(dep -> dep.getGav().toString().equals("org.junit.jupiter:junit-jupiter:6.0.1"));
                        })
                    )
                )
            )
        );
    }
}
