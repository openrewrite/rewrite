package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class RemovePropertyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveProperty("bla.version"));
    }

    @Test
    void removeProperty() {
        rewriteRun(
          pomXml(
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                     
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    
                    <properties>
                      <a.version>a</a.version>
                      <bla.version>b</bla.version>
                    </properties>
                  </project>
              """,
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                     
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    
                    <properties>
                      <a.version>a</a.version>
                    </properties>
                  </project>
              """
          )
        );
    }
}
