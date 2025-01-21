/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class UseMavenCompilerPluginReleaseConfigurationTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseMavenCompilerPluginReleaseConfiguration(11));
    }

    @DocumentExample
    @Test
    void replacesSourceAndTargetConfig() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
              
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-compiler-plugin</artifactId>
                      <version>3.8.0</version>
                      <configuration>
                        <source>1.8</source>
                        <target>1.8</target>
                      </configuration>
                    </plugin>
                  </plugins>
                </build>
              
              </project>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
              
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-compiler-plugin</artifactId>
                      <version>3.8.0</version>
                      <configuration>
                        <release>11</release>
                      </configuration>
                    </plugin>
                  </plugins>
                </build>
              
              </project>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/514")
    @Test
    void replaceSourceAndTargetConfigIfDefault() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
              
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-compiler-plugin</artifactId>
                      <version>3.8.0</version>
                      <configuration>
                        <parameters>true</parameters>
                        <source>${maven.compiler.source}</source>
                        <target>${maven.compiler.target}</target>
                      </configuration>
                    </plugin>
                  </plugins>
                </build>
              
              </project>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
              
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-compiler-plugin</artifactId>
                      <version>3.8.0</version>
                      <configuration>
                        <parameters>true</parameters>
                        <release>11</release>
                      </configuration>
                    </plugin>
                  </plugins>
                </build>
              
              </project>
              """
          )
        );
    }

    @Test
    void reusesJavaVersionVariableIfAvailable() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
              
                <properties>
                  <java.version>11</java.version>
                </properties>
              
                <build>
                  <plugins>
                    <plugin>
                      <artifactId>maven-compiler-plugin</artifactId>
                      <version>3.8.0</version>
                      <configuration>
                        <source>${java.version}</source>
                        <target>${java.version}</target>
                      </configuration>
                    </plugin>
                  </plugins>
                </build>
              
              </project>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
              
                <properties>
                  <java.version>11</java.version>
                </properties>
              
                <build>
                  <plugins>
                    <plugin>
                      <artifactId>maven-compiler-plugin</artifactId>
                      <version>3.8.0</version>
                      <configuration>
                        <release>${java.version}</release>
                      </configuration>
                    </plugin>
                  </plugins>
                </build>
              
              </project>
              """
          )
        );
    }

    @Test
    void upgradesExistingReleaseConfig() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
              
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-compiler-plugin</artifactId>
                      <version>3.8.0</version>
                      <configuration>
                        <release>10</release>
                      </configuration>
                    </plugin>
                  </plugins>
                </build>
              
              </project>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
              
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-compiler-plugin</artifactId>
                      <version>3.8.0</version>
                      <configuration>
                        <release>11</release>
                      </configuration>
                    </plugin>
                  </plugins>
                </build>
              
              </project>
              """
          )
        );
    }

    @Test
    void prefersJavaVersionIfAvailable() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
              
                <properties>
                  <java.version>11</java.version>
                </properties>
              
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-compiler-plugin</artifactId>
                      <version>3.8.0</version>
                      <configuration>
                        <release>10</release>
                      </configuration>
                    </plugin>
                  </plugins>
                </build>
              
              </project>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
              
                <properties>
                  <java.version>11</java.version>
                </properties>
              
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-compiler-plugin</artifactId>
                      <version>3.8.0</version>
                      <configuration>
                        <release>${java.version}</release>
                      </configuration>
                    </plugin>
                  </plugins>
                </build>
              
              </project>
              """
          )
        );
    }

    @Test
    void notMisledByUnrelatedProperty() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
              
                <properties>
                  <foobar>11</foobar>
                </properties>
              
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-compiler-plugin</artifactId>
                      <version>3.8.0</version>
                      <configuration>
                        <release>10</release>
                        <basedir>${foobar}</basedir>
                      </configuration>
                    </plugin>
                  </plugins>
                </build>
              
              </project>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
              
                <properties>
                  <foobar>11</foobar>
                </properties>
              
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-compiler-plugin</artifactId>
                      <version>3.8.0</version>
                      <configuration>
                        <release>11</release>
                        <basedir>${foobar}</basedir>
                      </configuration>
                    </plugin>
                  </plugins>
                </build>
              
              </project>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/169")
    @Test
    void noVersionDowngrade() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
              
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-compiler-plugin</artifactId>
                      <version>3.8.0</version>
                      <configuration>
                        <release>17</release>
                      </configuration>
                    </plugin>
                  </plugins>
                </build>
              
              </project>
              """)
        );
    }

    @Test
    void reusesJavaVersionVariableIfDefinedInParentPom() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>parent</artifactId>
                <version>1.0.0</version>
              
                <properties>
                  <java.version>11</java.version>
                </properties>
              
                <packaging>pom</packaging>
              </project>
              """),
          mavenProject(
            "sample",
            //language=xml
            pomXml("""
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                
                  <parent>
                    <groupId>org.sample</groupId>
                    <artifactId>parent</artifactId>
                    <version>1.0.0</version>
                  </parent>
                
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>
                
                  <build>
                    <plugins>
                      <plugin>
                        <artifactId>maven-compiler-plugin</artifactId>
                        <version>3.8.0</version>
                        <configuration>
                          <source>${java.version}</source>
                          <target>${java.version}</target>
                        </configuration>
                      </plugin>
                    </plugins>
                  </build>
                </project>
                """,
              """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                
                  <parent>
                    <groupId>org.sample</groupId>
                    <artifactId>parent</artifactId>
                    <version>1.0.0</version>
                  </parent>
                
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>
                
                  <build>
                    <plugins>
                      <plugin>
                        <artifactId>maven-compiler-plugin</artifactId>
                        <version>3.8.0</version>
                        <configuration>
                          <release>${java.version}</release>
                        </configuration>
                      </plugin>
                    </plugins>
                  </build>
                </project>
                """
            )
          )
        );
    }

    @Test
    void pluginManagement() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>parent</artifactId>
                <version>1.0.0</version>
                <build>
                  <pluginManagement>
                    <plugins>
                      <plugin>
                        <artifactId>maven-compiler-plugin</artifactId>
                        <version>3.8.0</version>
                        <configuration>
                          <release>8</release>
                        </configuration>
                      </plugin>
                    </plugins>
                  </pluginManagement>
                </build>
              </project>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>parent</artifactId>
                <version>1.0.0</version>
                <build>
                  <pluginManagement>
                    <plugins>
                      <plugin>
                        <artifactId>maven-compiler-plugin</artifactId>
                        <version>3.8.0</version>
                        <configuration>
                          <release>11</release>
                        </configuration>
                      </plugin>
                    </plugins>
                  </pluginManagement>
                </build>
              </project>
              """
          )
        );
    }
}
