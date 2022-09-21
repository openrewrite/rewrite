package org.openrewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.tree.ParsingExecutionContextView;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.SourceSpecs.text;

public class ParserTest implements RewriteTest {

    @Test
    void overrideCharset() {
        rewriteRun(
                spec -> spec.executionContext(ParsingExecutionContextView
                        .view(new InMemoryExecutionContext())
                        .setCharset(ISO_8859_1)),
                text(
                        "À1",
                        spec -> spec.beforeRecipe(txt ->
                                assertThat(txt.getCharset()).isEqualTo(ISO_8859_1))
                )
        );
    }
}
