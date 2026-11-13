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
package org.openrewrite.coldfusion.tree;

import lombok.Value;
import lombok.With;
import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.marker.Markers;

/**
 * A CFML comment, {@code <!--- ... --->}, inside a tag, between its attributes or in its expression.
 * The text is everything between the delimiters, nested comments included.
 */
@Value
public class TagComment implements Comment {

    @With
    String text;

    String suffix;

    @SuppressWarnings("unchecked")
    @Override
    public TagComment withSuffix(String suffix) {
        return suffix.equals(this.suffix) ? this : new TagComment(text, suffix, markers);
    }

    @With
    Markers markers;

    @Override
    public boolean isMultiline() {
        return true;
    }

    @Override
    public <P> void printComment(Cursor cursor, PrintOutputCapture<P> p) {
        p.append("<!---").append(text).append("--->");
    }
}
