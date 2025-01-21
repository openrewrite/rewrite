package org.openrewrite.json.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.json.Assertions.json;

public class NewLinesVisitorTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NewLines());
    }

    @Test
    void simple() {
        rewriteRun(
          json(
            """
            {"a": 3,"b": 5}
            """,
            """
            {
            "a": 3,
            "b": 5
            }
            """
          )
        );
    }

    @Test
    void keepWrongIndentsIfTheLinesAreFine() {
        rewriteRun(
          json(
            """
            {
               "x": "x",
                   "key": {
               "a": "b"
            }
            }
            """
          )
        );
    }

    @Test
    void splitBracesToNewLines() {
        rewriteRun(
          json(
            """
            {
               "x": "x",
               "key": {"a": "b"}}
            """,
            """
            {
               "x": "x",
               "key": {
            "a": "b"
            }
            }
            """
          )
        );
    }
}
