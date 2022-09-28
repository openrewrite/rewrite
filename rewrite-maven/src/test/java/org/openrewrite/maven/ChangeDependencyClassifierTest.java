package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

public class ChangeDependencyClassifierTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeDependencyClassifier("org.ehcache", "ehcache", "jakarta"));
    }

    @Test
    void noClassifierToClassifier() {
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
                        <groupId>org.ehcache</groupId>
                        <artifactId>ehcache</artifactId>
                        <version>3.10.0</version>
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
                        <groupId>org.ehcache</groupId>
                        <artifactId>ehcache</artifactId>
                        <version>3.10.0</version>
                        <classifier>jakarta</classifier>
                      </dependency>
                    </dependencies>
                  </project>
              """
          )
        );
    }

    @Test
    void classifierToClassifier() {
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
                        <groupId>org.ehcache</groupId>
                        <artifactId>ehcache</artifactId>
                        <version>3.10.0</version>
                        <classifier>javax</classifier>
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
                        <groupId>org.ehcache</groupId>
                        <artifactId>ehcache</artifactId>
                        <version>3.10.0</version>
                        <classifier>jakarta</classifier>
                      </dependency>
                    </dependencies>
                  </project>
              """
          )
        );
    }

    @Test
    void classifierToNoClassifier() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyClassifier("org.ehcache", "ehcache", null)),
          pomXml(
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    
                    <dependencies>
                      <dependency>
                        <groupId>org.ehcache</groupId>
                        <artifactId>ehcache</artifactId>
                        <version>3.10.0</version>
                        <classifier>jakarta</classifier>
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
                        <groupId>org.ehcache</groupId>
                        <artifactId>ehcache</artifactId>
                        <version>3.10.0</version>
                      </dependency>
                    </dependencies>
                  </project>
              """
          )
        );
    }
}
