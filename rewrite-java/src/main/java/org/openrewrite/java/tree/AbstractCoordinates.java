package org.openrewrite.java.tree;

import org.openrewrite.internal.lang.Nullable;

/**
 * Package protected base class that handles boilerplate for all concrete coordinate classes in the Coordinates
 * interface.
 */
abstract class AbstractCoordinates implements Coordinates {

    private J tree;

    protected AbstractCoordinates(J tree) {
        this.tree = tree;
    }

    protected JavaTreeCoordinates create(@Nullable Space.Location location) {
        return new JavaTreeCoordinates(tree, location);
    }

    public JavaTreeCoordinates around() {
        return create(null);
    }
}
