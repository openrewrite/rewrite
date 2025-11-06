/*
 * Copyright 2022 the original author or authors.
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
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.Markup;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.PlainText;

import java.nio.file.Paths;
import java.util.function.UnaryOperator;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.SourceSpecs.text;

class TreeTest implements RewriteTest {

    @Test
    void customMarkerPrinting() {
        rewriteRun(
          text("hello",
            spec -> spec.afterRecipe(pt -> {
                String printed = Markup.info(pt, "jon").printAll(new PrintOutputCapture<>(0,
                  new PrintOutputCapture.MarkerPrinter() {
                      @Override
                      public String afterSyntax(Marker marker, Cursor cursor, UnaryOperator<String> commentWrapper) {
                          if (marker instanceof Markup.Info markup) {
                              return " " + requireNonNull(markup.getMessage());
                          }
                          return "";
                      }
                  })
                );
                assertThat(printed).isEqualTo("hello jon");
            })
          )
        );
    }

    @Test
    void printRestoresBomWhenPresent() {
        PlainText sourceFile = new PlainText(
            Tree.randomId(),
            Paths.get("test.txt"),
            Markers.EMPTY,
            "UTF-8",
            true, // charsetBomMarked = true
            null,
            null,
            "Hello World",
            null
        );

        String printed = sourceFile.printAll();

        assertThat(printed).startsWith("\uFEFF");
        assertThat(printed).isEqualTo("\uFEFFHello World");
    }

    @Test
    void printDoesNotAddBomWhenNotPresent() {
        PlainText sourceFile = new PlainText(
            Tree.randomId(),
            Paths.get("test.txt"),
            Markers.EMPTY,
            "UTF-8",
            false, // charsetBomMarked = false
            null,
            null,
            "Hello World",
            null
        );

        String printed = sourceFile.printAll();

        assertThat(printed).doesNotStartWith("\uFEFF");
        assertThat(printed).isEqualTo("Hello World");
    }
}
