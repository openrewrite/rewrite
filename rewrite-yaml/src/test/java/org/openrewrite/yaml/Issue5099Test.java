package org.openrewrite.yaml;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.openrewrite.yaml.Assertions.yaml;

class Issue5099Test implements RewriteTest {
    @Test
    void parseFlowSequenceAtBufferBoundary() {
        // May change over time in SnakeYaml, rendering this test fragile
        var snakeYamlEffectiveStreamReaderBufferSize = 1024 - 1;

        @Language("yml")
        var yaml = "a: " + "x".repeat(1000) + "\n" + "b".repeat(16) + ": []";
        assertEquals(snakeYamlEffectiveStreamReaderBufferSize - 1, yaml.lastIndexOf('['));

        rewriteRun(
            spec -> spec.recipe(new DeleteKey(".nonexistent","*")),
            yaml(yaml)
        );
    }
}
