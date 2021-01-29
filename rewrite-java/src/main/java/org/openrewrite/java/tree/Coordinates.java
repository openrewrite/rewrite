package org.openrewrite.java.tree;

import org.openrewrite.internal.lang.Nullable;

public interface Coordinates {

    JavaTreeCoordinates around();
    JavaTreeCoordinates before();

    class ClassCoordinates extends AbstractCoordinates {

        protected ClassCoordinates(J.ClassDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { return create(Space.Location.CLASS_DECL_PREFIX); }
        public JavaTreeCoordinates extending() {return create(Space.Location.EXTENDS); }

        //TODO MOAR!
    }

    class MethodCoordinates extends AbstractCoordinates {

        protected MethodCoordinates(J.MethodDecl tree) {super(tree); }

        @Override
        public JavaTreeCoordinates before() { return create(Space.Location.METHOD_DECL_PREFIX); }
        public JavaTreeCoordinates arguments() {return create(Space.Location.METHOD_DECL_ARGUMENTS); }

        //TODO MOAR!
    }
}
