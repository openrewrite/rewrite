package org.openrewrite.xml.internal;

import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.marker.Marker;
import org.openrewrite.xml.XmlProcessor;
import org.openrewrite.xml.tree.Xml;

import java.util.Set;

public class XmlListMarkersProcessor<T> extends XmlProcessor<Set<T>> {

    private final Class<? extends Marker> markerType;

    public XmlListMarkersProcessor(Class<? extends Marker> markerType) {
        this.markerType = markerType;
    }

    @Override
    public Xml visitEach(Xml xml, @NonNull Set<T> ts) {
        if (xml.getMarkers().findFirst(markerType).isPresent()) {
            //noinspection unchecked
            ts.add((T) xml);
        }
        return super.visitEach(xml, ts);
    }
}
