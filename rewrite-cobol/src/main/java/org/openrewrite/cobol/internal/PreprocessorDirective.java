package org.openrewrite.cobol.internal;

import lombok.Value;
import lombok.With;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.marker.Marker;

import java.util.UUID;

@Value
@With
public class PreprocessorDirective implements Marker {
    UUID id;

    public <P> void printDirective(PrintOutputCapture<P> p) {
    }
}
