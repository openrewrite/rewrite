package org.openrewrite.marker;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Provenance {
    private final GitProvenance gitProvenance;
    private final OsProvenance osProvenance;
}
