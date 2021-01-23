package org.openrewrite.marker;

public interface Markable {

    Markers getMarkers();

    <M extends Markable> M withMarkers(Markers markers);

    default <M extends Markable> M mark(Marker... add) {
        Markers markers = getMarkers();
        for (Marker marker : add) {
            markers = markers.add(marker);
        }
        return withMarkers(markers);
    }
}
