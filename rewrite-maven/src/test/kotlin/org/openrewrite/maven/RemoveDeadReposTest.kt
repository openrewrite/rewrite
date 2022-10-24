package org.openrewrite.maven

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe

class RemoveDeadReposTest : MavenRecipeTest {

    final class IdentifyAndRemoveRecipe: Recipe {
        override fun getDisplayName() = "Identify and remove"
        constructor() {
                doNext(IdentifyUnreachableRepos())
                doNext(RemoveDeadRepos())
        }
    }

    @Test
    fun `replace https maven glassfish dead repo` () = assertChanged(
        recipe = RemoveDeadRepos(),
        before = """
                <project>
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
                    <repositories>
                        <repository>
                            <id>central</id>
                            <url>https://maven.java.net</url>
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
    fun `remove (fake) abcmaven dead repo` () = assertChanged(
        recipe = RemoveDeadRepos(),
        before = """
                <project>
                    <repositories>
                        <repository>
                            <id>abcmaven</id>
                            <url>https://abcmaven.com</url>
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
    fun `replace https maven glassfish dead repo link in properties` () = assertChanged(
        recipe = RemoveDeadRepos(),
        before = """
                <project>
                    <properties>
                        <maven.repo.url>https://maven.glassfish.org/content/groups/public</maven.repo.url>
                    </properties>
                    <repositories>
                        <repository>
                            <id>central</id>
                            <url>${'$'}{maven.repo.url}</url>
                        </repository>
                        <repository>
                            <id>spring-snapshots</id>
                            <url>${'$'}{maven.repo.url}</url>
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
                        <maven.repo.url>https://maven.java.net</maven.repo.url>
                    </properties>
                    <repositories>
                        <repository>
                            <id>central</id>
                            <url>${'$'}{maven.repo.url}</url>
                        </repository>
                        <repository>
                            <id>spring-snapshots</id>
                            <url>${'$'}{maven.repo.url}</url>
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
    fun `remove https (fake) abcmaven dead repo link in properties and all associated repos` () = assertChanged(
        recipe = RemoveDeadRepos(),
        before = """
                <project>
                    <properties>
                        <abc.repo.url>https://abcmaven.com</abc.repo.url>
                    </properties>
                    <repositories>
                        <repository>
                            <id>abcmaven</id>
                            <url>${'$'}{abc.repo.url}</url>
                        </repository>
                        <repository>
                            <id>spring-snapshots</id>
                            <url>${'$'}{abc.repo.url}</url>
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
                    </properties>
                    <repositories>
                        <repository>
                            <id>spring-milestones</id>
                            <url>https://repo.spring.io/milestone</url>
                        </repository>
                    </repositories>
                </project>
            """
    )

    @Test
    fun `no dead repos` () = assertUnchanged(
        recipe = RemoveDeadRepos(),
        before = """
                <project>
                    <repositories>
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


}
