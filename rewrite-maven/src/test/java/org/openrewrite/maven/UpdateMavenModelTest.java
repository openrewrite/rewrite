package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.maven.MavenParser.mavenConfig;

class UpdateMavenModelTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
          .classpath("guava", "jackson-databind"));
    }

    @Test
    void mavenConstantsExistAfterUpdateMavenModel() {
        rewriteRun(
          spec -> spec
            .recipes(
              new AddDependency("com.google.guava", "guava","29.0-jre", null, null, true, null, null, null,false, null, null),
              new AddDependency("com.fasterxml.jackson.module", "jackson-module-afterburner","2.10.5", null, null, true, null, null, null,false, null, null)
            ),
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
                  <dependencies>
                      <dependency>
                          <groupId>com.fasterxml.jackson.module</groupId>
                          <artifactId>jackson-module-afterburner</artifactId>
                          <version>2.10.5</version>
                      </dependency>
                      <dependency>
                          <groupId>com.google.guava</groupId>
                          <artifactId>guava</artifactId>
                          <version>29.0-jre</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.afterRecipe(p -> {
                var results = p.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                assertThat(results.getPom().getProperties().get("revision")).isEqualTo("1.0.0");
            }),
            mavenConfig("""
            -Drevision=1.0.0
            """)
          )
        );
    }
}
