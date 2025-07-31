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
package org.openrewrite.maven.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.maven.table.ParentPomsInUse;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class ParentPomInsightTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ParentPomInsight("org.springframework.boot", "spring-boot-starter-parent", null, null));
    }

    @DocumentExample
    @Test
    void findParent() {
        rewriteRun(
          spec -> spec.dataTable(ParentPomsInUse.Row.class, rows -> assertThat(rows)
            .singleElement()
            .satisfies(row -> {
                  assertThat(row.getProjectArtifactId()).isEqualTo("demo");
                  assertThat(row.getGroupId()).isEqualTo("org.springframework.boot");
                  assertThat(row.getArtifactId()).isEqualTo("spring-boot-starter-parent");
                  assertThat(row.getVersion()).isEqualTo("3.1.4");
                  assertThat(row.getRelativePath()).isNull();
              }
            )
          ),
          mavenProject("demo",
            pomXml(
              """
                <project>
                	<modelVersion>4.0.0</modelVersion>
                	<parent>
                		<groupId>org.springframework.boot</groupId>
                		<artifactId>spring-boot-starter-parent</artifactId>
                		<version>3.1.4</version>
                		<relativePath/> <!-- lookup parent from repository -->
                	</parent>
                	<groupId>com.example</groupId>
                	<artifactId>demo</artifactId>
                	<version>0.0.1-SNAPSHOT</version>
                </project>
                """,
              """
                <project>
                	<modelVersion>4.0.0</modelVersion>
                	<!--~~>--><parent>
                		<groupId>org.springframework.boot</groupId>
                		<artifactId>spring-boot-starter-parent</artifactId>
                		<version>3.1.4</version>
                		<relativePath/> <!-- lookup parent from repository -->
                	</parent>
                	<groupId>com.example</groupId>
                	<artifactId>demo</artifactId>
                	<version>0.0.1-SNAPSHOT</version>
                </project>
                """
            )
          )
        );
    }

    @Test
    void noParent() {
        rewriteRun(
          pomXml(
            //language=xml
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
    void multiModuleOnlyRoot() {
        rewriteRun(
          spec -> spec
            .recipe(new ParentPomInsight("*", "*", null, null))
            .dataTableAsCsv(ParentPomsInUse.class.getName(), """
              projectArtifactId,groupId,artifactId,version,relativePath
              sample,org.springframework.boot,"spring-boot-starter-parent",2.5.0,
              module1,org.sample,sample,1.0.0,../
              module2,org.sample,sample,1.0.0,../
              """),
          mavenProject("sample",
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

                  <!--~~>--><parent>
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
                      <relativePath>../</relativePath>
                    </parent>
                    <artifactId>module1</artifactId>
                  </project>
                  """,
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <!--~~>--><parent>
                      <groupId>org.sample</groupId>
                      <artifactId>sample</artifactId>
                      <version>1.0.0</version>
                      <relativePath>../</relativePath>
                    </parent>
                    <artifactId>module1</artifactId>
                  </project>
                  """,
                spec -> spec.path("module1/pom.xml")
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
                      <relativePath>../</relativePath>
                    </parent>
                    <artifactId>module2</artifactId>
                  </project>
                  """,
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <!--~~>--><parent>
                      <groupId>org.sample</groupId>
                      <artifactId>sample</artifactId>
                      <version>1.0.0</version>
                      <relativePath>../</relativePath>
                    </parent>
                    <artifactId>module2</artifactId>
                  </project>
                  """,
                spec -> spec.path("module2/pom.xml")
              )
            )
          )
        );
    }

    @Test
    void ancestorMatchesVersion() {
        rewriteRun(
          spec -> spec
            .recipe(new ParentPomInsight("*", "*", "2.5.0", null))
            .dataTableAsCsv(ParentPomsInUse.class.getName(), """
              projectArtifactId,groupId,artifactId,version,relativePath
              sample,org.springframework.boot,"spring-boot-starter-parent",2.5.0,
              module1,org.springframework.boot,"spring-boot-starter-parent",2.5.0,
              module2,org.springframework.boot,"spring-boot-starter-parent",2.5.0,
              """),
          mavenProject("sample",
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

                  <!--~~>--><parent>
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
                      <relativePath>../</relativePath>
                    </parent>
                    <artifactId>module1</artifactId>
                  </project>
                  """,
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <!--~~>--><parent>
                      <groupId>org.sample</groupId>
                      <artifactId>sample</artifactId>
                      <version>1.0.0</version>
                      <relativePath>../</relativePath>
                    </parent>
                    <artifactId>module1</artifactId>
                  </project>
                  """,
                spec -> spec.path("module1/pom.xml")
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
                      <relativePath>../</relativePath>
                    </parent>
                    <artifactId>module2</artifactId>
                  </project>
                  """,
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <!--~~>--><parent>
                      <groupId>org.sample</groupId>
                      <artifactId>sample</artifactId>
                      <version>1.0.0</version>
                      <relativePath>../</relativePath>
                    </parent>
                    <artifactId>module2</artifactId>
                  </project>
                  """,
                spec -> spec.path("module2/pom.xml")
              )
            )
          )
        );
    }

    @Test
    void matchNonSnapshot() {
        rewriteRun(
          spec -> spec
            .recipe(new ParentPomInsight("*", "*", "~2", false))
            .dataTableAsCsv(ParentPomsInUse.class.getName(), """
              projectArtifactId,groupId,artifactId,version,relativePath
              sample,org.springframework.boot,"spring-boot-starter-parent",2.5.0,
              """),
          mavenProject("sample",
            pomXml(
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0-SNAPSHOT</version>

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
                  <version>1.0.0-SNAPSHOT</version>

                  <!--~~>--><parent>
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
                      <version>1.0.0-SNAPSHOT</version>
                      <relativePath>../</relativePath>
                    </parent>
                    <artifactId>module1</artifactId>
                  </project>
                  """,
                spec -> spec.path("module1/pom.xml")
              )),
            mavenProject("module2",
              pomXml(
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                      <groupId>org.sample</groupId>
                      <artifactId>sample</artifactId>
                      <version>1.0.0-SNAPSHOT</version>
                      <relativePath>../</relativePath>
                    </parent>
                    <artifactId>module2</artifactId>
                  </project>
                  """,
                spec -> spec.path("module2/pom.xml")
              )
            )
          )
        );
    }

    @Test
    void directParentMatchesFullGAV() {
        rewriteRun(
          spec -> {
              spec.recipe(new ParentPomInsight("org.springframework.boot", "spring-boot-starter-parent", "3.3.3", null));
              spec.dataTable(ParentPomsInUse.Row.class, rows -> assertThat(rows)
                .singleElement()
                .satisfies(row -> {
                      assertThat(row.getProjectArtifactId()).isEqualTo("my-app");
                      assertThat(row.getGroupId()).isEqualTo("org.springframework.boot");
                      assertThat(row.getArtifactId()).isEqualTo("spring-boot-starter-parent");
                      assertThat(row.getVersion()).isEqualTo("3.3.3");
                      assertThat(row.getRelativePath()).isNull();
                  }
                ));
          },
          pomXml(
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>

              <parent>
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
              </parent>

              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              </project>
              """,
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>

              <!--~~>--><parent>
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
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
    void directParentMatchesGAVMinorVersion() {
        rewriteRun(
          spec -> {
              spec.recipe(new ParentPomInsight("org.springframework.boot", "spring-boot-starter-parent", "3.3.x", null));
              spec.dataTable(ParentPomsInUse.Row.class, rows -> assertThat(rows)
                .singleElement()
                .satisfies(row -> {
                      assertThat(row.getProjectArtifactId()).isEqualTo("my-app");
                      assertThat(row.getGroupId()).isEqualTo("org.springframework.boot");
                      assertThat(row.getArtifactId()).isEqualTo("spring-boot-starter-parent");
                      assertThat(row.getVersion()).isEqualTo("3.3.3");
                      assertThat(row.getRelativePath()).isNull();
                  }
                ));
          },
          pomXml(
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>

              <parent>
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
              </parent>

              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              </project>
              """,
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>

              <!--~~>--><parent>
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
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
    void directParentMatchesGroupIdGlob() {
        rewriteRun(
          spec -> {
              spec.recipe(new ParentPomInsight("org.springframework.*", "spring-boot-starter-parent", null, null));
              spec.dataTable(ParentPomsInUse.Row.class, rows -> assertThat(rows)
                .singleElement()
                .satisfies(row -> {
                      assertThat(row.getProjectArtifactId()).isEqualTo("my-app");
                      assertThat(row.getGroupId()).isEqualTo("org.springframework.boot");
                      assertThat(row.getArtifactId()).isEqualTo("spring-boot-starter-parent");
                      assertThat(row.getVersion()).isEqualTo("3.3.3");
                      assertThat(row.getRelativePath()).isNull();
                  }
                ));
          },
          pomXml(
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>

              <parent>
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
              </parent>

              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              </project>
              """,
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>

              <!--~~>--><parent>
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
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
    void directParentMatchesArtifactIdGlob() {
        rewriteRun(
          spec -> {
              spec.recipe(new ParentPomInsight("org.springframework.boot", "spring-*-parent", null, null));
              spec.dataTable(ParentPomsInUse.Row.class, rows -> assertThat(rows)
                .singleElement()
                .satisfies(row -> {
                      assertThat(row.getProjectArtifactId()).isEqualTo("my-app");
                      assertThat(row.getGroupId()).isEqualTo("org.springframework.boot");
                      assertThat(row.getArtifactId()).isEqualTo("spring-boot-starter-parent");
                      assertThat(row.getVersion()).isEqualTo("3.3.3");
                      assertThat(row.getRelativePath()).isNull();
                  }
                ));
          },
          pomXml(
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>

              <parent>
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
              </parent>

              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              </project>
              """,
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>

              <!--~~>--><parent>
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
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
    void indirectParentMatches() {
        rewriteRun(
          spec -> {
              spec.recipe(new ParentPomInsight("org.springframework.boot", "spring-boot-dependencies", null, null));
              spec.dataTable(ParentPomsInUse.Row.class, rows -> assertThat(rows)
                .singleElement()
                .satisfies(row -> {
                      assertThat(row.getProjectArtifactId()).isEqualTo("my-app");
                      assertThat(row.getGroupId()).isEqualTo("org.springframework.boot");
                      assertThat(row.getArtifactId()).isEqualTo("spring-boot-dependencies");
                      assertThat(row.getVersion()).isEqualTo("3.3.3");
                      assertThat(row.getRelativePath()).isNull();
                  }
                ));
          },
          pomXml(
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>

              <parent>
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
              </parent>

              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              </project>
              """,
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>

              <!--~~>--><parent>
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
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
    void indirectParentMatchesNonRecursive() {
        rewriteRun(
          spec -> spec.recipe(new ParentPomInsight("org.springframework.boot", "spring-boot-dependencies", null, false)),
          pomXml(
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>

              <parent>
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
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
    void indirectParentMatchesGAVPattern() {
        rewriteRun(
          spec -> {
              spec.recipe(new ParentPomInsight("*.springframework.*", "spring-*-dependencies", "3.x", null));
              spec.dataTable(ParentPomsInUse.Row.class, rows -> assertThat(rows)
                .singleElement()
                .satisfies(row -> {
                      assertThat(row.getProjectArtifactId()).isEqualTo("my-app");
                      assertThat(row.getGroupId()).isEqualTo("org.springframework.boot");
                      assertThat(row.getArtifactId()).isEqualTo("spring-boot-dependencies");
                      assertThat(row.getVersion()).isEqualTo("3.3.3");
                      assertThat(row.getRelativePath()).isNull();
                  }
                ));
          },
          pomXml(
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>

              <parent>
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
              </parent>

              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              </project>
              """,
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>

              <!--~~>--><parent>
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
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
    void multiModuleParentMatches() {
        rewriteRun(
          spec -> spec.dataTable(ParentPomsInUse.Row.class, rows -> assertThat(rows)
            .singleElement()
            .satisfies(row -> {
                  assertThat(row.getProjectArtifactId()).isEqualTo("child");
                  assertThat(row.getGroupId()).isEqualTo("org.springframework.boot");
                  assertThat(row.getArtifactId()).isEqualTo("spring-boot-starter-parent");
                  assertThat(row.getVersion()).isEqualTo("3.3.3");
                  assertThat(row.getRelativePath()).isNull();
              }
            )),
          pomXml(
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>

              <parent>
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
              </parent>
              <modules>
              	<module>child</module>
              </modules>

              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              </project>
              """,
            SourceSpec::skip
          ),
          mavenProject("child",
            pomXml(
              //language=xml
              """
                <project>
                <modelVersion>4.0.0</modelVersion>

                <parent>
                	<groupId>com.mycompany.app</groupId>
                	<artifactId>my-app</artifactId>
                	<version>1</version>
                	<relativePath>../</relativePath>
                </parent>

                <artifactId>child</artifactId>
                </project>
                """,
              //language=xml
              """
                <project>
                <modelVersion>4.0.0</modelVersion>

                <!--~~>--><parent>
                	<groupId>com.mycompany.app</groupId>
                	<artifactId>my-app</artifactId>
                	<version>1</version>
                	<relativePath>../</relativePath>
                </parent>

                <artifactId>child</artifactId>
                </project>
                """
            )
          )
        );
    }

    @Test
    void groupIdDoesNotMatch() {
        rewriteRun(
          spec -> spec.recipe(new ParentPomInsight("org.springframework.invalid", "spring-boot-starter-parent", null, null)),
          pomXml(
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>

              <parent>
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
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
    void artifactIdDoesNotMatch() {
        rewriteRun(
          spec -> spec.recipe(new ParentPomInsight("org.springframework.boot", "spring-boot-starter-web", null, null)),
          pomXml(
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>

              <parent>
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
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
    void versionDoesNotMatch() {
        rewriteRun(
          spec -> spec.recipe(new ParentPomInsight("org.springframework.boot", "spring-boot-starter-parent", "3.3.4", null)),
          pomXml(
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>

              <parent>
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
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
    void minorVersionDoesNotMatch() {
        rewriteRun(
          spec -> spec.recipe(new ParentPomInsight("org.springframework.boot", "spring-boot-starter-parent", "3.3.x", null)),
          pomXml(
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>

              <parent>
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.0.5</version>
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
    void doesNotMatchGroupIdGlob() {
        rewriteRun(
          spec -> spec.recipe(new ParentPomInsight("org.invalid.*", "spring-boot-starter-parent", null, null)),
          pomXml(
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>

              <parent>
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
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
    void doesNotMatchArtifactIdGlob() {
        rewriteRun(
          spec -> spec.recipe(new ParentPomInsight("org.springframework.boot", "spring-boot-*-web", null, null)),
          pomXml(
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>

              <parent>
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
              </parent>

              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              </project>
              """
          )
        );
    }
}
