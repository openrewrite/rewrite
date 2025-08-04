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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.java.Assertions.srcMainResources;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.test.SourceSpecs.text;
import static org.openrewrite.xml.Assertions.xml;

class RemoveUnusedPropertiesTest implements RewriteTest {
    @Override
    public void defaults(final RecipeSpec spec) {
        spec.recipe(new RemoveUnusedProperties(null));
    }

    @DocumentExample
    @Test
    void removesWholePropertiesSection() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>

                <properties>
                  <foo>xyz</foo>
                  <bar>xyz</bar>
                </properties>

              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>

              </project>
              """
          )
        );
    }

    @Test
    void ignoresMavenDefaultProperties() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>

                <properties>
                  <maven.build.timestamp.format>yyyy-MM-dd'T'HH:mm:ss'Z'</maven.build.timestamp.format>
                </properties>

              </project>
              """
          )
        );
    }

    @Test
    void regexPropertyPattern() {
        rewriteRun(
          spec -> spec.recipe(new RemoveUnusedProperties(".+\\.version")),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>

                <properties>
                  <spring.version>1.2.3</spring.version>
                  <gson.version>1.2.3</gson.version>
                  <bar>xyz</bar>
                </properties>

              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>

                <properties>
                  <bar>xyz</bar>
                </properties>

              </project>
              """
          )
        );
    }

    @Test
    void regexPropertyPatternNegativeLookahead() {
        rewriteRun(
          spec -> spec.recipe(new RemoveUnusedProperties("(?!java.|spring.).+")),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>

                <properties>
                  <java.version>21</java.version>
                  <spring.version>1.2.3</spring.version>
                  <gson.version>1.2.3</gson.version>
                  <bar>xyz</bar>
                </properties>

              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>

                <properties>
                  <java.version>21</java.version>
                  <spring.version>1.2.3</spring.version>
                </properties>

              </project>
              """
          )
        );
    }

    @Test
    void requiresCyclesForCascadingRemovals() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(2),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>

                <properties>
                  <foo>xyz</foo>
                  <bar>${foo}</bar>
                </properties>

              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>

              </project>
              """
          )
        );
    }

    @Test
    void keepsUsedProperty() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>

                <properties>
                  <spring.version>6.0.0</spring.version>
                  <bar>xyz</bar>
                </properties>

                <dependencies>
                  <dependency>
                    <groupId>org.springframework</groupId>
                    <artifactId>spring-beans</artifactId>
                    <version>${spring.version}</version>
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

                <properties>
                  <spring.version>6.0.0</spring.version>
                </properties>

                <dependencies>
                  <dependency>
                    <groupId>org.springframework</groupId>
                    <artifactId>spring-beans</artifactId>
                    <version>${spring.version}</version>
                  </dependency>
                </dependencies>

              </project>
              """
          )
        );
    }

    @Test
    void catchesMultiplePropertyUsagesInSameElement() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>

                <properties>
                  <six>6</six>
                  <zero>0</zero>
                  <ten>10</ten>
                </properties>

                <dependencies>
                  <dependency>
                    <groupId>org.springframework</groupId>
                    <artifactId>spring-beans</artifactId>
                    <version>${six}.${zero}.${zero}</version>
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

                <properties>
                  <six>6</six>
                  <zero>0</zero>
                </properties>

                <dependencies>
                  <dependency>
                    <groupId>org.springframework</groupId>
                    <artifactId>spring-beans</artifactId>
                    <version>${six}.${zero}.${zero}</version>
                  </dependency>
                </dependencies>

              </project>
              """
          )
        );
    }

    @Test
    void keepsOverrideOfParentProperty() {
        rewriteRun(
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
                  <version>3.0.0</version>
                  <relativePath />
                </parent>

                <properties>
                  <gson.version>2.11.0</gson.version>
                </properties>

              </project>
              """
          )
        );
    }

    @Test
    void keepsPropertyUsedByChild() {
        rewriteRun(
          mavenProject("parent",
            pomXml(
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>

                  <modules>
                    <module>module1</module>
                  </modules>

                  <properties>
                    <spring.version>6.0.0</spring.version>
                  </properties>

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

                    <dependencies>
                      <dependency>
                        <groupId>org.springframework</groupId>
                        <artifactId>spring-beans</artifactId>
                        <version>${spring.version}</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """
              ))
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "Hello ${a}",
      "Hello @a@"
    })
    void keepsPropertyUsedByFilteredResource(String text) {
        rewriteRun(
          mavenProject("my-project",
            pomXml(
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>

                  <properties>
                    <a>a</a>
                    <b>b</b>
                  </properties>

                  <build>
                    <resources>
                      <resource>
                        <directory>src/main/resources</directory>
                        <filtering>true</filtering>
                      </resource>
                    </resources>
                  </build>

                </project>
                """,
                """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>

                  <properties>
                    <a>a</a>
                  </properties>

                  <build>
                    <resources>
                      <resource>
                        <directory>src/main/resources</directory>
                        <filtering>true</filtering>
                      </resource>
                    </resources>
                  </build>

                </project>
                """
            ),
            srcMainResources(
              text(text)
            )
          )
        );
    }

    @Test
    void removesPropertyUsedByNonFilteredResource_filteringFalse() {
        rewriteRun(
          mavenProject("my-project",
            pomXml(
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>

                  <properties>
                    <a>a</a>
                    <b>b</b>
                  </properties>

                  <build>
                    <resources>
                      <resource>
                        <directory>src/main/resources</directory>
                        <filtering>false</filtering>
                      </resource>
                    </resources>
                  </build>

                </project>
                """, """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>

                  <build>
                    <resources>
                      <resource>
                        <directory>src/main/resources</directory>
                        <filtering>false</filtering>
                      </resource>
                    </resources>
                  </build>

                </project>
                """
            ),
            srcMainResources(
              text("Hello ${a}")
            )
          )
        );
    }

    @Test
    void removesPropertyUsedByNonFilteredResource_noFiltering() {
        rewriteRun(
          mavenProject("my-project",
            pomXml(
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>

                  <properties>
                    <a>a</a>
                    <b>b</b>
                  </properties>

                </project>
                """, """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>

                </project>
                """
            ),
            srcMainResources(
              text("Hello ${a}")
            )
          )
        );
    }

    @Test
    void removesPropertyUsedByNonFilteredResource_wrongDir() {
        rewriteRun(
          mavenProject("my-project",
            pomXml(
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>

                  <properties>
                    <a>a</a>
                    <b>b</b>
                  </properties>

                  <build>
                    <resources>
                      <resource>
                        <directory>src/main/resources-filtered</directory>
                        <filtering>false</filtering>
                      </resource>
                    </resources>
                  </build>

                </project>
                """, """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>

                  <build>
                    <resources>
                      <resource>
                        <directory>src/main/resources-filtered</directory>
                        <filtering>false</filtering>
                      </resource>
                    </resources>
                  </build>

                </project>
                """
            ),
            srcMainResources(
              text("Hello ${a}")
            )
          )
        );
    }

    @Test
    void keepsMultiplePropertiesUsedBySameFilteredResource() {
        rewriteRun(
          mavenProject("my-project",
            pomXml(
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>

                  <properties>
                    <a>a</a>
                    <b>b</b>
                    <c>c</c>
                  </properties>

                  <build>
                    <resources>
                      <resource>
                        <directory>src/main/resources</directory>
                        <filtering>true</filtering>
                      </resource>
                    </resources>
                  </build>

                </project>
                """, """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>

                  <properties>
                    <a>a</a>
                    <c>c</c>
                  </properties>

                  <build>
                    <resources>
                      <resource>
                        <directory>src/main/resources</directory>
                        <filtering>true</filtering>
                      </resource>
                    </resources>
                  </build>

                </project>
                """
            ),
            srcMainResources(
              text("Hello ${a} ${c}")
            )
          )
        );
    }

    @Test
    void keepsPropertyUsedByResourceFilter_Xml() {
        rewriteRun(
          mavenProject("my-project",
            pomXml(
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>

                  <properties>
                    <spring.version>6.0.0</spring.version>
                  </properties>

                  <build>
                    <resources>
                      <resource>
                        <directory>src/main/resources</directory>
                        <filtering>true</filtering>
                      </resource>
                    </resources>
                  </build>

                </project>
                """
            ),
            srcMainResources(
              xml("""
              <elements>
                <element>${spring.version}</element>
              </elements>
              """)
            )
          )
        );
    }

    @Test
    void removesIrrelevantPropertyDeclaration() {
        rewriteRun(
          mavenProject("parent",
            pomXml(
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
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
                      <version>1.0.0</version>
                    </parent>
                    <artifactId>module1</artifactId>

                    <properties>
                      <spring.version>6.0.0</spring.version>
                    </properties>

                    <dependencies>
                      <dependency>
                        <groupId>org.springframework</groupId>
                        <artifactId>spring-beans</artifactId>
                        <version>${spring.version}</version>
                      </dependency>
                    </dependencies>
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

                    <properties>
                      <spring.version>6.0.0</spring.version>
                    </properties>
                  </project>
                  """, """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                      <groupId>org.sample</groupId>
                      <artifactId>sample</artifactId>
                      <version>1.0.0</version>
                    </parent>
                    <artifactId>module2</artifactId>
                  </project>
                  """
              )
            )
          )
        );
    }

    @Test
    void removesIrrelevantPropertyDeclarationForFilteredResourceUsage() {
        rewriteRun(
          mavenProject("parent",
            pomXml(
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
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
                      <version>1.0.0</version>
                    </parent>
                    <artifactId>module1</artifactId>

                    <properties>
                      <a>a</a>
                    </properties>

                    <build>
                      <resources>
                        <resource>
                          <directory>src/main/resources</directory>
                          <filtering>true</filtering>
                        </resource>
                      </resources>
                    </build>

                  </project>
                  """
              ),
              srcMainResources(
                text("Hello ${a}")
              )
            ),
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

                    <properties>
                      <a>a</a>
                    </properties>
                  </project>
                  """, """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                      <groupId>org.sample</groupId>
                      <artifactId>sample</artifactId>
                      <version>1.0.0</version>
                    </parent>
                    <artifactId>module2</artifactId>
                  </project>
                  """
              )
            )
          )
        );
    }
}
