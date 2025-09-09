package org.openrewrite.java.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Marker;

import java.util.UUID;

@Value
@With
public class PackageOnCompactSourceFile implements Marker {
    UUID id;
    Space prefix;
    String packageDefinition;
}
