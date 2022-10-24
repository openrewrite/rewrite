/*
 * Copyright 2022 the original author or authors.
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

class IdentifyUnreachableReposTest : MavenRecipeTest {

    // HTTPS yields "ambiguous"
    @Test
    fun `https maven glassfish dead repo` () = assertChanged(
        recipe = IdentifyUnreachableRepos(),
        before = """
                <project>
                    <modelVersion>4.0.0</modelVersion>

                    <modelVersion>4.0.0</modelVersion>

                    <groupId>org.openrewrite.example</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    
                    <repositories>
                        <repository>
                            <id>central</id>
                            <url>https://maven.glassfish.org/content/groups/public</url>
                        </repository>
                        <repository>
                            <id>spring-snapshots</id>
                            <url>https://repo.spring.io/snapshot</url>
                        </repository>
                        <repository>
                            <id>spring-milestones</id>
                            <url>https://repo.spring.io/milestone</url>
                        </repository>
                    </repositories>
                </project>
            """,
        after = """
                <project>
                    <modelVersion>4.0.0</modelVersion>

                    <modelVersion>4.0.0</modelVersion>

                    <groupId>org.openrewrite.example</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    
                    <repositories>
                        <repository>
                            <id>central</id>
                            <!--~~(replacement)~~>--><url>https://maven.glassfish.org/content/groups/public</url>
                        </repository>
                        <repository>
                            <id>spring-snapshots</id>
                            <url>https://repo.spring.io/snapshot</url>
                        </repository>
                        <repository>
                            <id>spring-milestones</id>
                            <url>https://repo.spring.io/milestone</url>
                        </repository>
                    </repositories>
                </project>
            """
    )

    // HTTP yields "dead"
    @Test
    fun `http maven glassfish dead repo within property` () = assertChanged(
        recipe = IdentifyUnreachableRepos(),
        before = """
                <project>
                    <properties>
                        <my-repo-url>http://maven.glassfish.org/content/groups/public</my-repo-url>
                    </properties>
                    <repositories>
                        <repository>
                            <id>central</id>
                            <url>${'$'}{my-repo-url}</url>
                        </repository>
                        <repository>
                            <id>spring-snapshots</id>
                            <url>https://repo.spring.io/snapshot</url>
                        </repository>
                        <repository>
                            <id>spring-milestones</id>
                            <url>https://repo.spring.io/milestone</url>
                        </repository>
                    </repositories>
                </project>
            """,
        after = """
                <project>
                    <properties>
                        <!--~~(replacement)~~>--><my-repo-url>http://maven.glassfish.org/content/groups/public</my-repo-url>
                    </properties>
                    <repositories>
                        <repository>
                            <id>central</id>
                            <url>${'$'}{my-repo-url}</url>
                        </repository>
                        <repository>
                            <id>spring-snapshots</id>
                            <url>https://repo.spring.io/snapshot</url>
                        </repository>
                        <repository>
                            <id>spring-milestones</id>
                            <url>https://repo.spring.io/milestone</url>
                        </repository>
                    </repositories>
                </project>
            """
    )

    // HTTP yields "dead"
    @Test
    fun `http maven glassfish dead repo` () = assertChanged(
        recipe = IdentifyUnreachableRepos(),
        before = """
                <project>
                    <repositories>
                        <repository>
                            <id>central</id>
                            <url>http://maven.glassfish.org/content/groups/public</url>
                        </repository>
                        <repository>
                            <id>spring-snapshots</id>
                            <url>https://repo.spring.io/snapshot</url>
                        </repository>
                        <repository>
                            <id>spring-milestones</id>
                            <url>https://repo.spring.io/milestone</url>
                        </repository>
                    </repositories>
                </project>
            """,
        after = """
                <project>
                    <repositories>
                        <repository>
                            <id>central</id>
                            <!--~~(replacement)~~>--><url>http://maven.glassfish.org/content/groups/public</url>
                        </repository>
                        <repository>
                            <id>spring-snapshots</id>
                            <url>https://repo.spring.io/snapshot</url>
                        </repository>
                        <repository>
                            <id>spring-milestones</id>
                            <url>https://repo.spring.io/milestone</url>
                        </repository>
                    </repositories>
                </project>
            """
    )

    @Test
    fun removeNoDead() = assertUnchanged(
        recipe = IdentifyUnreachableRepos(),
        before = """
                <project>
                    <repositories>
                        <repository>
                            <id>central</id>
                            <url>https://repo.maven.apache.org/maven2</url>
                        </repository>
                    </repositories>
                    <properties>
                        <spring-repo-snapshot>https://repo.spring.io/snapshot</spring-repo-snapshot>
                        <spring-repo-milestone>https://repo.spring.io/milestone</spring-repo-milestone>
                    </properties>
                </project>
            """
    )
}
