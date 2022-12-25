/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.marker.Markers;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;

class SpaceTest {

    @Test
    void formatLastSuffixWithSameSuffixDoesntChangeReference() {
        var suffix = Space.format(" ");
        var trees = List.of(new JRightPadded<>(new J.Empty(randomId(), suffix, Markers.EMPTY), suffix, Markers.EMPTY));
        assertThat(Space.formatLastSuffix(trees, suffix)).isSameAs(trees);
    }

    @Test
    void formatFirstPrefixWithSamePrefixDoesntChangeReference() {
        var prefix = Space.format(" ");
        var trees = List.of(new J.Empty(randomId(), prefix, Markers.EMPTY));
        assertThat(Space.formatFirstPrefix(trees, prefix)).isSameAs(trees);
    }

    @Test
    void spaceWithSameCommentsDoesntChangeReference() {
        var comments = List.<Comment>of(new TextComment(true, "test", "", Markers.EMPTY));
        var s = Space.build("", comments);
        assertThat(s.withComments(comments)).isSameAs(s);
    }

    @Test
    void singleLineComment() {
        @SuppressWarnings("TextBlockMigration") var cf = Space.format("  \n" +
                                                                      "// I'm a little // teapot\n" +
                                                                      "// Short and stout //\n    " +
                                                                      "// Here is my handle\n  ");

        assertThat(cf.getComments()).hasSize(3);

        TextComment c1 = (TextComment) cf.getComments().get(0);
        TextComment c2 = (TextComment) cf.getComments().get(1);
        TextComment c3 = (TextComment) cf.getComments().get(2);

        assertThat(c1.getText()).isEqualTo(" I'm a little // teapot");
        assertThat(c2.getText()).isEqualTo(" Short and stout //");
        assertThat(c3.getText()).isEqualTo(" Here is my handle");

        assertThat(c1.getSuffix()).isEqualTo("\n");
        assertThat(c2.getSuffix()).isEqualTo("\n    ");
        assertThat(c3.getSuffix()).isEqualTo("\n  ");

        assertThat(cf.getWhitespace()).isEqualTo("  \n");
    }

    @Test
    void multiLineComment() {
        @SuppressWarnings("TextBlockMigration") var cf = Space.format("  \n" +
                                                                      "/*   /*    Here is my spout     */\n" +
                                                                      "/* When I get all steamed up */\n" +
                                                                      "/* /*\n" +
                                                                      "Here me shout\n" +
                                                                      "*/\n  ");

        assertThat(cf.getComments()).hasSize(3);

        TextComment c1 = (TextComment) cf.getComments().get(0);
        TextComment c2 = (TextComment) cf.getComments().get(1);
        TextComment c3 = (TextComment) cf.getComments().get(2);

        assertThat(c1.getText()).isEqualTo("   /*    Here is my spout     ");
        assertThat(c2.getText()).isEqualTo(" When I get all steamed up ");
        assertThat(c3.getText()).isEqualTo(" /*\nHere me shout\n");

        assertThat(c1.getSuffix()).isEqualTo("\n");
        assertThat(c2.getSuffix()).isEqualTo("\n");
        assertThat(c3.getSuffix()).isEqualTo("\n  ");

        assertThat(cf.getWhitespace()).isEqualTo("  \n");
    }

    @Test
    void javadocComment() {
        @SuppressWarnings("TextBlockMigration") var cf = Space.format(
                "  \n" +
                "/**\n" +
                " * /** Tip me over and pour me out!\n" +
                " * https://somewhere/over/the/rainbow.txt\n" +
                " */\n  "
        );

        assertThat(cf.getComments()).hasSize(1);
        assertThat(cf.getComments().get(0).getSuffix()).isEqualTo("\n  ");
        assertThat(cf.getWhitespace()).isEqualTo("  \n");
    }

    @Test
    void multilineCommentWithDoubleSlashCommentOnFirstLine() {
        var cf = Space.format("""
                /*// debugging
                * bla
                */
                """
        );
        assertThat(cf.getComments()).hasSize(1);

        TextComment c1 = (TextComment) cf.getComments().get(0);
        assertThat(c1.getText()).isEqualTo("// debugging\n* bla\n");
    }

    @Test
    void stringify() {
        assertThat(Space.format("\n  \n\t \t").toString())
                .isEqualTo("Space(comments=<0 comments>, whitespace='\\n·₁·₂\\n-₁·₂-₃')");
    }

    @Test
    void findIndent() {
        assertThat(Space.build(" ", List.of(new TextComment(false, "hi", "\n   ", Markers.EMPTY))).getIndent())
                .isEqualTo("   ");
        assertThat(Space.build("   ", emptyList()).getIndent())
                .isEqualTo("   ");
        assertThat(Space.build("  \n ", emptyList()).getIndent())
                .isEqualTo(" ");
        assertThat(Space.build("  \n   \n    ", emptyList()).getIndent())
                .isEqualTo("    ");
    }
}
