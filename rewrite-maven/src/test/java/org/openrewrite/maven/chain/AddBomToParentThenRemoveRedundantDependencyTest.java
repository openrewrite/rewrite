/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
          mavenProject("parent",
            //language=xml
            pomXml(
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>parent</artifactId>
                  <version>1</version>
                  <packaging>pom</packaging>
                  <modules>
                    <module>child</module>
                  </modules>
                </project>
                """,
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>parent</artifactId>
                  <version>1</version>
                  <packaging>pom</packaging>
                  <modules>
                    <module>child</module>
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
          mavenProject("child",
            //language=xml
            pomXml("""
              <project>
                <parent>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>parent</artifactId>
                  <version>1</version>
                </parent>
              	<artifactId>child</artifactId>
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
                  <artifactId>parent</artifactId>
                  <version>1</version>
                </parent>
              	<artifactId>child</artifactId>
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
