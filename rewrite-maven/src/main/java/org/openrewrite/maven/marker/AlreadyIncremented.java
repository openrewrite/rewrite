package org.openrewrite.maven.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;

import java.util.UUID;

@Value
@With
public class AlreadyIncremented implements Marker {
    UUID id;
}
