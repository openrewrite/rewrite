package org.openrewrite.hcl.tree;

public abstract class CoordinateBuilder {
    Hcl tree;

    CoordinateBuilder(Hcl tree) {
        this.tree = tree;
    }

    HclCoordinates before(Space.Location location) {
        return new HclCoordinates(tree, location, HclCoordinates.Mode.BEFORE, null);
    }

    HclCoordinates after(Space.Location location) {
        return new HclCoordinates(tree, location, HclCoordinates.Mode.AFTER, null);
    }

    HclCoordinates replace(Space.Location location) {
        return new HclCoordinates(tree, location, HclCoordinates.Mode.REPLACEMENT, null);
    }

    public static class Body extends CoordinateBuilder {
        Body(Hcl.Body tree) {
            super(tree);
        }

        public HclCoordinates last() {
            return before(Space.Location.BLOCK_CLOSE);
        }
    }
}
