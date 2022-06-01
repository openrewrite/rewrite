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
package org.openrewrite.maven

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

class UpgradeParentVersionTest : MavenRecipeTest, RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/1317")
    @Test
    fun doesNotDowngradeVersion() = assertUnchanged(
        recipe = UpgradeParentVersion(
            "org.springframework.boot",
            "spring-boot-starter-parent",
            "~1.5",
            null
        ),
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              
              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>2.4.12</version>
                <relativePath/> <!-- lookup parent from repository -->
              </parent>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
            </project>
        """
    )


    @Test
    fun shouldUpdateVersion() {
        val recipe = UpgradeParentVersion("org.jenkins-ci.plugins", "plugin", "4.40", null)
        rewriteRun(
            { spec: RecipeSpec -> spec.recipe(recipe) },
            pomXml(
                """
                    <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
                        <modelVersion>4.0.0</modelVersion>
                        <parent>
                            <groupId>org.jenkins-ci.plugins</groupId>
                            <artifactId>plugin</artifactId>
                            <version>4.33</version>
                        </parent>
                        <artifactId>antisamy-markup-formatter</artifactId>
                        <version>1.0.0</version>
                        <packaging>hpi</packaging>
                        <name>OWASP Markup Formatter Plugin</name>
                        <description>Sanitize HTML markup in user-submitted text to be displayed on the Jenkins UI.</description>
                        <url>https://github.com/jenkinsci/antisamy-markup-formatter-plugin</url>
                        <licenses>
                            <license>
                                <name>MIT</name>
                                <url>https://opensource.org/licenses/MIT</url>
                            </license>
                        </licenses>
                        <properties>
                            <changelist>999999-SNAPSHOT</changelist>
                            <gitHubRepo>jenkinsci/antisamy-markup-formatter-plugin</gitHubRepo>
                            <jenkins.version>2.277.4</jenkins.version>
                            <java.level>8</java.level>
                            <hpi.compatibleSinceVersion>2.0</hpi.compatibleSinceVersion>
                        </properties>
                    
                        <repositories>
                            <repository>
                                <id>repo.jenkins-ci.org</id>
                                <url>https://repo.jenkins-ci.org/public/</url>
                            </repository>
                        </repositories>
                        <pluginRepositories>
                            <pluginRepository>
                                <id>repo.jenkins-ci.org</id>
                                <url>https://repo.jenkins-ci.org/public/</url>
                            </pluginRepository>
                        </pluginRepositories>
                        <dependencyManagement>
                            <dependencies>
                                <dependency>
                                    <groupId>io.jenkins.tools.bom</groupId>
                                    <artifactId>bom-2.277.x</artifactId>
                                    <version>961.vf0c9f6f59827</version>
                                    <scope>import</scope>
                                    <type>pom</type>
                                </dependency>
                            </dependencies>
                        </dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>com.googlecode.owasp-java-html-sanitizer</groupId>
                                <artifactId>owasp-java-html-sanitizer</artifactId>
                                <version>20211018.2</version>
                                <exclusions>
                                    <exclusion>
                                        <groupId>com.google.guava</groupId>
                                        <artifactId>guava</artifactId>
                                    </exclusion>
                                </exclusions>
                            </dependency>
                            <!-- JCasC compatibility -->
                            <dependency>
                                <groupId>io.jenkins</groupId>
                                <artifactId>configuration-as-code</artifactId>
                                <scope>test</scope>
                            </dependency>
                            <dependency>
                                <groupId>io.jenkins.configuration-as-code</groupId>
                                <artifactId>test-harness</artifactId>
                                <scope>test</scope>
                            </dependency>
                        </dependencies>
                    </project>
                """,
                """
                    <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
                        <modelVersion>4.0.0</modelVersion>
                        <parent>
                            <groupId>org.jenkins-ci.plugins</groupId>
                            <artifactId>plugin</artifactId>
                            <version>4.40</version>
                        </parent>
                        <artifactId>antisamy-markup-formatter</artifactId>
                        <version>1.0.0</version>
                        <packaging>hpi</packaging>
                        <name>OWASP Markup Formatter Plugin</name>
                        <description>Sanitize HTML markup in user-submitted text to be displayed on the Jenkins UI.</description>
                        <url>https://github.com/jenkinsci/antisamy-markup-formatter-plugin</url>
                        <licenses>
                            <license>
                                <name>MIT</name>
                                <url>https://opensource.org/licenses/MIT</url>
                            </license>
                        </licenses>
                        <properties>
                            <changelist>999999-SNAPSHOT</changelist>
                            <gitHubRepo>jenkinsci/antisamy-markup-formatter-plugin</gitHubRepo>
                            <jenkins.version>2.277.4</jenkins.version>
                            <java.level>8</java.level>
                            <hpi.compatibleSinceVersion>2.0</hpi.compatibleSinceVersion>
                        </properties>
                    
                        <repositories>
                            <repository>
                                <id>repo.jenkins-ci.org</id>
                                <url>https://repo.jenkins-ci.org/public/</url>
                            </repository>
                        </repositories>
                        <pluginRepositories>
                            <pluginRepository>
                                <id>repo.jenkins-ci.org</id>
                                <url>https://repo.jenkins-ci.org/public/</url>
                            </pluginRepository>
                        </pluginRepositories>
                        <dependencyManagement>
                            <dependencies>
                                <dependency>
                                    <groupId>io.jenkins.tools.bom</groupId>
                                    <artifactId>bom-2.277.x</artifactId>
                                    <version>961.vf0c9f6f59827</version>
                                    <scope>import</scope>
                                    <type>pom</type>
                                </dependency>
                            </dependencies>
                        </dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>com.googlecode.owasp-java-html-sanitizer</groupId>
                                <artifactId>owasp-java-html-sanitizer</artifactId>
                                <version>20211018.2</version>
                                <exclusions>
                                    <exclusion>
                                        <groupId>com.google.guava</groupId>
                                        <artifactId>guava</artifactId>
                                    </exclusion>
                                </exclusions>
                            </dependency>
                            <!-- JCasC compatibility -->
                            <dependency>
                                <groupId>io.jenkins</groupId>
                                <artifactId>configuration-as-code</artifactId>
                                <scope>test</scope>
                            </dependency>
                            <dependency>
                                <groupId>io.jenkins.configuration-as-code</groupId>
                                <artifactId>test-harness</artifactId>
                                <scope>test</scope>
                            </dependency>
                        </dependencies>
                    </project>
                """
            )
        )
    }


    @Test
    fun upgradeVersion() = assertChanged(
        recipe = UpgradeParentVersion(
            "org.springframework.boot",
            "spring-boot-starter-parent",
            "~1.5",
            null
        ),
        before = """
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
        after = """
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

    @Test
    fun upgradeToExactVersion() = assertChanged(
        recipe = UpgradeParentVersion(
            "org.springframework.boot",
            "spring-boot-starter-parent",
            "1.5.22.RELEASE",
            null
        ),
        before = """
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
        after = """
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

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = UpgradeParentVersion(null, null, null, null)
        var valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(3)
        assertThat(valid.failures()[0].property).isEqualTo("artifactId")
        assertThat(valid.failures()[1].property).isEqualTo("groupId")
        assertThat(valid.failures()[2].property).isEqualTo("newVersion")

        recipe = UpgradeParentVersion(null, "rewrite-maven", "latest.release", null)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("groupId")

        recipe = UpgradeParentVersion("org.openrewrite", null, null, null)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(2)
        assertThat(valid.failures()[0].property).isEqualTo("artifactId")
        assertThat(valid.failures()[1].property).isEqualTo("newVersion")

        recipe = UpgradeParentVersion("org.openrewrite", "rewrite-maven", "latest.release", null)
        valid = recipe.validate()
        assertThat(valid.isValid).isTrue()

        recipe = UpgradeParentVersion("org.openrewrite", "rewrite-maven", "latest.release", "123")
        valid = recipe.validate()
        assertThat(valid.isValid).isTrue()

        recipe = UpgradeParentVersion("org.springframework.boot", "spring-boot-starter-parent", "1.5.22.RELEASE", null)
        valid = recipe.validate()
        assertThat(valid.isValid).isTrue()
    }
}
