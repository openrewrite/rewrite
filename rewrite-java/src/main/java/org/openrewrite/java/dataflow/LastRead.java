package org.openrewrite.java.dataflow;

import lombok.Value;

import java.util.UUID;

@Value
public class LastRead implements DataflowMarker {
    UUID id;
    UUID declaration;
}
