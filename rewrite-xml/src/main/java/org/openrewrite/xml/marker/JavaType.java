package org.openrewrite.xml.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;

import java.util.UUID;

@Value
@With
public class JavaType implements Marker {
    UUID id;
}
