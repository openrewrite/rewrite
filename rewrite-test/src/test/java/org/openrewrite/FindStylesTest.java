/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.SourceSpecs.text;

class FindStylesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindStyles());
    }

    @DocumentExample
    @Test
    void findNoStyles() {
        rewriteRun(
          text(
            "hello world!",
            "~~(No styles attached)~~>hello world!",
            spec -> spec.path("hello.txt")
          )
        );
    }

    @Test
    void findAttachedStyles() {
        // Create a simple test style
        TestStyle testStyle = new TestStyle(4, true);
        NamedStyles namedStyles = new NamedStyles(
            Tree.randomId(),
            "org.openrewrite.test.TestStyles",
            "Test Styles",
            "Test styles for unit testing",
            Collections.emptySet(),
            Collections.singletonList(testStyle)
        );

        // Parse a simple text file
        PlainText plainText = PlainTextParser.builder().build()
            .parse("hello world!")
            .map(PlainText.class::cast)
            .findFirst()
            .orElseThrow()
            .withSourcePath(Paths.get("hello.txt"));

        // Attach the styles
        plainText = plainText.withMarkers(plainText.getMarkers().add(namedStyles));

        // Run the recipe
        FindStyles recipe = new FindStyles();
        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        RecipeRun run = recipe.run(new InMemoryLargeSourceSet(List.of(plainText)), ctx);

        // Verify the output
        assertThat(run.getChangeset().getAllResults()).hasSize(1);
        String content = run.getChangeset().getAllResults().get(0).getAfter().printAll();
        assertThat(content).contains("~~(");
        assertThat(content).contains("org.openrewrite.test.TestStyles");
        assertThat(content).contains("TestStyle");
        assertThat(content).contains("\"indentSize\" : 4");
        assertThat(content).contains("\"useTabs\" : true");
    }

    /**
     * A simple test style class for testing purposes.
     */
    private static class TestStyle implements Style {
        private final int indentSize;
        private final boolean useTabs;

        TestStyle(int indentSize, boolean useTabs) {
            this.indentSize = indentSize;
            this.useTabs = useTabs;
        }

        public int getIndentSize() {
            return indentSize;
        }

        public boolean isUseTabs() {
            return useTabs;
        }
    }
}
