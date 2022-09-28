package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class RemoveExclusionTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveExclusion(
          "com.google.guava",
          "guava",
          "commons-lang",
          "commons-lang"
        ));
    }

    @Test
    void removeUnusedExclusions() {
        rewriteRun(
          pomXml(
            """
                  <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <dependencies>
                      <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>29.0-jre</version>
                        <exclusions>
                          <exclusion>
                            <groupId>commons-lang</groupId>
                            <artifactId>commons-lang</artifactId>
                          </exclusion>
                        </exclusions>
                      </dependency>
                    </dependencies>
                  </project>
              """,
            """
                  <project>
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

    @Test
    void removeUnusedExclusionsFromDependencyManagement() {
        rewriteRun(
          pomXml("""
                  <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <dependencyManagement>
                      <dependencies>
                        <dependency>
                          <groupId>com.google.guava</groupId>
                          <artifactId>guava</artifactId>
                          <version>29.0-jre</version>
                          <exclusions>
                            <exclusion>
                              <groupId>commons-lang</groupId>
                              <artifactId>commons-lang</artifactId>
                            </exclusion>
                          </exclusions>
                        </dependency>
                      </dependencies>
                    </dependencyManagement>
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
                          <groupId>com.google.guava</groupId>
                          <artifactId>guava</artifactId>
                          <version>29.0-jre</version>
                        </dependency>
                      </dependencies>
                    </dependencyManagement>
                  </project>
              """
          )
        );
    }
}
