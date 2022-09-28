package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

public class ChangeDependencyScopeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeDependencyScope("com.google.guava", "guava", "test"));
    }

    @Test
    void noScopeToScope() {
        rewriteRun(
          pomXml(
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                  
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  
                    <dependencies>
                      <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>28.2-jre</version>
                      </dependency>
                    </dependencies>
                  </project>
              """,
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                  
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  
                    <dependencies>
                      <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>28.2-jre</version>
                        <scope>test</scope>
                      </dependency>
                    </dependencies>
                  </project>
              """
          )
        );
    }

    @Test
    void scopeToScope() {
        rewriteRun(
          pomXml(
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    
                    <dependencies>
                      <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>28.2-jre</version>
                        <scope>compile</scope>
                      </dependency>
                    </dependencies>
                  </project>
              """,
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    
                    <dependencies>
                      <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>28.2-jre</version>
                        <scope>test</scope>
                      </dependency>
                    </dependencies>
                  </project>
              """
          )
        );
    }

    @Test
    void scopeToNoScope() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyScope("com.google.guava", "guava", null)),
          pomXml(
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    
                    <dependencies>
                      <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>28.2-jre</version>
                        <scope>test</scope>
                      </dependency>
                    </dependencies>
                  </project>
              """,
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    
                    <dependencies>
                      <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>28.2-jre</version>
                      </dependency>
                    </dependencies>
                  </project>
              """
          )
        );
    }
}
