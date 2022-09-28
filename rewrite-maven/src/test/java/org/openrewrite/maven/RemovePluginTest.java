package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class RemovePluginTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemovePlugin("org.openrewrite.maven", "rewrite-maven-plugin"));
    }

    @Test
    void removePluginFromBuild() {
        rewriteRun(
          spec -> spec.recipe(new RemovePlugin("org.apache.avro", "avro-maven-plugin")),
          pomXml(
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>

                    <groupId>org.openrewrite.example</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>

                    <properties>
                      <avro-maven-plugin.version>1.10.2</avro-maven-plugin.version>
                    </properties>

                    <build>
                      <plugins>
                        <plugin>
                          <groupId>org.apache.avro</groupId>
                          <artifactId>avro-maven-plugin</artifactId>
                          <version>${"$"}{avro-maven-plugin.version}</version>
                          <executions>
                            <execution>
                              <id>schemas</id>
                              <phase>generate-sources</phase>
                              <goals>
                                <goal>schema</goal>
                                <goal>protocol</goal>
                                <goal>idl-protocol</goal>
                              </goals>
                              <configuration>
                                <sourceDirectory>${"$"}{project.basedir}/src/main/resources/</sourceDirectory>
                                <outputDirectory>${"$"}{project.basedir}/src/main/java/</outputDirectory>
                              </configuration>
                            </execution>
                          </executions>
                        </plugin>
                      </plugins>
                    </build>
                  </project>
              """,
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>

                    <groupId>org.openrewrite.example</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>

                    <properties>
                      <avro-maven-plugin.version>1.10.2</avro-maven-plugin.version>
                    </properties>
                  </project>
              """
          )
        );
    }

    @Test
    void removePluginFromReporting() {
        rewriteRun(
          pomXml(
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>

                    <groupId>org.openrewrite.example</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>

                    <reporting>
                      <plugins>
                        <plugin>
                          <groupId>org.openrewrite.maven</groupId>
                          <artifactId>rewrite-maven-plugin</artifactId>
                          <version>4.0.0</version>
                        </plugin>
                      </plugins>
                    </reporting>
                  </project>
              """,
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>

                    <groupId>org.openrewrite.example</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </project>
              """
          )
        );
    }

    @Test
    void removePluginFromBothBuildAndReporting() {
        rewriteRun(
          pomXml(
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>

                    <groupId>org.openrewrite.example</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>

                    <build>
                      <plugins>
                        <plugin>
                          <groupId>org.openrewrite.maven</groupId>
                          <artifactId>rewrite-maven-plugin</artifactId>
                          <version>4.0.0</version>
                        </plugin>
                      </plugins>
                    </build>

                    <reporting>
                      <plugins>
                        <plugin>
                          <groupId>org.openrewrite.maven</groupId>
                          <artifactId>rewrite-maven-plugin</artifactId>
                          <version>4.0.0</version>
                        </plugin>
                      </plugins>
                    </reporting>
                  </project>
              """,
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>

                    <groupId>org.openrewrite.example</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </project>
              """
          )
        );
    }

    @Test
    void removePluginWhenAlongsideOtherPlugins() {
        rewriteRun(
          pomXml(
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>

                    <groupId>org.openrewrite.example</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>

                    <properties>
                      <rewrite-maven-plugin.version>4.0.0</rewrite-maven-plugin.version>
                      <maven-checkstyle-plugin.version>3.1.2</maven-checkstyle-plugin.version>
                    </properties>

                    <build>
                      <plugins>
                        <plugin>
                          <groupId>org.openrewrite.maven</groupId>
                          <artifactId>rewrite-maven-plugin</artifactId>
                          <version>${"$"}{rewrite-maven-plugin.version}</version>
                        </plugin>
                        <plugin>
                          <groupId>org.apache.maven.plugins</groupId>
                          <artifactId>maven-gpg-plugin</artifactId>
                          <version>3.0.1</version>
                        </plugin>
                      </plugins>
                    </build>

                    <reporting>
                      <plugins>
                        <plugin>
                          <groupId>org.openrewrite.maven</groupId>
                          <artifactId>rewrite-maven-plugin</artifactId>
                          <version>${"$"}{rewrite-maven-plugin.version}</version>
                        </plugin>
                        <plugin>
                          <groupId>org.apache.maven.plugins</groupId>
                          <artifactId>maven-checkstyle-plugin</artifactId>
                          <version>${"$"}{maven-checkstyle-plugin.version}</version>
                          <configuration>
                            <configLocation>src/main/resources/checkstyle.xml</configLocation>
                          </configuration>
                        </plugin>
                      </plugins>
                    </reporting>
                  </project>
              """,
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>

                    <groupId>org.openrewrite.example</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>

                    <properties>
                      <rewrite-maven-plugin.version>4.0.0</rewrite-maven-plugin.version>
                      <maven-checkstyle-plugin.version>3.1.2</maven-checkstyle-plugin.version>
                    </properties>

                    <build>
                      <plugins>
                        <plugin>
                          <groupId>org.apache.maven.plugins</groupId>
                          <artifactId>maven-gpg-plugin</artifactId>
                          <version>3.0.1</version>
                        </plugin>
                      </plugins>
                    </build>

                    <reporting>
                      <plugins>
                        <plugin>
                          <groupId>org.apache.maven.plugins</groupId>
                          <artifactId>maven-checkstyle-plugin</artifactId>
                          <version>${"$"}{maven-checkstyle-plugin.version}</version>
                          <configuration>
                            <configLocation>src/main/resources/checkstyle.xml</configLocation>
                          </configuration>
                        </plugin>
                      </plugins>
                    </reporting>
                  </project>
              """
          )
        );
    }

    @Test
    void pluginToRemoveNotFound() {
        rewriteRun(
          pomXml(
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>

                    <groupId>org.openrewrite.example</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>

                    <build>
                      <plugins>
                        <plugin>
                          <groupId>org.apache.maven.plugins</groupId>
                          <artifactId>maven-gpg-plugin</artifactId>
                          <version>3.0.1</version>
                        </plugin>
                      </plugins>
                    </build>

                    <reporting>
                      <plugins>
                        <plugin>
                          <groupId>org.apache.maven.plugins</groupId>
                          <artifactId>maven-checkstyle-plugin</artifactId>
                          <version>${"$"}{maven-checkstyle-plugin}</version>
                          <configuration>
                            <configLocation>src/main/resources/checkstyle.xml</configLocation>
                          </configuration>
                        </plugin>
                      </plugins>
                    </reporting>
                  </project>
              """
          )
        );
    }
}
