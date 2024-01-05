/*
 * Copyright 2023 the original author or authors.
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
import org.openrewrite.test.SourceSpecs;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.xml.Assertions.xml;

class AddDevelocityMavenExtensionTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddDevelocityMavenExtension("1.17", "https://foo", null,
          null, null, null));
    }

    private static final SourceSpecs POM_XML_SOURCE_SPEC = pomXml(
      """
        <project>
            <groupId>com.mycompany.app</groupId>
            <artifactId>my-app</artifactId>
            <version>1</version>
        </project>
        """
    );

    @DocumentExample
    @Test
    void addGradleEnterpriseMavenExtensionToExistingExtensionsXmlFile() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
              </project>
              """
          ),
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <extensions>
              </extensions>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <extensions>
                <extension>
                  <groupId>com.gradle</groupId>
                  <artifactId>gradle-enterprise-maven-extension</artifactId>
                  <version>1.17</version>
                </extension>
              </extensions>
              """,
            spec -> spec.path(".mvn/extensions.xml")
          ),
          xml(
            null,
            """
              <gradleEnterprise>
                <server>
                  <url>https://foo</url>
                </server>
              </gradleEnterprise>
              """,
            spec -> spec.path(".mvn/gradle-enterprise.xml")
          )
        );
    }

    @Test
    void createNewExtensionsXmlFileIfNotExist() {
        rewriteRun(
          POM_XML_SOURCE_SPEC,
          xml(
            null,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <extensions>
                <extension>
                  <groupId>com.gradle</groupId>
                  <artifactId>gradle-enterprise-maven-extension</artifactId>
                  <version>1.17</version>
                </extension>
              </extensions>
              """,
            spec -> spec.path(".mvn/extensions.xml")
          ),
          xml(
            null,
            """
              <gradleEnterprise>
                <server>
                  <url>https://foo</url>
                </server>
              </gradleEnterprise>
              """,
            spec -> spec.path(".mvn/gradle-enterprise.xml")
          )
        );
    }


    // if not version of GradleEnterpriseMavenExtension specified, then no version specified in file '.mvn/extensions.xml'
    @Test
    void noVersionSpecified() {
        rewriteRun(
          spec -> spec.recipe(new AddDevelocityMavenExtension(null, "https://foo", null, null, null, null)),
          POM_XML_SOURCE_SPEC,
          xml(
            null,
            spec -> spec.path(".mvn/extensions.xml").after(after -> {
                Matcher versionMatcher = Pattern.compile("<version>(.*)</version>").matcher(after);
                assertThat(versionMatcher.find()).isTrue();
                String version = versionMatcher.group(1);
                assertThat(version).isNotBlank();
                return """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <extensions>
                    <extension>
                      <groupId>com.gradle</groupId>
                      <artifactId>gradle-enterprise-maven-extension</artifactId>
                      <version>%s</version>
                    </extension>
                  </extensions>
                  """.formatted(version);
            })
          ),
          xml(
            null,
            """
              <gradleEnterprise>
                <server>
                  <url>https://foo</url>
                </server>
              </gradleEnterprise>
              """,
            spec -> spec.path(".mvn/gradle-enterprise.xml")
          )
        );
    }

    // No change if the `.mvn/gradle-enterprise.xml` file already exists.
    @Test
    void noChangeIfGradleEnterpriseXmlExists() {
        rewriteRun(
          POM_XML_SOURCE_SPEC,
          xml(
            """
              <?xml version="1.0" encoding="UTF-8" standalone="yes" ?>
              <gradleEnterprise>
              </gradleEnterprise>
              """,
            spec -> spec.path(".mvn/gradle-enterprise.xml")
          )
        );
    }

    @Test
    void allSettings() {
        rewriteRun(
          spec -> spec.recipe(new AddDevelocityMavenExtension("1.17", "https://foo", true, true, false, AddDevelocityMavenExtension.PublishCriteria.Failure)),
          POM_XML_SOURCE_SPEC,
          xml(
            null,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <extensions>
                <extension>
                  <groupId>com.gradle</groupId>
                  <artifactId>gradle-enterprise-maven-extension</artifactId>
                  <version>1.17</version>
                </extension>
              </extensions>
              """,
            spec -> spec.path(".mvn/extensions.xml")
          ),
          xml(
            null,
            """
              <gradleEnterprise>
                <server>
                  <url>https://foo</url>
                  <allowUntrusted>true</allowUntrusted>
                </server>
                <buildScan>
                  <backgroundBuildScanUpload>false</backgroundBuildScanUpload>
                  <publish>ON_FAILURE</publish>
                  <capture>
                    <goalInputFiles>true</goalInputFiles>
                  </capture>
                </buildScan>
              </gradleEnterprise>
              """,
            spec -> spec.path(".mvn/gradle-enterprise.xml")
          )
        );
    }

}
