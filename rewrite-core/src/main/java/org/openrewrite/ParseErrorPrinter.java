/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite;

import org.openrewrite.marker.Marker;
import org.openrewrite.tree.ParseError;

public class ParseErrorPrinter<P> extends ParseErrorVisitor<PrintOutputCapture<P>> {
    @Override
    public ParseError visitParseError(ParseError e, PrintOutputCapture<P> p) {
        for (Marker marker : e.getMarkers().getMarkers()) {
            p.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), it -> it));
        }
        visitMarkers(e.getMarkers(), p);
        for (Marker marker : e.getMarkers().getMarkers()) {
            p.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), it -> it));
        }
        p.append(e.getText());
        for (Marker marker : e.getMarkers().getMarkers()) {
            p.append(p.getMarkerPrinter().afterSyntax(marker, new Cursor(getCursor(), marker), it -> it));
        }
        return e;
    }
}
