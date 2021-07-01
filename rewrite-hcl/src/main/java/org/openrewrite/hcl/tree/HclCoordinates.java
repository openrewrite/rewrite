package org.openrewrite.hcl.tree;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.template.Coordinates;

import java.util.Comparator;

@Data
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class HclCoordinates implements Coordinates {
    Hcl tree;
    Space.Location spaceLocation;
    Mode mode;

    @Nullable
    Comparator<? extends Hcl> comparator;

    public boolean isReplacement() {
        return Mode.REPLACEMENT.equals(mode);
    }

    /**
     * Determines whether we are replacing a whole tree element, and not either
     * (1) replacing just a piece of a method, class, or variable declaration signature or
     * (2) inserting a new element
     */
    public boolean isReplaceWholeCursorValue() {
        return isReplacement() && spaceLocation == null;
    }

    public enum Mode {
        AFTER,
        BEFORE,
        REPLACEMENT
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    public <H extends Hcl> Comparator<H> getComparator() {
        return (Comparator<H>) comparator;
    }
}
