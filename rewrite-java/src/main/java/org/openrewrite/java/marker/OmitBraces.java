package org.openrewrite.java.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;

import java.util.UUID;

@Value
@With
public class OmitBraces implements Marker {
    UUID id;

    public OmitBraces(UUID id) {
        this.id = id;
    }
}
