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

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Recipe

class OrderPomElementsTest : MavenRecipeTest {

    override val recipe: Recipe
        get() = OrderPomElements()

    @Test
    fun validOrderNoChange() = assertUnchanged(
        before = """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <parent>
                    <groupId>org.example</groupId>
                    <artifactId>parent-pom-test</artifactId>
                    <version>1.0-SNAPSHOT</version>
                </parent>

                <groupId>my.org.proejct</groupId>
                <artifactId>my-project</artifactId>
                <version>4.3.0</version>
                
                <name>Some Project</name>
                <description>Some project desc</description>
                
                <properties>
                </properties>
                
                <dependencyManagement>
                    <dependencies>
                    </dependencies>
                </dependencyManagement>
                
                <dependencies>
                </dependencies>

                <repositories>
                </repositories>
                
                <pluginRepositories>
                </pluginRepositories>
                
                <build>
                </build>
            </project>
        """
    )
    @Test
    fun updateOrder() = assertChanged(
        before = """
            <project>
                <parent>
                    <groupId>org.example</groupId>
                    <artifactId>parent-pom-test</artifactId>
                    <version>1.0-SNAPSHOT</version>
                </parent>
                <modelVersion>4.0.0</modelVersion>
                <artifactId>my-project</artifactId>
                <groupId>my.org.project</groupId>
                <version>4.3.0</version>
                <properties>
                </properties>
                <description>Some project desc</description>
                <name>Some Project</name>
                <dependencies>
                    <dependency>
                        <!-- artifact content
                            comment -->
                        <artifactId>my-project</artifactId>
                        <!-- group content -->
                        <groupId>my.org.project</groupId>
                        <!-- version content -->
                        <version>4.3.0</version>
                    </dependency>
                </dependencies>
                <dependencyManagement>
                    <dependency>
                        <version>2</version>
                        <groupId>my.org.project</groupId>
                        <artifactId>my-project-thing</artifactId>
                    </dependency>
                </dependencyManagement>
                <repositories>
                </repositories>
                <pluginRepositories>
                </pluginRepositories>
                <build>
                </build>
            </project>
        """,
        after = """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <parent>
                    <groupId>org.example</groupId>
                    <artifactId>parent-pom-test</artifactId>
                    <version>1.0-SNAPSHOT</version>
                </parent>
                <groupId>my.org.project</groupId>
                <artifactId>my-project</artifactId>
                <version>4.3.0</version>
                <name>Some Project</name>
                <description>Some project desc</description>
                <properties>
                </properties>
                <dependencyManagement>
                    <dependency>
                        <groupId>my.org.project</groupId>
                        <artifactId>my-project-thing</artifactId>
                        <version>2</version>
                    </dependency>
                </dependencyManagement>
                <dependencies>
                    <dependency>
                        <!-- group content -->
                        <groupId>my.org.project</groupId>
                        <!-- artifact content
                            comment -->
                        <artifactId>my-project</artifactId>
                        <!-- version content -->
                        <version>4.3.0</version>
                    </dependency>
                </dependencies>
                <repositories>
                </repositories>
                <pluginRepositories>
                </pluginRepositories>
                <build>
                </build>
            </project>
        """
    )

    @Disabled
    @Test
    fun updateOrderCorrectNewLines() = assertChanged(
        before = """
            <project>
                <parent>
                    <groupId>org.example</groupId>
                    <artifactId>parent-pom-test</artifactId>
                    <version>1.0-SNAPSHOT</version>
                </parent>
                
                <modelVersion>4.0.0</modelVersion>
                
                <artifactId>my-project</artifactId>
                <groupId>my.org.project</groupId>
                <version>4.3.0</version>
                
                <properties>
                </properties>
                
                <description>Some project desc</description>
                <name>Some Project</name>
                
                <dependencies>
                    <dependency>
                        <!-- artifact content
                            comment -->
                        <artifactId>my-project</artifactId>
                        <!-- group content -->
                        <groupId>my.org.project</groupId>
                        <!-- version content -->
                        <version>4.3.0</version>
                    </dependency>
                </dependencies>
                <dependencyManagement>
                    <dependency>
                        <version>2</version>
                        <groupId>my.org.project</groupId>
                        <artifactId>my-project-thing</artifactId>
                    </dependency>
                </dependencyManagement>
                
                <repositories>
                </repositories>
                <pluginRepositories>
                </pluginRepositories>
                
                <build>
                </build>
            </project>
        """,
        after = """
            <project>
                <modelVersion>4.0.0</modelVersion>
                
                <parent>
                    <groupId>org.example</groupId>
                    <artifactId>parent-pom-test</artifactId>
                    <version>1.0-SNAPSHOT</version>
                </parent>
                
                <groupId>my.org.project</groupId>
                <artifactId>my-project</artifactId>
                <version>4.3.0</version>
                
                <name>Some Project</name>
                <description>Some project desc</description>
                
                <properties>
                </properties>
                
                <dependencyManagement>
                    <dependency>
                        <groupId>my.org.project</groupId>
                        <artifactId>my-project-thing</artifactId>
                        <version>2</version>
                    </dependency>
                </dependencyManagement>
                <dependencies>
                    <dependency>
                        <!-- group content -->
                        <groupId>my.org.project</groupId>
                        <!-- artifact content
                            comment -->
                        <artifactId>my-project</artifactId>
                        <!-- version content -->
                        <version>4.3.0</version>
                    </dependency>
                </dependencies>
                
                <repositories>
                </repositories>
                <pluginRepositories>
                </pluginRepositories>
                
                <build>
                </build>
            </project>
        """
    )
}