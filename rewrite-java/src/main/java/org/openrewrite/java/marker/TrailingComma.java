package org.openrewrite.java.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Marker;

import java.util.UUID;

@Value
@With
public class TrailingComma implements Marker {
    UUID id;
    Space suffix;
}
