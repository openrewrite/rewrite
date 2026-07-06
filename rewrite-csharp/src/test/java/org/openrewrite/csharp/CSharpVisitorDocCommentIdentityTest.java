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
import org.openrewrite.Issue;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.csharp.tree.CsDocComment;
import org.openrewrite.csharp.tree.CsDocCommentRawComment;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Issue("https://github.com/moderneinc/customer-requests/issues/2696")
class CSharpVisitorDocCommentIdentityTest {

    private static final String DOC_TEXT = "/ <summary>\n" +
                                           "/// Adds two numbers together. See <see cref=\"System.Math\"/>.\n" +
                                           "/// </summary>\n" +
                                           "/// <param name=\"left\">The left operand.</param>";

    private static Space spaceWithComment(Comment comment) {
        return Space.build("\n    ", List.of(comment));
    }

    @Test
    void noOpVisitReturnsIdenticalSpaceForRawDocComment() {
        Space space = spaceWithComment(new CsDocCommentRawComment(DOC_TEXT, "\n    ", Markers.EMPTY));

        Space visited = new CSharpIsoVisitor<Integer>().visitSpace(space, Space.Location.ANY, 0);

        assertThat(visited).isSameAs(space);
    }

    @Test
    void noOpVisitReturnsIdenticalSpaceForStructuredDocComment() {
        CsDocComment.DocComment parsed = CsDocCommentParser.parse(
                new CsDocCommentRawComment(DOC_TEXT, "\n    ", Markers.EMPTY));
        Space space = spaceWithComment(parsed);

        Space visited = new CSharpIsoVisitor<Integer>().visitSpace(space, Space.Location.ANY, 0);

        assertThat(visited).isSameAs(space);
    }

    @Test
    void modifyingVisitStillReplacesRawCommentWithStructuredTree() {
        Space space = spaceWithComment(new CsDocCommentRawComment(DOC_TEXT, "\n    ", Markers.EMPTY));
        CSharpVisitor<Integer> visitor = new CSharpVisitor<>() {
            @Override
            protected CsDocCommentVisitor<Integer> getCsDocCommentVisitor() {
                return new CsDocCommentVisitor<>(this) {
                    @Override
                    public CsDocComment visitXmlText(CsDocComment.XmlText text, Integer p) {
                        return text.withText(text.getText().replace("Adds", "Sums"));
                    }
                };
            }
        };

        Space visited = visitor.visitSpace(space, Space.Location.ANY, 0);

        assertThat(visited).isNotSameAs(space);
        Comment comment = visited.getComments().get(0);
        assertThat(comment).isInstanceOf(CsDocComment.DocComment.class);
        PrintOutputCapture<Integer> out = new PrintOutputCapture<>(0);
        new CsDocCommentPrinter<Integer>().visit((CsDocComment.DocComment) comment, out, new Cursor(null, Cursor.ROOT_VALUE));
        assertThat(out.getOut()).contains("Sums two numbers together");
    }
}
