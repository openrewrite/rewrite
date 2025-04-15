package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;


public class AddExplicitImportTest implements RewriteTest {

    @Test
    void addExplicitImportWhenNoExistingImports() {
        rewriteRun(spec -> spec.recipe(new AddExplicitImport("foo.bar"))
          , java("""
            class Dummy {}
            """, """
            import foo.bar;

            class Dummy {}
            """));
    }
}
