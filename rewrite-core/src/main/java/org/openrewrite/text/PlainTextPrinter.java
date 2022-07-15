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

import org.openrewrite.PrintOutputCapture;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.SearchResult;

public class PlainTextPrinter<P> extends PlainTextVisitor<PrintOutputCapture<P>> {
    @Override
    public PlainText visitText(PlainText text, PrintOutputCapture<P> p) {
        visitMarkers(text.getMarkers(), p);
        p.out.append(text.getText());
        return text;
    }

    @Override
    public <M extends Marker> M visitMarker(Marker marker, PrintOutputCapture<P> p) {
        if (marker instanceof SearchResult) {
            String description = ((SearchResult) marker).getDescription();
            p.out.append("~~")
                    .append(description == null ? "" : "(" + description + ")~~")
                    .append(">");
        }
        //noinspection unchecked
        return (M) marker;
    }
}
