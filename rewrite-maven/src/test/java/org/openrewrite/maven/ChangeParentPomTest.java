package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

public class ChangeParentPomTest implements RewriteTest {

    @Test
    void changeParent() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.springframework.boot",
            "com.fasterxml.jackson",
            "spring-boot-starter-parent",
            "jackson-parent",
            "2.12",
            null,
            false
          )),
          pomXml(
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    
                    <parent>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-parent</artifactId>
                      <version>1.5.12.RELEASE</version>
                    </parent>
                    
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </project>
              """,
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    
                    <parent>
                      <groupId>com.fasterxml.jackson</groupId>
                      <artifactId>jackson-parent</artifactId>
                      <version>2.12</version>
                    </parent>
                    
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </project>
              """
          )
        );
    }

    @Test
    void upgradeVersion() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.springframework.boot",
            null,
            "spring-boot-starter-parent",
            null,
            "~1.5",
            null,
            false
          )),
          pomXml(
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    
                    <parent>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-parent</artifactId>
                      <version>1.5.12.RELEASE</version>
                      <relativePath/> <!-- lookup parent from repository -->
                    </parent>
                    
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </project>
              """,
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    
                    <parent>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-parent</artifactId>
                      <version>1.5.22.RELEASE</version>
                      <relativePath/> <!-- lookup parent from repository -->
                    </parent>
                    
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </project>
              """
          )
        );
    }

    @Test
    void upgradeToExactVersion() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.springframework.boot",
            null,
            "spring-boot-starter-parent",
            null,
            "1.5.22.RELEASE",
            null,
            false
          )),
          pomXml(
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    
                    <parent>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-parent</artifactId>
                      <version>1.5.12.RELEASE</version>
                      <relativePath/> <!-- lookup parent from repository -->
                    </parent>
                    
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </project>
              """,
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    
                    <parent>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-parent</artifactId>
                      <version>1.5.22.RELEASE</version>
                      <relativePath/> <!-- lookup parent from repository -->
                    </parent>
                    
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </project>
              """
          )
        );
    }

    @Test
    void doNotDowngradeToLowerVersionWhenArtifactsAreTheSame() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.springframework.boot",
            null,
            "spring-boot-starter-parent",
            null,
            "1.5.12.RELEASE",
            null,
            false
          )),
          pomXml(
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    
                    <parent>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-parent</artifactId>
                      <version>1.5.22.RELEASE</version>
                      <relativePath/> <!-- lookup parent from repository -->
                    </parent>
                    
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </project>
              """
          )
        );
    }

    @Test
    void downgradeToLowerVersionWhenFlagisSet() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.springframework.boot",
            null,
            "spring-boot-starter-parent",
            null,
            "1.5.12.RELEASE",
            null,
            true
          )),
          pomXml(
            """
                    <project>
                      <modelVersion>4.0.0</modelVersion>
                      
                      <parent>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-parent</artifactId>
                        <version>1.5.22.RELEASE</version>
                        <relativePath/> <!-- lookup parent from repository -->
                      </parent>
                      
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-app</artifactId>
                      <version>1</version>
                    </project>
              """,
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    
                    <parent>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-parent</artifactId>
                      <version>1.5.12.RELEASE</version>
                      <relativePath/> <!-- lookup parent from repository -->
                    </parent>
                    
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </project>
              """
          )
        );
    }
}
