package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class RemoveManagedDependencyTest implements RewriteTest {

    @Test
    void removeManagedDependency() {
        rewriteRun(
          spec -> spec.recipe(new RemoveManagedDependency(
            "javax.activation",
            "javax.activation-api",
            null
          )),
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
                                  <groupId>javax.activation</groupId>
                                  <artifactId>javax.activation-api</artifactId>
                                  <version>1.2.0</version>
                              </dependency>
                              <dependency>
                                  <groupId>jakarta.activation</groupId>
                                  <artifactId>jakarta.activation-api</artifactId>
                                  <version>1.2.1</version>
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
                                  <groupId>jakarta.activation</groupId>
                                  <artifactId>jakarta.activation-api</artifactId>
                                  <version>1.2.1</version>
                              </dependency>
                          </dependencies>
                      </dependencyManagement>
                  </project>
              """
          )
        );
    }

    @Test
    void removeManagedDependencyWithScopeNone() {
        rewriteRun(
          spec -> spec.recipe(new RemoveManagedDependency(
            "javax.activation",
            "javax.activation-api",
            null
          )),
          pomXml("""
                  <project>
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-app</artifactId>
                      <version>1</version>
                      <dependencyManagement>
                          <dependencies>
                              <dependency>
                                  <groupId>javax.activation</groupId>
                                  <artifactId>javax.activation-api</artifactId>
                                  <version>1.2.0</version>
                                  <scope>test</scope>
                              </dependency>
                              <dependency>
                                  <groupId>jakarta.activation</groupId>
                                  <artifactId>jakarta.activation-api</artifactId>
                                  <version>1.2.1</version>
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
                                  <groupId>jakarta.activation</groupId>
                                  <artifactId>jakarta.activation-api</artifactId>
                                  <version>1.2.1</version>
                              </dependency>
                          </dependencies>
                      </dependencyManagement>
                  </project>
              """
          )
        );
    }

    @Test
    void removeManagedDependencyWithScopeMatching() {
        rewriteRun(
          spec -> spec.recipe(new RemoveManagedDependency(
            "javax.activation",
            "javax.activation-api",
            "test"
          )),
          pomXml("""
                  <project>
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-app</artifactId>
                      <version>1</version>
                      <dependencyManagement>
                          <dependencies>
                              <dependency>
                                  <groupId>javax.activation</groupId>
                                  <artifactId>javax.activation-api</artifactId>
                                  <version>1.2.0</version>
                                  <scope>test</scope>
                              </dependency>
                              <dependency>
                                  <groupId>jakarta.activation</groupId>
                                  <artifactId>jakarta.activation-api</artifactId>
                                  <version>1.2.1</version>
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
                                  <groupId>jakarta.activation</groupId>
                                  <artifactId>jakarta.activation-api</artifactId>
                                  <version>1.2.1</version>
                              </dependency>
                          </dependencies>
                      </dependencyManagement>
                  </project>
              """
          )
        );
    }

    @Test
    void removeManagedDependencyWithScopeNonMatching() {
        rewriteRun(
          spec -> spec.recipe(new RemoveManagedDependency(
            "javax.activation",
            "javax.activation-api",
            "compile"
          )),
          pomXml("""
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>javax.activation</groupId>
                                <artifactId>javax.activation-api</artifactId>
                                <version>1.2.0</version>
                                <scope>test</scope>
                            </dependency>
                            <dependency>
                                <groupId>jakarta.activation</groupId>
                                <artifactId>jakarta.activation-api</artifactId>
                                <version>1.2.1</version>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """
          )
        );
    }
}
