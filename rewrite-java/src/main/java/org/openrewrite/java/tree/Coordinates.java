/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
