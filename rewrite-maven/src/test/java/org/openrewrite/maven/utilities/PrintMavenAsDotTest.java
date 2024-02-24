/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.maven.utilities;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class PrintMavenAsDotTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new PrintMavenAsDot());
    }

    @DocumentExample
    @Test
    void dot() {
        rewriteRun(
          pomXml(
            """
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              
              <dependencies>
                <dependency>
                    <groupId>org.yaml</groupId>
                    <artifactId>snakeyaml</artifactId>
                    <version>1.27</version>
                </dependency>
                <dependency>
                  <groupId>org.junit.jupiter</groupId>
                  <artifactId>junit-jupiter</artifactId>
                  <version>5.7.0</version>
                  <scope>test</scope>
                </dependency>
              </dependencies>
            </project>
            """,
            """
            <!--~~(digraph main {
            0 [label="com.mycompany.app:my-app:1"];
            1 [label="org.yaml:snakeyaml:1.27"];
            2 [label="org.junit.jupiter:junit-jupiter:5.7.0"];
            3 [label="org.junit.jupiter:junit-jupiter-api:5.7.0"];
            4 [label="org.junit.jupiter:junit-jupiter-params:5.7.0"];
            5 [label="org.junit.jupiter:junit-jupiter-engine:5.7.0"];
            6 [label="org.apiguardian:apiguardian-api:1.1.0"];
            7 [label="org.opentest4j:opentest4j:1.2.0"];
            8 [label="org.junit.platform:junit-platform-commons:1.7.0"];
            9 [label="org.junit.platform:junit-platform-engine:1.7.0"];
            0 -> 1 [taillabel="Compile"];
            0 -> 2 [taillabel="Test"];
            2 -> 3 [taillabel="Test"];
            3 -> 6 [taillabel="Test"];
            3 -> 7 [taillabel="Test"];
            3 -> 8 [taillabel="Test"];
            2 -> 4 [taillabel="Test"];
            2 -> 5 [taillabel="Test"];
            5 -> 9 [taillabel="Test"];
            })~~>--><project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              
              <dependencies>
                <dependency>
                    <groupId>org.yaml</groupId>
                    <artifactId>snakeyaml</artifactId>
                    <version>1.27</version>
                </dependency>
                <dependency>
                  <groupId>org.junit.jupiter</groupId>
                  <artifactId>junit-jupiter</artifactId>
                  <version>5.7.0</version>
                  <scope>test</scope>
                </dependency>
              </dependencies>
            </project>
            """
          )
        );
    }

    @Test
    @Disabled
    void bigDot() {
        rewriteRun(
          pomXml(
            """
                <project>
                    <parent>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-parent</artifactId>
                      <version>2.7.4</version>
                      <relativePath/> <!-- lookup parent from repository -->
                    </parent>
                    <groupId>com.example</groupId>
                    <artifactId>demo</artifactId>
                    <version>0.0.1-SNAPSHOT</version>
                    <name>demo</name>
                    <description>Demo project for Spring Boot</description>
                    <properties>
                      <java.version>17</java.version>
                      <spring-cloud.version>2021.0.4</spring-cloud.version>
                    </properties>
                    <dependencies>
                      <dependency>
                        <groupId>org.springframework.cloud</groupId>
                        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
                      </dependency>
                  
                      <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-test</artifactId>
                        <scope>test</scope>
                      </dependency>
                    </dependencies>
                    <dependencyManagement>
                      <dependencies>
                        <dependency>
                          <groupId>org.springframework.cloud</groupId>
                          <artifactId>spring-cloud-dependencies</artifactId>
                          <version>${spring-cloud.version}</version>
                          <type>pom</type>
                          <scope>import</scope>
                        </dependency>
                      </dependencies>
                    </dependencyManagement>
              </project>
            """
          )
        );
    }
}
