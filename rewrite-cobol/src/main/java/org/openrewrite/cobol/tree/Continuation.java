package org.openrewrite.cobol.tree;

import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;

import java.util.UUID;

@With
@Value
public class Continuation implements Marker {

    UUID id;
    CobolContainer<Integer> continuations;
}
