package org.openrewrite.marker;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;

import java.util.UUID;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@With
public class OperatingSystem implements Marker {
    @EqualsAndHashCode.Include
    UUID id;

    Type type;

    public enum Type {
        Unix,
        Windows
    }
}
