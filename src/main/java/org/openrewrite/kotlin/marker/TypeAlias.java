package org.openrewrite.kotlin.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;

import java.util.UUID;

@Value
@With
public class TypeAlias implements Marker {
    UUID id;
}
