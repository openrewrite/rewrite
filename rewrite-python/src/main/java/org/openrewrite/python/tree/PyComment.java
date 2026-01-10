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
package org.openrewrite.python.tree;

import lombok.Value;
import lombok.With;
import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.marker.Markers;

@Value
public class PyComment implements Comment {
    @With
    String text;

    @With
    String suffix;

    @With
    boolean alignedToIndent;

    @With
    Markers markers;

    @Override
    public boolean isMultiline() {
        // Python comments can *never* span multiple lines.
        return false;
    }

    @Override
    public <P> void printComment(Cursor cursor, PrintOutputCapture<P> p) {
        p.append("#").append(text);
    }
}
