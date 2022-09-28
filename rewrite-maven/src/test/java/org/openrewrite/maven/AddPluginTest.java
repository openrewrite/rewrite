package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

public class AddPluginTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddPlugin("org.openrewrite.maven", "rewrite-maven-plugin", "100.0", null, null, null));
    }

    @Test
    void addPluginWithConfiguration() {
        rewriteRun(
          spec -> spec.recipe(new AddPlugin("org.openrewrite.maven", "rewrite-maven-plugin", "100.0",
            "<configuration>\n<activeRecipes>\n<recipe>io.moderne.FindTest</recipe>\n</activeRecipes>\n</configuration>",
            null, null)),
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
                    <build>
                      <plugins>
                        <plugin>
                          <groupId>org.openrewrite.maven</groupId>
                          <artifactId>rewrite-maven-plugin</artifactId>
                          <version>100.0</version>
                          <configuration>
                            <activeRecipes>
                              <recipe>io.moderne.FindTest</recipe>
                            </activeRecipes>
                          </configuration>
                        </plugin>
                      </plugins>
                    </build>
                  </project>
              """
          )
        );
    }

    @Test
    void addPluginWithDependencies() {
        rewriteRun(
          spec -> spec.recipe(new AddPlugin("org.openrewrite.maven", "rewrite-maven-plugin", "100.0", null,
            """
                  <dependencies>
                      <dependency>
                          <groupId>org.openrewrite.maven</groupId>
                          <artifactId>java-8-migration</artifactId>
                          <version>1.0.0</version>
                      </dependency>
                  </dependencies>
              """, null)),
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
                    <build>
                      <plugins>
                        <plugin>
                          <groupId>org.openrewrite.maven</groupId>
                          <artifactId>rewrite-maven-plugin</artifactId>
                          <version>100.0</version>
                          <dependencies>
                            <dependency>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>java-8-migration</artifactId>
                              <version>1.0.0</version>
                            </dependency>
                          </dependencies>
                        </plugin>
                      </plugins>
                    </build>
                  </project>
              """
          )
        );
    }

    @Test
    void addPluginWithExecutions() {
        rewriteRun(
          spec -> spec.recipe(new AddPlugin("org.openrewrite.maven", "rewrite-maven-plugin", "100.0", null, null,
            """
                  <executions>
                    <execution>
                      <id>xjc</id>
                      <goals>
                        <goal>xjc</goal>
                      </goals>
                      <configuration>
                        <sources>
                          <source>/src/main/resources/countries.xsd</source>
                          <source>/src/main/resources/countries1.xsd</source>
                        </sources>
                        <outputDirectory>/src/main/generated-java</outputDirectory>
                      </configuration>
                    </execution>
                  </executions>
              """)),
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
                    <build>
                      <plugins>
                        <plugin>
                          <groupId>org.openrewrite.maven</groupId>
                          <artifactId>rewrite-maven-plugin</artifactId>
                          <version>100.0</version>
                          <executions>
                            <execution>
                              <id>xjc</id>
                              <goals>
                                <goal>xjc</goal>
                              </goals>
                              <configuration>
                                <sources>
                                  <source>/src/main/resources/countries.xsd</source>
                                  <source>/src/main/resources/countries1.xsd</source>
                                </sources>
                                <outputDirectory>/src/main/generated-java</outputDirectory>
                              </configuration>
                            </execution>
                          </executions>
                        </plugin>
                      </plugins>
                    </build>
                  </project>
              """
          )
        );
    }

    @Test
    void addPlugin() {
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
                    <build>
                      <plugins>
                        <plugin>
                          <groupId>org.openrewrite.maven</groupId>
                          <artifactId>rewrite-maven-plugin</artifactId>
                          <version>100.0</version>
                        </plugin>
                      </plugins>
                    </build>
                  </project>
              """
          )
        );
    }

    @Test
    void updatePluginVersion() {
        rewriteRun(
          pomXml(
            """
                  <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <build>
                      <plugins>
                        <plugin>
                          <groupId>org.openrewrite.maven</groupId>
                          <artifactId>rewrite-maven-plugin</artifactId>
                          <version>99.0</version>
                        </plugin>
                      </plugins>
                    </build>
                  </project>
              """,
            """
                  <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <build>
                      <plugins>
                        <plugin>
                          <groupId>org.openrewrite.maven</groupId>
                          <artifactId>rewrite-maven-plugin</artifactId>
                          <version>100.0</version>
                        </plugin>
                      </plugins>
                    </build>
                  </project>
              """
          )
        );
    }
}
