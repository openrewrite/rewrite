package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.AddDependency;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class LombokUtilityClassTest implements RewriteTest {

    @Test
    void happyPath1() {
        rewriteRun(
                recipeSpec -> recipeSpec
                        .recipes(
                                addDependency("org.projectlombok:lombok:1.18.28", "false"),
                                new LombokUtilityClass()
                        ),
                java(
                        """
                                public class A {
                                   public static int add(final int x, final int y) {
                                      return x + y;
                                   }
                                }
                                """,
                        """
                                import lombok.experimental.UtilityClass;
                                
                                @UtilityClass
                                public class A {
                                   public int add(final int x, final int y) {
                                      return x + y;
                                   }
                                }
                                """
                )
        );
    }

    private AddDependency addDependency(String gav, String onlyIfUsing) {
        return addDependency(gav, onlyIfUsing, null, null);
    }

    private AddDependency addDependency(String gav, String onlyIfUsing, Boolean acceptTransitive) {
        return addDependency(gav, onlyIfUsing, null, acceptTransitive);
    }

    private AddDependency addDependency(String gav, String onlyIfUsing, @Nullable String scope) {
        return addDependency(gav, onlyIfUsing, scope, null);
    }

    private AddDependency addDependency(String gav, String onlyIfUsing, @Nullable String scope, @Nullable Boolean acceptTransitive) {
        String[] gavParts = gav.split(":");
        return new AddDependency(gavParts[0], gavParts[1], gavParts[2], null, scope, true, onlyIfUsing, null, null,
                false, null, acceptTransitive);
    }
}