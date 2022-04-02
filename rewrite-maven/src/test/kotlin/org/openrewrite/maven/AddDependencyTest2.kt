package org.openrewrite.maven

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.openrewrite.java.JavaParser
import org.openrewrite.maven.tree.Scope
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

class AddDependencyTest2 : RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        spec.allSources().java().project("myproject")
    }

    @ParameterizedTest
    @ValueSource(strings = ["com.google.common.math.*", "com.google.common.math.IntMath"])
    fun addDependency(onlyIfUsing: String) = rewriteRun(
        { spec ->
            spec
                .recipe(addDependency("com.google.guava:guava:29.0-jre", onlyIfUsing))
                .parser(JavaParser.fromJavaVersion().classpath("guava").build())
        },
        java(
            """
                import com.google.common.math.IntMath;
                public class A {
                    boolean getMap() {
                        //noinspection UnstableApiUsage
                        return IntMath.isPrime(5);
                    }
                }
            """,
        ),
        mavenChange(
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
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>29.0-jre</version>
                        </dependency>
                    </dependencies>
                </project>
            """
        ) { spec ->
            spec.afterRecipe { maven ->
                assertThat(
                    maven.mavenResolutionResult()
                        .findDependencies("com.google.guava", "guava", Scope.Compile)
                ).isNotEmpty()
            }
        }
    )

    private fun addDependency(gav: String, onlyIfUsing: String): AddDependency {
        val (group, artifact, version) = gav.split(":")
        return AddDependency(
            group, artifact, version, null, null, true,
            onlyIfUsing, null, null, false, null
        )
    }
}
