package org.openrewrite.ruby.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;

import java.util.UUID;

/**
 * The "unless" keyword inverts an if condition.
 */
@Value
@With
public class Unless implements Marker {
    UUID id;
}
