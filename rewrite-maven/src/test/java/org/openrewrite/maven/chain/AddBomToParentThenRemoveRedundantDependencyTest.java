package org.openrewrite.maven.chain;

import org.junit.jupiter.api.Test;
import org.openrewrite.maven.AddManagedDependency;
import org.openrewrite.maven.RemoveRedundantDependencyVersions;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class AddBomToParentThenRemoveRedundantDependencyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(
          new AddManagedDependency("org.springframework.boot", "spring-boot-dependencies", "3.4.2", "import", "pom", null, null, null, null, true),
          new RemoveRedundantDependencyVersions("*", "*", RemoveRedundantDependencyVersions.Comparator.ANY, null));
    }

    @Test
    void bomShouldBeAddedToParentPomAndVersionTagShouldBeRemoved() {
        rewriteRun(
          mavenProject(
            "my-app-parent",
            //language=xml
            pomXml(
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app-parent</artifactId>
                  <version>1</version>
                  <packaging>pom</packaging>
                  <modules>
                    <module>my-app</module>
                  </modules>
                </project>
                """,
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app-parent</artifactId>
                  <version>1</version>
                  <packaging>pom</packaging>
                  <modules>
                    <module>my-app</module>
                  </modules>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-dependencies</artifactId>
                        <version>3.4.2</version>
                        <type>pom</type>
                        <scope>import</scope>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                </project>
                """
            )
          ),
          mavenProject(
            "my-app",
            //language=xml
            pomXml("""
              <project>
                <parent>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app-parent</artifactId>
                  <version>1</version>
                </parent>
              	<artifactId>my-app</artifactId>
              	<dependencies>
              		<dependency>
              		   <groupId>org.hibernate.orm</groupId>
              		   <artifactId>hibernate-core</artifactId>
              		   <version>6.6.5.Final</version>
              	   </dependency>
              	</dependencies>
              </project>
              """, """
              <project>
                <parent>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app-parent</artifactId>
                  <version>1</version>
                </parent>
              	<artifactId>my-app</artifactId>
              	<dependencies>
              		<dependency>
              		   <groupId>org.hibernate.orm</groupId>
              		   <artifactId>hibernate-core</artifactId>
              	   </dependency>
              	</dependencies>
              </project>
              """)
          )
        );
    }

}
