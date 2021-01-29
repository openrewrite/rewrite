package org.openrewrite.java.tree;

import lombok.AccessLevel;
import lombok.Data;
import lombok.With;
import lombok.experimental.FieldDefaults;
import org.openrewrite.internal.lang.Nullable;

@Data
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class JavaTreeCoordinates {

    @With
    J tree;

    @Nullable
    @With
    Space.Location spaceLocation;
}
