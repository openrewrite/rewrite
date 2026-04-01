/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.csharp.tree;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.marker.Markers;

/**
 * A raw XML documentation comment received from the C# parser via RPC.
 * Contains the raw text of the doc comment block (with continuation {@code ///} prefixes).
 * <p>
 * This is a transient representation: {@link org.openrewrite.csharp.CSharpVisitor#visitSpace}
 * converts these into structured {@link CsDocComment.DocComment} trees for visitor/recipe support.
 */
@Value
@EqualsAndHashCode(callSuper = false)
@With
public class CsDocCommentRawComment implements Comment {
    String text;
    String suffix;
    Markers markers;

    public CsDocCommentRawComment(String text, String suffix) {
        this.text = text;
        this.suffix = suffix;
        this.markers = Markers.EMPTY;
    }

    public CsDocCommentRawComment(String text, String suffix, Markers markers) {
        this.text = text;
        this.suffix = suffix;
        this.markers = markers;
    }

    @Override
    public boolean isMultiline() {
        return true;
    }

    @Override
    public <P> void printComment(Cursor cursor, PrintOutputCapture<P> p) {
        p.append("//");
        p.append(text);
    }
}
