package org.openrewrite.yaml;

import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;

import java.util.UUID;

/**
 * Multiline scalars are added directly to the tree, which leads to a wrong ident level.
 */
@Value
@With
public class MultilineScalarAdded implements Marker {
    UUID id;
}
