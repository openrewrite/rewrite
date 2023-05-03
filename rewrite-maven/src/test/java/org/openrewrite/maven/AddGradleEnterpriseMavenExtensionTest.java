package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;

import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.xml.Assertions.xml;

class AddGradleEnterpriseMavenExtensionTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddGradleEnterpriseMavenExtension("1.17", "https://foo", true));
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

    @Test
    void addGradleEnterpriseMavenExtensionToExistingExtensionsXmlFile() {
        rewriteRun(
          POM_XML_SOURCE_SPEC,
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
              <?xml version="1.0" encoding="UTF-8" standalone="yes" ?>
              <gradleEnterprise
                  xmlns="https://www.gradle.com/gradle-enterprise-maven" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="https://www.gradle.com/gradle-enterprise-maven https://www.gradle.com/schema/gradle-enterprise-maven.xsd">
                <server>
                  <url>https://foo</url>
                  <allowUntrusted>true</allowUntrusted>
                </server>
                <buildScan>
                  <backgroundBuildScanUpload>false</backgroundBuildScanUpload>
                  <publish>ALWAYS</publish>
                </buildScan>
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
              <?xml version="1.0" encoding="UTF-8" standalone="yes" ?>
              <gradleEnterprise
                  xmlns="https://www.gradle.com/gradle-enterprise-maven" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="https://www.gradle.com/gradle-enterprise-maven https://www.gradle.com/schema/gradle-enterprise-maven.xsd">
                <server>
                  <url>https://foo</url>
                  <allowUntrusted>true</allowUntrusted>
                </server>
                <buildScan>
                  <backgroundBuildScanUpload>false</backgroundBuildScanUpload>
                  <publish>ALWAYS</publish>
                </buildScan>
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
          spec -> spec.recipe(new AddGradleEnterpriseMavenExtension(null, "https://foo", true)),
          POM_XML_SOURCE_SPEC,
          xml(
            null,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <extensions>
                <extension>
                  <groupId>com.gradle</groupId>
                  <artifactId>gradle-enterprise-maven-extension</artifactId>
                </extension>
              </extensions>
              """,
            spec -> spec.path(".mvn/extensions.xml")
          ),
          xml(
            null,
            """
              <?xml version="1.0" encoding="UTF-8" standalone="yes" ?>
              <gradleEnterprise
                  xmlns="https://www.gradle.com/gradle-enterprise-maven" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="https://www.gradle.com/gradle-enterprise-maven https://www.gradle.com/schema/gradle-enterprise-maven.xsd">
                <server>
                  <url>https://foo</url>
                  <allowUntrusted>true</allowUntrusted>
                </server>
                <buildScan>
                  <backgroundBuildScanUpload>false</backgroundBuildScanUpload>
                  <publish>ALWAYS</publish>
                </buildScan>
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
}
