package org.openrewrite.groovy.marker;

import lombok.Value;
import org.openrewrite.marker.Marker;

import java.util.UUID;

@Value
public class InStyleForEachLoop implements Marker {
    UUID id;
}
