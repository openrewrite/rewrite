package org.openrewrite.marker;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;

import java.util.UUID;

@Value
@With
public class LstProvenance implements Marker {
    @EqualsAndHashCode.Exclude
    UUID id;

    Type buildToolType;
    String buildToolVersion;
    String lstSerializerVersion;

    public enum Type {
        Gradle,
        Maven,
        Bazel,
        Cli
    }
}
