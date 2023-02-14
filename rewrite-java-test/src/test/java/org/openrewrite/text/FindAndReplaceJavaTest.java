package org.openrewrite.text;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class FindAndReplaceJavaTest implements RewriteTest {

    @Test
    void findAndReplaceJava() {
        rewriteRun(
          spec -> spec.recipe(new FindAndReplace("Test", "Replaced", null, null)),
          java(
            "class Test {}",
            "class Replaced {}"
          )
        );
    }
}
