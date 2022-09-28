package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

public class ChangeManagedDependencyGroupIdAndArtifactIdTest implements RewriteTest {

    @Test
    void changeManagedDependencyGroupIdAndArtifactId() {
        rewriteRun(
          spec -> spec.recipe(new ChangeManagedDependencyGroupIdAndArtifactId(
            "javax.activation",
            "javax.activation-api",
            "jakarta.activation",
            "jakarta.activation-api",
            "2.1.0"
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
                                  <version>2.1.0</version>
                              </dependency>
                          </dependencies>
                      </dependencyManagement>
                  </project>
              """
          )
        );
    }
}
