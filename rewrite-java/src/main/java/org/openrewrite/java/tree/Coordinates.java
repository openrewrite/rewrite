package org.openrewrite.java.tree;

public interface Coordinates {

    J getTree();

    TreeCoordinates before();

    default TreeCoordinates around() {
        return new TreeCoordinates(getTree(), null);
    }
}
