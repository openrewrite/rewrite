package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

public class IdentifyUnreachableReposTest2 implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new IdentifyUnreachableRepos());
    }

    // HTTPS yields "ambiguous"
    @Test
    void httpsMavenGlassfishDeadRepo() {
        rewriteRun(
          pomXml("""
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
            """
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
        );
    }

    // HTTP yields "dead"
    @Test
    void httpMavenGlassfishDeadRepoWithinProperty() {
        rewriteRun(
          pomXml(
            """
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
            """
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
            ));
    }

    // HTTP yields "dead"
    @Test
    void httpMavenGlassfishDeadRepo() {
        rewriteRun(
          pomXml("""
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
            """, """
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
        );
    }

    @Test
    void removeNoDead() {
        rewriteRun(
          pomXml(
            """
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
        );

    }

}
