package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

public class ChangeProjectVersionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeProjectVersion("org.openrewrite", "rewrite-maven", "8.4.2"));
    }

    @Test
    void changeProjectVersion() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>rewrite-maven</artifactId>
                  <version>8.4.1</version>
              </project>
              """, """
              <project>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>rewrite-maven</artifactId>
                  <version>8.4.2</version>
              </project>
              """
          )
        );
    }

    @Test
    void changeProjectVersionProperty() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>rewrite-maven</artifactId>
                  <version>${rewrite.version}</version>
                  
                  <properties>
                      <rewrite.version>8.4.1</rewrite.version>
                  </properties>
              </project>
              """, """
              <project>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>rewrite-maven</artifactId>
                  <version>${rewrite.version}</version>
                  
                  <properties>
                      <rewrite.version>8.4.2</rewrite.version>
                  </properties>
              </project>
              """
          )
        );
    }

    @Test
    void changeProjectVersionResolveProperties() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>${rewrite.groupId}</groupId>
                  <artifactId>${rewrite-maven.artifactId}</artifactId>
                  <version>8.4.1</version>
                  
                  <properties>
                      <rewrite.groupId>org.openrewrite</rewrite.groupId>
                      <rewrite-maven.artifactId>rewrite-maven</rewrite-maven.artifactId>
                  </properties>
              </project>
              """, """
              <project>
                  <groupId>${rewrite.groupId}</groupId>
                  <artifactId>${rewrite-maven.artifactId}</artifactId>
                  <version>8.4.2</version>
                  
                  <properties>
                      <rewrite.groupId>org.openrewrite</rewrite.groupId>
                      <rewrite-maven.artifactId>rewrite-maven</rewrite-maven.artifactId>
                  </properties>
              </project>
              """
          )
        );
    }

    @Test
    void changeProjectVersionResolvePropertiesOnParent() {
        rewriteRun(
          pomXml(
            """
                <project>
                    <groupId>org.openrewrite</groupId>
                    <artifactId>rewrite-core</artifactId>
                    <version>8.4.1</version>
                    
                    <properties>
                        <rewrite.groupId>org.openrewrite</rewrite.groupId>
                        <rewrite-maven.artifactId>rewrite-maven</rewrite-maven.artifactId>
                    </properties>
                </project>
              """
          ),
          mavenProject("rewrite-maven",
            pomXml(
            """
              <project>
                  <parent>
                      <groupId>org.openrewrite</groupId>
                      <artifactId>rewrite-core</artifactId>
                      <version>8.4.1</version>
                  </parent>
                  <groupId>${rewrite.groupId}</groupId>
                  <artifactId>${rewrite-maven.artifactId}</artifactId>
                  <version>8.4.1</version>
              </project>
              """, """
              <project>
                  <parent>
                      <groupId>org.openrewrite</groupId>
                      <artifactId>rewrite-core</artifactId>
                      <version>8.4.1</version>
                  </parent>
                  <groupId>${rewrite.groupId}</groupId>
                  <artifactId>${rewrite-maven.artifactId}</artifactId>
                  <version>8.4.2</version>
              </project>
              """
            )
          )
        );
    }

    @Test
    void doNotChangeOtherProjectVersion() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>rewrite-gradle</artifactId>
                  <version>8.4.1</version>
              </project>
              """
          )
        );
    }

    @Test
    void changesMultipleMatchingProjects() {
        rewriteRun(
          spec -> spec.recipe(new ChangeProjectVersion("org.openrewrite", "rewrite-*", "8.4.2")),
          pomXml(
            """
              <project>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>rewrite-maven</artifactId>
                  <version>8.4.1</version>
              </project>
              """, """
              <project>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>rewrite-maven</artifactId>
                  <version>8.4.2</version>
              </project>
              """
          ),
          pomXml(
            """
              <project>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>rewrite-gradle</artifactId>
                  <version>8.4.1</version>
              </project>
              """, """
              <project>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>rewrite-gradle</artifactId>
                  <version>8.4.2</version>
              </project>
              """
          )
        );
    }

}
