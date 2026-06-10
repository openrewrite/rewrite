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
package org.openrewrite.golang;

import org.junit.jupiter.api.Test;
import org.openrewrite.Tree;
import org.openrewrite.golang.tree.GoMod;
import org.openrewrite.golang.tree.GoMod.GoModStatement;
import org.openrewrite.golang.tree.GoModTree;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Java-side traversal of the {@code go.mod} LST via {@link GoModVisitor} — the
 * visitor reaches every node (directives, block entries, values) and can rewrite
 * them, mirroring how {@code GolangVisitor} works for {@code .go} sources.
 */
class GoModVisitorTest {

    @Test
    void visitsEveryNodeAndCanRewriteValues() {
        // given: module + go directives and a require block with one entry
        GoMod before = sampleGoMod();

        // when: a visitor counts values and bumps the go version
        AtomicInteger valuesSeen = new AtomicInteger();
        GoModVisitor<Integer> v = new GoModVisitor<Integer>() {
            @Override
            public GoModTree visitValue(GoMod.Value value, Integer p) {
                valuesSeen.incrementAndGet();
                GoMod.Value visited = (GoMod.Value) super.visitValue(value, p);
                return "1.21".equals(visited.getText()) ? visited.withText("1.22") : visited;
            }
        };
        GoMod after = (GoMod) v.visitGoMod(before, 0);

        // then: every value node was reached and the go version was rewritten
        assertThat(valuesSeen.get()).isEqualTo(4); // example.com/two, 1.21, github.com/a/b, v1.0.0
        assertThat(goVersion(after)).isEqualTo("1.22");
        assertThat(after.getStatements()).allSatisfy(rp -> assertThat(rp.getElement()).isNotNull());
    }

    private static String goVersion(GoMod gm) {
        for (JRightPadded<GoModStatement> rp : gm.getStatements()) {
            if (rp.getElement() instanceof GoMod.Directive) {
                GoMod.Directive d = (GoMod.Directive) rp.getElement();
                if ("go".equals(d.getKeyword())) {
                    return d.getValues().get(0).getText();
                }
            }
        }
        throw new IllegalStateException("no go directive");
    }

    private static GoMod sampleGoMod() {
        List<JRightPadded<GoModStatement>> stmts = new ArrayList<>();
        stmts.add(rightPad(directive("module", "example.com/two")));
        stmts.add(rightPad(directive("go", "1.21")));

        GoMod.Directive entry = new GoMod.Directive(Tree.randomId(), Space.format("\n\t"), Markers.EMPTY, "",
                List.of(value("github.com/a/b"), value("v1.0.0")));
        GoMod.Block block = new GoMod.Block(Tree.randomId(), Space.format("\n\n"), Markers.EMPTY, "require",
                Space.format(" "), List.of(rightPad(entry)), Space.format("\n"));
        stmts.add(rightPad(block));

        return new GoMod(Tree.randomId(), Space.EMPTY, Markers.EMPTY, Paths.get("go.mod"),
                "UTF-8", false, null, null, stmts, Space.EMPTY);
    }

    private static GoMod.Directive directive(String keyword, String value) {
        return new GoMod.Directive(Tree.randomId(), Space.EMPTY, Markers.EMPTY, keyword, List.of(value(value)));
    }

    private static GoMod.Value value(String text) {
        return new GoMod.Value(Tree.randomId(), Space.format(" "), Markers.EMPTY, text);
    }

    private static JRightPadded<GoModStatement> rightPad(GoModStatement s) {
        return new JRightPadded<>(s, Space.format("\n"), Markers.EMPTY);
    }
}
