package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

public class ChangePackagingTest implements RewriteTest {

    @Test
    void addPackaging() {
        rewriteRun(
          spec -> spec.recipe(new ChangePackaging("*", "*", "pom")),
          pomXml("""
                  <project>
                      <groupId>org.example</groupId>
                      <artifactId>foo</artifactId>
                      <version>1.0</version>
                  </project>
              """,
            """
                  <project>
                      <groupId>org.example</groupId>
                      <artifactId>foo</artifactId>
                      <version>1.0</version>
                      <packaging>pom</packaging>
                  </project>
              """
          )
        );
    }

    @Test
    void removePackaging() {
        rewriteRun(
          spec -> spec.recipe(new ChangePackaging("*", "*", null)),
          pomXml("""
                  <project>
                      <groupId>org.example</groupId>
                      <artifactId>foo</artifactId>
                      <version>1.0</version>
                      <packaging>pom</packaging>
                  </project>
              """,
            """
                  <project>
                      <groupId>org.example</groupId>
                      <artifactId>foo</artifactId>
                      <version>1.0</version>
                  </project>
              """
          )
        );
    }

    @Test
    void changePackaging() {
        rewriteRun(
          spec -> spec.recipe(new ChangePackaging("*", "*", "pom")),
          pomXml("""
                  <project>
                      <groupId>org.example</groupId>
                      <artifactId>foo</artifactId>
                      <version>1.0</version>
                      <packaging>jar</packaging>
                  </project>
              """,
            """
                  <project>
                      <groupId>org.example</groupId>
                      <artifactId>foo</artifactId>
                      <version>1.0</version>
                      <packaging>pom</packaging>
                  </project>
              """
          )
        );
    }
}
