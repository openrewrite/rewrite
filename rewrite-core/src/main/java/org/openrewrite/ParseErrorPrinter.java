package org.openrewrite;

import org.openrewrite.marker.Marker;

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
