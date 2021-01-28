package org.openrewrite.java.tree;

public class MethodCoordinates implements Coordinates {

    private final J.MethodDecl tree;

    public MethodCoordinates(J.MethodDecl tree) {
        this.tree = tree;
    }

    @Override
    public J.MethodDecl getTree() {
        return tree;
    }

    @Override
    public TreeCoordinates before() {
        return new TreeCoordinates(tree, Space.Location.METHOD_DECL_PREFIX);
    }

    public TreeCoordinates arguments() {
        return new TreeCoordinates(tree, Space.Location.METHOD_DECL_ARGUMENTS);
    }

    //TODO MOAR!
}
