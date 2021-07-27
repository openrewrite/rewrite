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
import org.openrewrite.TreePrinter;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

public class PlainTextPrinter<P> extends PlainTextVisitor<P> {
    private static final String PRINTER_ACC_KEY = "printed";

    private final TreePrinter<P> treePrinter;

    public PlainTextPrinter(TreePrinter<P> treePrinter) {
        this.treePrinter = treePrinter;
    }

    public String print(PlainText text, P p) {
        setCursor(new Cursor(null, "EPSILON"));
        visit(text, p);
        return getPrinter().toString();
    }

    @Override
    public PlainText visitText(PlainText text, P p) {
        visitMarkers(text.getMarkers(), p);
        getPrinter().append(text.getText());
        return text;
    }

    @NonNull
    protected StringBuilder getPrinter() {
        StringBuilder acc = getCursor().getRoot().getNearestMessage(PRINTER_ACC_KEY);
        if (acc == null) {
            acc = new StringBuilder();
            getCursor().getRoot().putMessage(PRINTER_ACC_KEY, acc);
        }
        return acc;
    }

    @Override
    public <M extends Marker> M visitMarker(Marker marker, P p) {
        StringBuilder acc = getPrinter();
        treePrinter.doBefore(marker, acc, p);
        acc.append(marker.print(treePrinter, p));
        treePrinter.doAfter(marker, acc, p);
        //noinspection unchecked
        return (M) marker;
    }

    @Override
    public Markers visitMarkers(Markers markers, P p) {
        StringBuilder acc = getPrinter();
        treePrinter.doBefore(markers, acc, p);
        Markers m = super.visitMarkers(markers, p);
        treePrinter.doAfter(markers, acc, p);
        return m;
    }
}
