/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class ModernizeObsoletePomsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ModernizeObsoletePoms());
    }

    @DocumentExample
    @Test
    void oldPom() {
        rewriteRun(
          pomXml(
                """
          <project>
              <pomVersion>3</pomVersion>
              <groupId>org.jvnet.staxex</groupId>
              <artifactId>stax-ex</artifactId>
              <name>Extended StAX API</name>
              <currentVersion>1.0</currentVersion>
              <description>Extensions to JSR-173 StAX API.</description>
              <issueTrackingUrl>https://stax-ex.dev.java.net/servlets/ProjectIssues</issueTrackingUrl>
              <organization>
                  <name>java.net</name>
                  <url>http://java.net/</url>
                  <logo>
                      https://stax-ex.dev.java.net/branding/images/header_jnet_new.jpg
                  </logo>
              </organization>
              <repository>
                  <connection>scm:cvs:pserver:guest@cvs.dev.java.net:/cvs:stax-ex</connection>
                  <url>https://stax-ex.dev.java.net/source/browse/stax-ex</url>
              </repository>
              <package>org.jvnet.staxex</package>
              <build>
                  <sourceDirectory>src/java</sourceDirectory>
                  <unitTest/>
                  <resources>
                    <resource>
                      <directory>src/resources</directory>
                    </resource>
                  </resources>
              </build>
          </project>
          """,
          """
          <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.jvnet.staxex</groupId>
              <artifactId>stax-ex</artifactId>
              <name>Extended StAX API</name>
              <version>1.0</version>
              <description>Extensions to JSR-173 StAX API.</description>
              <issueManagement>
                  <system>IssueTracker</system>
                  <url>https://stax-ex.dev.java.net/servlets/ProjectIssues</url>
              </issueManagement>
              <organization>
                  <name>java.net</name>
                  <url>http://java.net/</url>
              </organization>
              <repositories>
                  <repository>
                      <id>repo</id>
                      <url>https://stax-ex.dev.java.net/source/browse/stax-ex</url>
                  </repository>
              </repositories>
              <build>
                  <sourceDirectory>src/java</sourceDirectory>
                  <resources>
                    <resource>
                      <directory>src/resources</directory>
                    </resource>
                  </resources>
              </build>
          </project>
          """
          )
        );
    }
}
