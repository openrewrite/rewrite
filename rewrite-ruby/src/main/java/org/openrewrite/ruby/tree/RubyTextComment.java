/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.ruby.tree;

import lombok.Value;
import lombok.With;
import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.util.function.UnaryOperator;

/**
 * A Ruby comment. The text is everything the delimiters enclose — what follows the {@code #} for a
 * line comment, and everything between {@code =begin} and {@code =end}, newlines included, for a
 * block one — so that {@code withText(getText())} is lossless.
 */
@Value
public class RubyTextComment implements Comment {

    @With
    boolean multiline;

    @With
    String text;

    String suffix;

    @SuppressWarnings("unchecked")
    public RubyTextComment withSuffix(String suffix) {
        if (!suffix.equals(this.suffix)) {
            return new RubyTextComment(multiline, text, suffix, markers);
        }
        return this;
    }

    @With
    Markers markers;

    private static final UnaryOperator<String> RUBY_MARKER_WRAPPER =
            out -> "~~" + out + (out.isEmpty() ? "" : "~~") + ">";

    @Override
    public <P> void printComment(Cursor cursor, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(cursor, this), RUBY_MARKER_WRAPPER));
        }
        p.append(multiline ? "=begin" + text + "=end" : "#" + text);
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().afterSyntax(marker, new Cursor(cursor, this), RUBY_MARKER_WRAPPER));
        }
    }
}
