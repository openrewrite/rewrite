package org.openrewrite.marker;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.With;
import org.openrewrite.internal.lang.Nullable;

import java.util.UUID;

@Getter
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Provenance implements Marker {
    @Nullable
    private final GitProvenance gitProvenance;
    private final OsProvenance osProvenance;

    @With
    @EqualsAndHashCode.Include
    private final UUID id;
}
