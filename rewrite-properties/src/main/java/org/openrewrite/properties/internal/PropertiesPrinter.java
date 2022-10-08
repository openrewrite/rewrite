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
package org.openrewrite.properties.internal;

import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.marker.Marker;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.tree.Properties;

import java.util.function.UnaryOperator;

public class PropertiesPrinter<P> extends PropertiesVisitor<PrintOutputCapture<P>> {

    @Override
    public Properties visitFile(Properties.File file, PrintOutputCapture<P> p) {
        beforeSyntax(file, p);
        visit(file.getContent(), p);
        p.out.append(file.getEof());
        afterSyntax(file, p);
        return file;
    }

    @Override
    public Properties visitEntry(Properties.Entry entry, PrintOutputCapture<P> p) {
        beforeSyntax(entry, p);
        p.out.append(entry.getKey())
                .append(entry.getBeforeEquals())
                .append('=');
        visit(entry.getValue(), p);
        afterSyntax(entry, p);
        return entry;
    }

    @Override
    public Properties visitValue(Properties.Value value, PrintOutputCapture<P> p) {
        beforeSyntax(value, p);
        p.out.append(value.getText());
        afterSyntax(value, p);
        return value;
    }

    @Override
    public Properties visitComment(Properties.Comment comment, PrintOutputCapture<P> p) {
        beforeSyntax(comment, p);
        p.out.append('#').append(comment.getMessage());
        afterSyntax(comment, p);
        return comment;
    }

    private static final UnaryOperator<String> PROPERTIES_MARKER_WRAPPER =
            out -> "~~" + out + (out.isEmpty() ? "" : "~~") + ">";

    private void beforeSyntax(Properties props, PrintOutputCapture<P> p) {
        for (Marker marker : props.getMarkers().getMarkers()) {
            p.out.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), PROPERTIES_MARKER_WRAPPER));
        }
        p.out.append(props.getPrefix());
        visitMarkers(props.getMarkers(), p);
        for (Marker marker : props.getMarkers().getMarkers()) {
            p.out.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), PROPERTIES_MARKER_WRAPPER));
        }
    }

    private void afterSyntax(Properties props, PrintOutputCapture<P> p) {
        for (Marker marker : props.getMarkers().getMarkers()) {
            p.out.append(p.getMarkerPrinter().afterSyntax(marker, new Cursor(getCursor(), marker), PROPERTIES_MARKER_WRAPPER));
        }
    }
}
