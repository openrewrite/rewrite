package org.openrewrite.java.tree;

import org.openrewrite.internal.lang.Nullable;

public abstract class JavaCoordinatesMap<J2 extends J> {

    private J2 tree;

    protected JavaCoordinatesMap(J2 tree) {
        this.tree = tree;
    }

    protected JavaTreeCoordinates create(@Nullable Space.Location location) {
        return new JavaTreeCoordinates(tree, location);
    }
    public JavaTreeCoordinates around() {
        return create(null);
    }

    public abstract JavaTreeCoordinates before();
}
