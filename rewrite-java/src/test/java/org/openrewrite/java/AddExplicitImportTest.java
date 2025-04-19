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

    @Test
    void addExplicitImportWhenExistingImports() {
        rewriteRun(spec -> spec.recipe(new AddExplicitImport("foo.bar"))
          , java("""
            import xyz.bbb.ccc.D;

            class Dummy {}
            """, """
            import xyz.bbb.ccc.D;
            import foo.bar;

            class Dummy {}
            """));
    }

    @Test
    void addStaticImports() {
        rewriteRun(spec -> spec.recipe(new AddExplicitImport("static foo.bar.staticMethod"))
          , java("""
            import xyz.bbb.ccc.D;

            class Dummy {}
            """, """
            import xyz.bbb.ccc.D;
            import static foo.bar.staticMethod;

            class Dummy {}
            """));
    }
}
