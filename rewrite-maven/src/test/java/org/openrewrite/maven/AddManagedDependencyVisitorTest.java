package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.test.RewriteTest.toRecipe;

public class AddManagedDependencyVisitorTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new AddManagedDependencyVisitor("org.apache.logging.log4j", "log4j-bom", "2.17.2",
          "import","pom",null)));
    }

    @Test
    void alreadyExists() {
        rewriteRun(
          pomXml(
            """
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
        );
    }

    @Test
    void newDependencyManagementTag() {
        rewriteRun(
          spec -> spec.cycles(1).expectedCyclesThatMakeChanges(1),
          pomXml(
            """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                </project>
            """,
            """
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
        );
    }

    @Test
    void dependencyManagementTagExists() {
        rewriteRun(
          spec -> spec.cycles(1).expectedCyclesThatMakeChanges(1),
          pomXml(
            """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <dependencyManagement/>
                </project>
            """,
            """
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
        );
    }
}
