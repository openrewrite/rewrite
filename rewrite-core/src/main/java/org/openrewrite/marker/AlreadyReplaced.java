package org.openrewrite.marker;

import lombok.Value;
import lombok.With;

import java.util.UUID;

/**
 * Ensure that the same replacement is not applied to the same file more than once per recipe run.
 * Used to avoid the situation where replacing "a" with "ab" results in something like "abb".
 */
@Value
@With
public class AlreadyReplaced implements Marker {
    UUID id;
    String find;
    String replace;
}
