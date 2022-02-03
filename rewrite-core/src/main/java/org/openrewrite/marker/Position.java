package org.openrewrite.marker;

import lombok.Value;
import lombok.With;

import java.util.UUID;

@Value
@With
public class Position implements Marker {
    UUID id;
    int startPosition;
    int length;
}
