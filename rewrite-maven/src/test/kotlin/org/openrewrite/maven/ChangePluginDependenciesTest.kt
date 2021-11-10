/*
 * Copyright 2021 the original author or authors.
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

import org.junit.jupiter.api.Test

class ChangePluginDependenciesTest: MavenRecipeTest {

    @Test
    fun removeConfiguration() = assertChanged(
        recipe = ChangePluginDependencies("org.openrewrite.maven", "rewrite-maven-plugin", null),
        before = """
            <project>
                <groupId>org.example</groupId>
                <artifactId>foo</artifactId>
                <version>1.0</version>
                
                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.openrewrite.maven</groupId>
                            <artifactId>rewrite-maven-plugin</artifactId>
                            <version>4.1.5</version>
                            <dependencies>
                                <dependency>
                                    <groupId>org.openrewrite.recipe</groupId>
                                    <artifactId>rewrite-spring</artifactId>
                                    <version>1.0.0</version>
                                </dependency>
                            </dependencies>
                        </plugin>
                    </plugins>
                </build>
            </project>
        """,
        after = """
            <project>
                <groupId>org.example</groupId>
                <artifactId>foo</artifactId>
                <version>1.0</version>
                
                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.openrewrite.maven</groupId>
                            <artifactId>rewrite-maven-plugin</artifactId>
                            <version>4.1.5</version>
                        </plugin>
                    </plugins>
                </build>
            </project>
        """
    )

    @Test
    fun addConfiguration() = assertChanged(
        recipe = ChangePluginDependencies(
            "org.openrewrite.maven",
            "rewrite-maven-plugin",
            "org.openrewrite.recipe:rewrite-spring:1.0.0"),
        before = """
            <project>
                <groupId>org.example</groupId>
                <artifactId>foo</artifactId>
                <version>1.0</version>
                
                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.openrewrite.maven</groupId>
                            <artifactId>rewrite-maven-plugin</artifactId>
                            <version>4.1.5</version>
                        </plugin>
                    </plugins>
                </build>
            </project>
        """,
        after = """
            <project>
                <groupId>org.example</groupId>
                <artifactId>foo</artifactId>
                <version>1.0</version>
                
                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.openrewrite.maven</groupId>
                            <artifactId>rewrite-maven-plugin</artifactId>
                            <version>4.1.5</version>
                            <dependencies>
                                <dependency>
                                    <groupId>org.openrewrite.recipe</groupId>
                                    <artifactId>rewrite-spring</artifactId>
                                    <version>1.0.0</version>
                                </dependency>
                            </dependencies>
                        </plugin>
                    </plugins>
                </build>
            </project>
        """
    )

    @Test
    fun replaceConfiguration() = assertChanged(
        recipe = ChangePluginDependencies(
            "org.openrewrite.maven",
            "rewrite-maven-plugin",
            "org.openrewrite.recipe:rewrite-spring:1.0.0, org.openrewrite.recipe:rewrite-testing-frameworks:1.0.0"),
        before = """
            <project>
                <groupId>org.example</groupId>
                <artifactId>foo</artifactId>
                <version>1.0</version>
                
                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.openrewrite.maven</groupId>
                            <artifactId>rewrite-maven-plugin</artifactId>
                            <version>4.1.5</version>
                            <dependencies />
                        </plugin>
                    </plugins>
                </build>
            </project>
        """,
        after = """
            <project>
                <groupId>org.example</groupId>
                <artifactId>foo</artifactId>
                <version>1.0</version>
                
                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.openrewrite.maven</groupId>
                            <artifactId>rewrite-maven-plugin</artifactId>
                            <version>4.1.5</version>
                            <dependencies>
                                <dependency>
                                    <groupId>org.openrewrite.recipe</groupId>
                                    <artifactId>rewrite-spring</artifactId>
                                    <version>1.0.0</version>
                                </dependency>
                                <dependency>
                                    <groupId>org.openrewrite.recipe</groupId>
                                    <artifactId>rewrite-testing-frameworks</artifactId>
                                    <version>1.0.0</version>
                                </dependency>
                            </dependencies>
                        </plugin>
                    </plugins>
                </build>
            </project>
        """
    )
}
