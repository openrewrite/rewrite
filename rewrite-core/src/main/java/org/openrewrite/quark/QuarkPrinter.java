package org.openrewrite.quark;

import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.util.function.UnaryOperator;

public class QuarkPrinter<P> extends QuarkVisitor<PrintOutputCapture<P>> {

    @Override
    public Quark visitQuark(Quark quark, PrintOutputCapture<P> p) {
        beforeSyntax(quark.getMarkers(), p);
        if (!p.out.toString().isEmpty()) {
            p.append("⚛⚛⚛ The contents of this file are not visible. ⚛⚛⚛");
        }
        afterSyntax(quark.getMarkers(), p);
        return quark;
    }

    private static final UnaryOperator<String> QUARK_MARKER_WRAPPER =
            out -> "~~" + out + (out.isEmpty() ? "" : "~~") + ">";

    private void beforeSyntax(Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.out.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), QUARK_MARKER_WRAPPER));
        }
        visitMarkers(markers, p);
        for (Marker marker : markers.getMarkers()) {
            p.out.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), QUARK_MARKER_WRAPPER));
        }
    }

    private void afterSyntax(Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.out.append(p.getMarkerPrinter().afterSyntax(marker, new Cursor(getCursor(), marker), QUARK_MARKER_WRAPPER));
        }
    }
}
