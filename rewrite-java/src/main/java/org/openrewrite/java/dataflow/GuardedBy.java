package org.openrewrite.java.dataflow;

import lombok.Value;

import java.util.UUID;

@Value
public class GuardedBy implements DataflowMarker {
    UUID id;
    UUID guard;
    boolean byNegation;
}
