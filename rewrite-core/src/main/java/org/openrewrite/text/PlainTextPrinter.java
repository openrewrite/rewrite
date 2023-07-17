/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.text;

import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.util.function.UnaryOperator;

public class PlainTextPrinter<P> extends PlainTextVisitor<PrintOutputCapture<P>> {
    private static final UnaryOperator<String> TEXT_MARKER_WRAPPER =
            out -> "~~" + out + (out.isEmpty() ? "" : "~~") + ">";

    @Override
    public PlainText visitText(PlainText text, PrintOutputCapture<P> p) {
        visitMarkableText(text.getMarkers(), text.getText(), p);
        for (PlainText.Snippet snippet : text.getSnippets()) {
            setCursor(new Cursor(getCursor(), snippet));
            visitSnippet(snippet, p);
            setCursor(getCursor().getParent());
        }
        return text;
    }

    @Override
    public PlainText.Snippet visitSnippet(PlainText.Snippet text, PrintOutputCapture<P> p) {
        visitMarkableText(text.getMarkers(), text.getText(), p);
        return text;
    }

    private void visitMarkableText(Markers markers, String text, PrintOutputCapture<P> p) {
        //noinspection DuplicatedCode
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), TEXT_MARKER_WRAPPER));
        }
        visitMarkers(markers, p);
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), TEXT_MARKER_WRAPPER));
        }
        p.append(text);
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().afterSyntax(marker, new Cursor(getCursor(), marker), TEXT_MARKER_WRAPPER));
        }
    }
}
