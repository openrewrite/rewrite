package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class Java21ParserTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "jackson-annotations"));
    }

    @Test
    void shouldCompile() {
        rewriteRun(
                //language=java
                java(
                        """
                                import com.fasterxml.jackson.annotation.JsonInclude;
                                class Test {
                                    @JsonInclude(value = JsonInclude.Include.NON_NULL)
                                    String text;
                                }
                                """
                )
        );
    }
}