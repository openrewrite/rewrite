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
import org.openrewrite.Recipe

class AddManagedDependencyVisitorTest : MavenRecipeTest {
    override val parser: MavenParser = MavenParser.builder()
        .build()

    override val recipe: Recipe
        get() = toRecipe { AddManagedDependencyVisitor("org.apache.logging.log4j", "log4j-bom", "2.17.2",
            "import","pom",null) }

    @Test
    fun `ManagedDependency Exists`() = assertUnchanged(
        before = """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>org.apache.logging.log4j</groupId>
                            <artifactId>log4j-bom</artifactId>
                            <version>2.17.0</version>
                            <type>pom</type>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
            </project>
        """
    )

    @Test
    fun `Add ManagedDependency DependencyManagement Tag does not exist`() = assertChanged(
        before = """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
            </project>
        """,
        after = """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>org.apache.logging.log4j</groupId>
                            <artifactId>log4j-bom</artifactId>
                            <version>2.17.2</version>
                            <type>pom</type>
                            <scope>import</scope>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
            </project>
        """
    )

    @Test
    fun `Add ManagedDependency DependencyManagement Tag exists`() = assertChanged(
        before = """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencyManagement/>
            </project>
        """,
        after = """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>org.apache.logging.log4j</groupId>
                            <artifactId>log4j-bom</artifactId>
                            <version>2.17.2</version>
                            <type>pom</type>
                            <scope>import</scope>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
            </project>
        """
    )
}
