/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.ruby.internal;

import org.junit.jupiter.api.Test;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.SourceFile;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.ruby.RubyVisitor;
import org.openrewrite.test.RewriteTest;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.ruby.Assertions.ruby;

class RubyPrinterTest implements RewriteTest {

    @Test
    void fencedMarkersCloseAroundEveryTree() {
        rewriteRun(
          ruby(
            """
              x = begin
                1
              end
              if x
                x
              end
              while x do
                x -= 1
              end
              """,
            spec -> spec.afterRecipe(cu -> assertThat(unclosedFences(cu)).isEmpty())
          )
        );
    }

    private static Set<String> unclosedFences(SourceFile cu) {
        Map<String, String> trees = new HashMap<>();
        SourceFile marked = (SourceFile) new RubyVisitor<Integer>() {
            @Override
            public J postVisit(J tree, Integer p) {
                SearchResult found = new SearchResult(randomId(), null);
                trees.put("{{" + found.getId() + "}}", tree.getClass().getSimpleName());
                return tree.withMarkers(tree.getMarkers().add(found));
            }
        }.visitNonNull(cu, 0);
        Map<String, Integer> fences = new HashMap<>();
        Matcher fence = Pattern.compile("\\{\\{[0-9a-f-]{36}}}")
          .matcher(marked.printAll(new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.FENCED)));
        while (fence.find()) {
            fences.merge(fence.group(), 1, Integer::sum);
        }
        return fences.entrySet().stream()
          .filter(e -> e.getValue() != 2)
          .map(e -> trees.get(e.getKey()))
          .collect(toSet());
    }
}
