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
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.maven.tree.Pom
import org.openrewrite.maven.tree.Scope

class MavenParserTest {

    @Test
    fun parse() {
        val pom = """
            <project>
                <modelVersion>4.0.0</modelVersion>
            
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <developers>
                    <developer>
                        <name>Trygve Laugst&oslash;l</name>
                    </developer>
                </developers>

                <dependencies>
                  <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter-api</artifactId>
                    <version>5.7.0</version>
                    <scope>test</scope>
                  </dependency>
                </dependencies>
            </project>
        """.trimIndent()

        val parser = MavenParser.builder().build()

        val maven = parser.parse(pom)[0]

        assertThat(maven.getMetadata(Pom::class.java)!!.dependencies.first().model.licenses.first()?.type)
                .isEqualTo(Pom.LicenseType.Eclipse)
    }

    @Test
    fun emptyArtifactPolicy() {
        // example from https://repo1.maven.org/maven2/org/openid4java/openid4java-parent/0.9.6/openid4java-parent-0.9.6.pom
        MavenParser.builder().build().parse("""
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <repositories>
                    <repository>
                        <id>alchim.snapshots</id>
                        <name>Achim Repository Snapshots</name>
                        <url>http://alchim.sf.net/download/snapshots</url>
                        <snapshots/>
                    </repository>
                </repositories>
            </project>
        """.trimIndent())
    }

    @Test
    fun handlesRepositories() {
        MavenParser.builder().build().parse("""
            <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>org.openrewrite.maven</groupId>
                <artifactId>single-project</artifactId>
                <version>0.1.0-SNAPSHOT</version>

                <properties>
                    <maven.compiler.source>1.8</maven.compiler.source>
                    <maven.compiler.target>1.8</maven.compiler.target>
                </properties>

                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>29.0-jre</version>
                    </dependency>
                    <dependency>
                        <groupId>org.openrewrite.recipe</groupId>
                        <artifactId>rewrite-checkstyle</artifactId>
                        <version>2.0.1</version>
                    </dependency>
                </dependencies>

                <repositories>
                    <repository>
                        <id>jcenter</id>
                        <name>JCenter</name>
                        <url>https://jcenter.bintray.com/</url>
                    </repository>
                    <repository>
                        <id>bintray</id>
                        <name>Bintray</name>
                        <url>https://dl.bintray.com/openrewrite/maven</url>
                    </repository>
                </repositories>
            </project>

        """)
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/198")
    @Test
    fun handlesPropertiesInDependencyScope() {
        val maven = MavenParser.builder().build().parse("""
            <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>org.openrewrite.maven</groupId>
                <artifactId>single-project</artifactId>
                <version>0.1.0-SNAPSHOT</version>

                <properties>
                    <dependency.scope>compile</dependency.scope>
                </properties>

                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>29.0-jre</version>
                        <scope>${"$"}{dependency.scope}</scope>
                    </dependency>
                </dependencies>
            </project>

        """).first()
        assertThat(maven.model.dependencies).hasSize(1)
        assertThat(maven.model.dependencies.first()).matches{ it.scope == Scope.Compile}
    }

    val parserStrict = MavenParser.builder().resolveOptional(false).build()
    val parserLenient = MavenParser.builder().continueOnError(true).resolveOptional(false).build()

    @Issue("https://github.com/openrewrite/rewrite/issues/199")
    @Test
    fun continueOnErrorInvalidScope() {
        val invalidPom = """
            <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>org.openrewrite.maven</groupId>
                <artifactId>single-project</artifactId>
                <version>0.1.0-SNAPSHOT</version>

                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>29.0-jre</version>
                        <scope>${"$"}{dependency.scope}</scope>
                    </dependency>
                </dependencies>
            </project>
        """
        assertThatThrownBy { parserStrict.parse(invalidPom) }
        parserLenient.parse(invalidPom)
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/199")
    @Test
    fun continueOnErrorMissingGroupId() {
        val invalidPom = """
            <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>org.openrewrite.maven</groupId>
                <artifactId>single-project</artifactId>
                <version>0.1.0-SNAPSHOT</version>

                <dependencies>
                    <dependency>
                        <artifactId>guava</artifactId>
                        <version>29.0-jre</version>
                    </dependency>
                </dependencies>
            </project>
        """
        assertThatThrownBy { parserStrict.parse(invalidPom) }
        parserLenient.parse(invalidPom)
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/199")
    @Test
    fun continueOnErrorMissingVersion() {
        val invalidPom = """
            <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>org.openrewrite.maven</groupId>
                <artifactId>single-project</artifactId>
                <version>0.1.0-SNAPSHOT</version>

                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                    </dependency>
                </dependencies>
            </project>
        """
        assertThatThrownBy { parserStrict.parse(invalidPom) }
        parserLenient.parse(invalidPom)
    }
}
