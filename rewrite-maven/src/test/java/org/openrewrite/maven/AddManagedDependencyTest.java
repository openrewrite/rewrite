package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.Validated;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

public class AddManagedDependencyTest implements RewriteTest {

    @Test
    void validation()  {
        AddManagedDependency recipe = new AddManagedDependency("org.apache.logging.log4j", "log4j-bom", "latest.release", "import",
          "pom", null, null, null, "org.apache.logging:*", true);
        Validated validated = recipe.validate();
        assertThat(validated).allMatch(Validated::isValid);
    }

    @Test
    void doNotAddIfTypeNotUsed() {
        rewriteRun(
          spec -> spec.recipe(new AddManagedDependency("org.apache.logging.log4j", "log4j-bom", "2.17.2", "import",
            "pom", null,null, null, "org.apache.logging.log4j:*", false)),
          pomXml(
            """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                </project>
            """
          )
        );
    }

    @Test
    void onlyAddedWhenUsing() {
        rewriteRun(
          spec -> spec.recipe(new AddManagedDependency("org.apache.logging.log4j", "log4j-bom", "2.17.2", "import",
            "pom", null,null, null, "org.apache.logging.log4j:*", false)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                    <groupId>org.apache.logging.log4j</groupId>
                    <artifactId>log4j-core</artifactId>
                    <version>2.17.2</version>
                  </dependency>
                </dependencies>
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
                      <groupId>org.apache.logging.log4j</groupId>
                      <artifactId>log4j-bom</artifactId>
                      <version>2.17.2</version>
                      <type>pom</type>
                      <scope>import</scope>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>org.apache.logging.log4j</groupId>
                    <artifactId>log4j-core</artifactId>
                    <version>2.17.2</version>
                  </dependency>
                </dependencies>
              </project>
            """
          )
        );
    }

    @Test
    void addedToRootPom() {
        rewriteRun(
          spec -> spec.recipe(new AddManagedDependency("org.apache.logging.log4j", "log4j-bom", "2.17.2", "import",
            "pom", null,null, null, null, true)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>project</artifactId>
                <version>1</version>
                <modules>
                  <module>core</module>
                  <module>service</module>
                </modules>
              </project>
            """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>project</artifactId>
                <version>1</version>
                <modules>
                  <module>core</module>
                  <module>service</module>
                </modules>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.apache.logging.log4j</groupId>
                      <artifactId>log4j-bom</artifactId>
                      <version>2.17.2</version>
                      <type>pom</type>
                      <scope>import</scope>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
            """
          ),
          mavenProject("service",
            pomXml(
              """
                <project>
                  <parent>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>project</artifactId>
                    <version>1</version>
                  </parent>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>service</artifactId>
                  <version>1</version>
                </project>
              """
            )
          ),
          mavenProject("core",
            pomXml(
              """
                <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>core</artifactId>
                  <version>1</version>
                </project>
              """,
              """
                <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>core</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>org.apache.logging.log4j</groupId>
                        <artifactId>log4j-bom</artifactId>
                        <version>2.17.2</version>
                        <type>pom</type>
                        <scope>import</scope>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                </project>
              """
            )
          )
        );
    }
}
