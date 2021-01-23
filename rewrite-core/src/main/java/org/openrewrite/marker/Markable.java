package org.openrewrite.marker;

public interface Markable {

    Markers getMarkers();

    <M extends Markable> M withMarkers(Markers markers);
}
