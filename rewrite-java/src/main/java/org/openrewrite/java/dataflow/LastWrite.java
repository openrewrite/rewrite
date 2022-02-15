package org.openrewrite.java.dataflow;

import lombok.Value;
import org.openrewrite.marker.Marker;

import java.util.UUID;

@Value
public class LastWrite implements DataflowMarker {
    UUID id;
    UUID declaration;
}
