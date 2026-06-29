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
package org.openrewrite.csharp;

import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.csharp.tree.CsDocComment;
import org.openrewrite.csharp.tree.CsDocCommentRawComment;
import org.openrewrite.marker.Markers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The structured {@link CsDocComment.DocComment} produced by {@link CsDocCommentParser} must print
 * back to the exact same text the original {@link CsDocCommentRawComment} would have produced.
 * Otherwise any recipe that visits a file containing doc comments produces a non-applicable patch.
 */
class CsDocCommentLosslessTest {

    private static String rawPrint(String rawText) {
        // What the original (unparsed) comment prints: "//" + text
        return "//" + rawText;
    }

    private static String structuredPrint(String rawText) {
        CsDocCommentRawComment raw = new CsDocCommentRawComment(rawText, "\n", Markers.EMPTY);
        CsDocComment.DocComment parsed = CsDocCommentParser.parse(raw);
        PrintOutputCapture<Integer> out = new PrintOutputCapture<>(0);
        new CsDocCommentPrinter<Integer>().visit(parsed, out, new Cursor(null, Cursor.ROOT_VALUE));
        return out.getOut();
    }

    private void assertLossless(String rawText) {
        assertThat(structuredPrint(rawText)).isEqualTo(rawPrint(rawText));
    }

    @Test
    void singleLineSummary() {
        assertLossless("/ <summary>doc</summary>");
    }

    @Test
    void multilineSummary() {
        assertLossless("/ <summary>\n" +
                       "/// 传统方式\n" +
                       "/// </summary>");
    }

    @Test
    void nestedElements() {
        assertLossless("/ <summary>\n" +
                       "/// Adds <paramref name=\"a\"/> to <c>b</c>.\n" +
                       "/// </summary>\n" +
                       "/// <param name=\"a\">first</param>");
    }
}
