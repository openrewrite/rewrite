/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.maven.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.maven.tree.Plugin;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.Profile;
import org.openrewrite.maven.tree.ProfileActivation;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class RawPomTest {

    @Test
    void profileActivationByJdk() {
        String javaVersion = System.getProperty("java.version");
        int dotIndex = javaVersion.indexOf('.');
        if (dotIndex > 0) {
            javaVersion = javaVersion.substring(0, dotIndex);
        }
        int runtimeVersion = Integer.parseInt(javaVersion);
        assertThat(new ProfileActivation(false, Integer.toString(runtimeVersion), null).isActive()).isTrue();
        assertThat(new ProfileActivation(false, "[," + (runtimeVersion + 1) + ")", null).isActive()).isTrue();
        assertThat(new ProfileActivation(false, "[," + runtimeVersion + "]", null).isActive()).isFalse();
    }

    @Test
    void profileActivationByAbsenceOfProperty() {
        assertThat(
          new ProfileActivation(
            false, null,
            new ProfileActivation.Property("!inactive", null)
          ).isActive()
        ).isTrue();
    }

    @Test
    void repositoriesSerializationAndDeserialization() {
        RawPom pom = RawPom.parse(
          new ByteArrayInputStream("""
                <project>
                  `<modelVersion>4.0.0</modelVersion>
                 
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <repositories>
                    <repository>
                        <id>spring-milestones</id>
                        <name>Spring Milestones</name>
                        <url>http://repo.spring.io/milestone</url>
                    </repository>
                  </repositories>
                </project>
            """.getBytes()),
          null
        );

        assertThat(pom.getRepositories()).isNotNull();
        assertThat(pom.getRepositories().getRepositories()).hasSize(1);
    }

    @Test
    void serializePluginFlags() {
        RawPom pom = RawPom.parse(
          new ByteArrayInputStream("""
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <properties>
                    <allowExtensions>true</allowExtensions>
                    <isInherited>true</isInherited>
                    </properties>
                    <build>
                      <plugins>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-surefire-plugin</artifactId>
                              <version>2.22.1</version>
                              <extensions>${allowExtensions}</extensions>
                              <inherited>${isInherited}</inherited>
                              <configuration>
                                  <includes>
                                          <include>**/*Tests.java</include>
                                          <include>**/*Test.java</include>
                                  </includes>
                                  <excludes>
                                          <exclude>**/Abstract*.java</exclude>
                                  </excludes>
                                  <argLine>hello</argLine>
                              </configuration>
                              <executions>
                                  <execution>
                                      <id>agent</id>
                                      <goals>
                                          <goal>prepare-agent</goal>
                                      </goals>
                                      <inherited>${isInherited}</inherited>
                                  </execution>
                              </executions>
                          </plugin>
                          <plugin>
                              <groupId>org.jacoco</groupId>
                              <artifactId>jacoco-maven-plugin</artifactId>
                              <extensions>false</extensions>
                              <inherited>false</inherited>
                              <executions>
                                  <execution>
                                      <id>agent</id>
                                      <inherited>false</inherited>
                                      <goals>
                                          <goal>prepare-agent</goal>
                                      </goals>
                                  </execution>
                                  <execution>
                                          <id>report</id>
                                          <phase>test</phase>
                                          <goals>
                                              <goal>report</goal>
                                          </goals>
                                  </execution>
                              </executions>
                          </plugin>
                      </plugins>
                    </build>
                </project>
            """.getBytes()),
          null
        );

        for (Plugin plugin : pom.toPom(null, null).getPlugins()) {
            if ("maven-surefire-plugin".equals(plugin.getArtifactId())) {
                assertThat(plugin.getExtensions()).isEqualTo("${allowExtensions}");
                assertThat(plugin.getInherited()).isEqualTo("${isInherited}");
                assertThat(plugin.getExecutions().get(0).getInherited()).isEqualTo("${isInherited}");
            } else {
                assertThat(plugin.getExtensions()).isEqualTo("false");
                assertThat(plugin.getInherited()).isEqualTo("false");
                assertThat(plugin.getExecutions().get(0).getInherited()).isEqualTo("false");
            }
        }
    }


    @SuppressWarnings("ConstantConditions")
    @Test
    void deserializePom() throws JsonProcessingException {
        //language=xml
        String pomString = """
              <project>
                  <modelVersion>4.0.0</modelVersion>
              
                  <parent>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-parent</artifactId>
                      <version>2.4.0</version>
                  </parent>
              
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <packaging>jar</packaging>
                  
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>org.springframework.cloud</groupId>
                              <artifactId>spring-cloud-dependencies</artifactId>
                              <version>Greenwich.SR6</version>
                              <type>pom</type>
                              <scope>import</scope>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
                  
                  <dependencies>
                    <dependency>
                      <groupId>org.junit.jupiter</groupId>
                      <artifactId>junit-jupiter-api</artifactId>
                      <version>5.7.0</version>
                      <scope>test</scope>
                      <exclusions>
                          <exclusion>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                          </exclusion>
                      </exclusions>
                    </dependency>
                  </dependencies>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.apache.maven.getPlugins()</groupId>
                              <artifactId>maven-surefire-plugin</artifactId>
                              <version>2.22.1</version>
                              <configuration>
                                  <includes>
                                          <include>**/*Tests.java</include>
                                          <include>**/*Test.java</include>
                                  </includes>
                                  <excludes>
                                          <exclude>**/Abstract*.java</exclude>
                                  </excludes>
                                  <!-- see https://stackoverflow.com/questions/18107375/getting-skipping-jacoco-execution-due-to-missing-execution-data-file-upon-exec -->
                                  <argLine>hello</argLine>
                              </configuration>
                          </plugin>
                          <plugin>
                              <groupId>org.jacoco</groupId>
                              <artifactId>jacoco-maven-plugin</artifactId>
                              <executions>
                                  <execution>
                                      <id>agent</id>
                                      <goals>
                                          <goal>prepare-agent</goal>
                                      </goals>
                                  </execution>
                                  <execution>
                                          <id>report</id>
                                          <phase>test</phase>
                                          <goals>
                                              <goal>report</goal>
                                          </goals>
                                  </execution>
                              </executions>
                          </plugin>
                      </plugins>
                      <pluginManagement>
                          <plugins>
                              <plugin>
                                  <groupId>org.openrewrite.maven</groupId>
                                  <artifactId>rewrite-maven-plugin</artifactId>
                                  <version>4.22.2</version>
                                  <configuration>
                                      <activeRecipes>
                                          <recipe>org.openrewrite.java.format.AutoFormat</recipe>
                                          <recipe>com.yourorg.VetToVeterinary</recipe>
                                          <recipe>org.openrewrite.java.spring.boot2.SpringBoot2JUnit4to5Migration</recipe>
                                      </activeRecipes>
                                  </configuration>
                                  <dependencies>
                                      <dependency>
                                          <groupId>org.openrewrite.recipe</groupId>
                                          <artifactId>rewrite-spring</artifactId>
                                          <version>4.19.3</version>
                                      </dependency>
                                  </dependencies>
                              </plugin>
                          </plugins>
                      </pluginManagement>
                  </build>
                  <licenses>
                    <license>
                      <name>Apache License, Version 2.0</name>
                      <url>https://www.apache.org/licenses/LICENSE-2.0.txt</url>
                      <distribution>repo</distribution>
                      <comments>A business-friendly OSS license</comments>
                    </license>
                  </licenses>
                  
                  <repositories>
                    <repository>
                      <releases>
                        <enabled>false</enabled>
                        <updatePolicy>always</updatePolicy>
                        <checksumPolicy>warn</checksumPolicy>
                      </releases>
                      <snapshots>
                        <enabled>true</enabled>
                        <updatePolicy>never</updatePolicy>
                        <checksumPolicy>fail</checksumPolicy>
                      </snapshots>
                      <name>Nexus Snapshots</name>
                      <id>snapshots-repo</id>
                      <url>https://oss.sonatype.org/content/repositories/snapshots</url>
                      <layout>default</layout>
                    </repository>
                  </repositories>
                  
                  <profiles>
                      <profile>
                          <id>java9+</id>
                          <activation>
                              <jdk>[9,)</jdk>
                          </activation>
                          <dependencies>
                              <dependency>
                                  <groupId>javax.xml.bind</groupId>
                                  <artifactId>jaxb-api</artifactId>
                              </dependency>
                          </dependencies>
                      </profile>
                      <profile>
                          <id>java11+</id>
                          <dependencies>
                          </dependencies>
                      </profile>
                      <profile>
                          <id>plugin-stuff</id>
                          <build>
                              <plugins>
                                  <plugin>
                                      <groupId>org.apache.maven.getPlugins()</groupId>
                                      <artifactId>maven-surefire-plugin</artifactId>
                                      <version>2.22.1</version>
                                      <configuration>
                                          <includes>
                                                  <include>**/*Tests.java</include>
                                                  <include>**/*Test.java</include>
                                          </includes>
                                          <excludes>
                                                  <exclude>**/Abstract*.java</exclude>
                                          </excludes>
                                          <!-- see https://stackoverflow.com/questions/18107375/getting-skipping-jacoco-execution-due-to-missing-execution-data-file-upon-exec -->
                                          <argLine>hello</argLine>
                                      </configuration>
                                  </plugin>
                                  <plugin>
                                      <groupId>org.jacoco</groupId>
                                      <artifactId>jacoco-maven-plugin</artifactId>
                                      <executions>
                                          <execution>
                                              <id>agent</id>
                                              <goals>
                                                  <goal>prepare-agent</goal>
                                              </goals>
                                          </execution>
                                          <execution>
                                                  <id>report</id>
                                                  <phase>test</phase>
                                                  <goals>
                                                      <goal>report</goal>
                                                  </goals>
                                          </execution>
                                      </executions>
                                  </plugin>
                              </plugins>
                              <pluginManagement>
                                  <plugins>
                                      <plugin>
                                          <groupId>org.openrewrite.maven</groupId>
                                          <artifactId>rewrite-maven-plugin</artifactId>
                                          <version>4.22.2</version>
                                          <configuration>
                                              <activeRecipes>
                                                  <recipe>org.openrewrite.java.format.AutoFormat</recipe>
                                                  <recipe>com.yourorg.VetToVeterinary</recipe>
                                                  <recipe>org.openrewrite.java.spring.boot2.SpringBoot2JUnit4to5Migration</recipe>
                                              </activeRecipes>
                                          </configuration>
                                          <dependencies>
                                              <dependency>
                                                  <groupId>org.openrewrite.recipe</groupId>
                                                  <artifactId>rewrite-spring</artifactId>
                                                  <version>4.19.3</version>
                                              </dependency>
                                          </dependencies>
                                      </plugin>
                                  </plugins>
                              </pluginManagement>
                          </build>

                      </profile>
                  </profiles>
              </project>
          """;

        Pom model = MavenXmlMapper.readMapper().readValue(pomString, RawPom.class).toPom(null, null);

        assertThat(model.getParent().getGroupId()).isEqualTo("org.springframework.boot");
        assertThat(model.getPackaging()).isEqualTo("jar");
        assertThat(model.getDependencies().get(0).getGroupId()).isEqualTo("org.junit.jupiter");
        assertThat(model.getDependencies().get(0).getExclusions().get(0).getGroupId())
          .isEqualTo("com.google.guava");
        assertThat(model.getDependencyManagement().get(0).getGroupId())
          .isEqualTo("org.springframework.cloud");
        assertThat(model.getPlugins()).hasSize(2);

        Plugin surefirePlugin = model.getPlugins().stream()
          .filter(p -> p.getArtifactId().equals("maven-surefire-plugin"))
          .findFirst()
          .orElseThrow();

        assertThat(surefirePlugin.getConfigurationList("includes", String.class))
          .hasSize(2)
          .contains("**/*Test.java", "**/*Tests.java");

        assertThat(surefirePlugin.getConfigurationList("excludes", String.class))
          .hasSize(1)
          .contains("**/Abstract*.java");

        assertThat(surefirePlugin.getConfigurationStringValue("argLine")).isEqualTo("hello");
        Plugin jacocoPlugin = model.getPlugins().stream()
          .filter(p -> p.getArtifactId().equals("jacoco-maven-plugin"))
          .findAny()
          .orElseThrow();

        assertThat(jacocoPlugin.getExecutions()).hasSize(2);

        var rewritePlugin = model.getPluginManagement().stream()
          .filter(p -> p.getArtifactId().equals("rewrite-maven-plugin"))
          .findAny()
          .orElseThrow();

        assertThat(rewritePlugin.getDependencies()).hasSize(1);
        assertThat(rewritePlugin.getDependencies().get(0).getGroupId()).isEqualTo("org.openrewrite.recipe");
        assertThat(rewritePlugin.getDependencies().get(0).getArtifactId()).isEqualTo("rewrite-spring");
        assertThat(rewritePlugin.getDependencies().get(0).getArtifactId()).isEqualTo("rewrite-spring");
        assertThat(rewritePlugin.getDependencies().get(0).getVersion()).isEqualTo("4.19.3");

        var activeRecipes = rewritePlugin.getConfigurationList("activeRecipes.recipe", String.class);
        assertThat(activeRecipes).contains(
          "org.openrewrite.java.format.AutoFormat",
          "com.yourorg.VetToVeterinary",
          "org.openrewrite.java.spring.boot2.SpringBoot2JUnit4to5Migration"
        );

        assertThat(model.getLicenses().get(0).getName())
          .isEqualTo("Apache License, Version 2.0");

        assertThat(model.getRepositories().get(0).getUri())
          .isEqualTo("https://oss.sonatype.org/content/repositories/snapshots");

        Profile java9Profile = model.getProfiles().stream()
          .filter(p -> "java9+".equals(p.getId()))
          .findAny()
          .orElseThrow();

        Profile java11Profile = model.getProfiles().stream()
          .filter(p -> "java11+".equals(p.getId()))
          .findAny()
          .orElseThrow();
        assertThat(java9Profile.getDependencies().get(0).getGroupId()).isEqualTo("javax.xml.bind");
        assertThat(java11Profile.getDependencies()).isEmpty();

        Profile rewriteProfile = model.getProfiles().stream()
          .filter(p -> "plugin-stuff".equals(p.getId()))
          .findAny()
          .orElseThrow();

        assertThat(rewriteProfile.getPlugins()).hasSize(2);
        jacocoPlugin = rewriteProfile.getPlugins().stream()
          .filter(p -> p.getArtifactId().equals("jacoco-maven-plugin"))
          .findAny()
          .orElseThrow();

        assertThat(jacocoPlugin.getExecutions()).hasSize(2);

        rewritePlugin = rewriteProfile.getPluginManagement().stream()
          .filter(p -> p.getArtifactId().equals("rewrite-maven-plugin"))
          .findAny()
          .orElseThrow();

        assertThat(rewritePlugin.getDependencies()).hasSize(1);
        assertThat(rewritePlugin.getDependencies().get(0).getGroupId()).isEqualTo("org.openrewrite.recipe");
        assertThat(rewritePlugin.getDependencies().get(0).getArtifactId()).isEqualTo("rewrite-spring");
        assertThat(rewritePlugin.getDependencies().get(0).getArtifactId()).isEqualTo("rewrite-spring");
        assertThat(rewritePlugin.getDependencies().get(0).getVersion()).isEqualTo("4.19.3");

        activeRecipes = rewritePlugin.getConfigurationList("activeRecipes", String.class);
        assertThat(activeRecipes).contains(
          "org.openrewrite.java.format.AutoFormat",
          "com.yourorg.VetToVeterinary",
          "org.openrewrite.java.spring.boot2.SpringBoot2JUnit4to5Migration"
        );
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void deserializePluginConfiguration() throws JsonProcessingException {
        @Language("xml") String pomString = """
              <project>
                  <modelVersion>4.0.0</modelVersion>
              
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <packaging>jar</packaging>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.apache.maven.getPlugins()</groupId>
                              <artifactId>maven-surefire-plugin</artifactId>
                              <version>2.22.1</version>
                              <configuration>
                                  <includes>
                                          <include>hello</include>
                                          <include>fred</include>
                                  </includes>
                                  <activeRecipes>
                                          <recipe>cool-recipe-1</recipe>
                                          <recipe>cool-recipe-2</recipe>
                                  </activeRecipes>
                                  <string-value>fred</string-value>
                                  <int-value>123</int-value>
                                  <grandparent>
                                      <parent>
                                          <child>
                                              <stringList>
                                                <element>f</element>
                                                <element>r</element>
                                                <element>e</element>
                                                <element>d</element>
                                              </stringList>
                                              <stringValue>fred</stringValue>
                                              <intValue>123</intValue>
                                          </child>
                                      </parent>
                                  </grandparent>
                              </configuration>
                          </plugin>
                      </plugins>
                  </build>
              </project>
          """;

        Pom model = MavenXmlMapper.readMapper().readValue(pomString, RawPom.class).toPom(null, null);
        var plugin = model.getPlugins().get(0);

        assertThat(plugin.getConfigurationList("includes", String.class)).hasSize(2).contains("fred", "hello");
        assertThat(plugin.getConfigurationList("activeRecipes", String.class)).hasSize(2)
          .contains("cool-recipe-1", "cool-recipe-2");

        assertThat(plugin.getConfigurationList("includes", String.class)).hasSize(2).contains("fred", "hello");
        assertThat(plugin.getConfigurationList("activeRecipes", String.class)).hasSize(2)
          .contains("cool-recipe-1", "cool-recipe-2");

        assertThat(plugin.getConfigurationStringValue("string-value")).isEqualTo("fred");
        assertThat(plugin.getConfigurationStringValue("int-value")).isEqualTo("123");
        assertThat(plugin.getConfiguration("int-value", Integer.class)).isEqualTo(123);

        var child = plugin.getConfiguration("grandparent.parent.child", ConfigChild.class);
        assertThat(child.getStringValue()).isEqualTo("fred");
        assertThat(child.getIntValue()).isEqualTo(123);
        assertThat(plugin.getConfigurationList("grandparent.parent.child.stringList", String.class)).hasSize(4)
          .contains("f", "r", "e", "d");
    }
}
