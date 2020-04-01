package org.openrewrite.java.tree;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;

import java.util.List;

import static java.util.Collections.emptyList;

public interface Expression extends J {
    @Nullable
    JavaType getType();

    <T extends Tree> T withType(@Nullable JavaType type);

    /**
     * @return A list of the side effects emitted by the statement, if the statement was decomposed.
     * So for a binary operation, there are up to two potential side effects (the left and right side) and as
     * few as zero if both sides of the expression are something like constants or variable references.
     */
    @JsonIgnore
    default List<Tree> getSideEffects() {
        return emptyList();
    }
}
