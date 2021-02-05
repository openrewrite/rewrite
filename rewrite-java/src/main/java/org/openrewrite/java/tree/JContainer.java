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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.RequiredArgsConstructor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markable;
import org.openrewrite.marker.Markers;

import java.util.List;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptyList;

/**
 * AST elements that contain lists of trees with some delimiter like parentheses, e.g. method arguments,
 * annotation arguments, catch variable declarations.
 * <p>
 * Sometimes the delimiter surrounds the list. Parentheses surround method arguments. Sometimes the delimiter only
 * precedes the list. Throws statements on method declarations are preceded by the "throws" keyword.
 * <p>
 * Sometimes containers are optional in the grammar, as in the
 * case of annotation arguments. Sometimes they are required, as in the case of method invocation arguments.
 *
 * @param <T> The type of the inner list of elements.
 */
public class JContainer<T> implements Markable {
    private transient Padding<T> padding;

    private static final JContainer<?> EMPTY = new JContainer<>(Space.EMPTY, emptyList(), Markers.EMPTY);

    private final Space before;
    private final List<JRightPadded<T>> elems;
    private final Markers markers;

    private JContainer(Space before, List<JRightPadded<T>> elems, Markers markers) {
        this.before = before;
        this.elems = elems;
        this.markers = markers;
    }

    public static <T> JContainer<T> build(List<JRightPadded<T>> elems) {
        return build(Space.EMPTY, elems, Markers.EMPTY);
    }

    @JsonCreator
    public static <T> JContainer<T> build(Space before, List<JRightPadded<T>> elems, Markers markers) {
        if (before.isEmpty() && elems.isEmpty()) {
            return empty();
        }
        return new JContainer<>(before, elems, markers);
    }

    @SuppressWarnings("unchecked")
    public static <T> JContainer<T> empty() {
        return (JContainer<T>) EMPTY;
    }

    public JContainer<T> withBefore(Space before) {
        return build(before, elems, markers);
    }

    @SuppressWarnings("unchecked")
    public JContainer<T> withMarkers(Markers markers) {
        return build(getBefore(), elems, markers);
    }

    public List<T> getElems() {
        return JRightPadded.getElems(elems);
    }

    public Space getBefore() {
        return before;
    }

    @Override
    public Markers getMarkers() {
        return markers;
    }

    public JContainer<T> map(UnaryOperator<T> map) {
        return getPadding().withElems(ListUtils.map(elems, t -> t.map(map)));
    }

    public Space getLastSpace() {
        return elems.isEmpty() ? Space.EMPTY : elems.get(elems.size() - 1).getAfter();
    }

    public enum Location {
        ANNOTATION_ARGUMENTS(Space.Location.ANNOTATION_ARGUMENTS, JRightPadded.Location.ANNOTATION_ARGUMENT),
        CASE(Space.Location.CASE, JRightPadded.Location.CASE),
        IMPLEMENTS(Space.Location.IMPLEMENTS, JRightPadded.Location.IMPLEMENTS),
        METHOD_DECL_PARAMETERS(Space.Location.METHOD_DECL_PARAMETERS, JRightPadded.Location.METHOD_DECL_PARAMETER),
        METHOD_INVOCATION_ARGUMENTS(Space.Location.METHOD_INVOCATION_ARGUMENTS, JRightPadded.Location.METHOD_INVOCATION_ARGUMENT),
        NEW_ARRAY_INITIALIZER(Space.Location.NEW_ARRAY_INITIALIZER, JRightPadded.Location.NEW_ARRAY_INITIALIZER),
        NEW_CLASS_ARGS(Space.Location.NEW_CLASS_ARGS, JRightPadded.Location.NEW_CLASS_ARGS),
        THROWS(Space.Location.THROWS, JRightPadded.Location.THROWS),
        TRY_RESOURCES(Space.Location.TRY_RESOURCES, JRightPadded.Location.TRY_RESOURCE),
        TYPE_BOUNDS(Space.Location.TYPE_BOUNDS, JRightPadded.Location.TYPE_BOUND),
        TYPE_PARAMETERS(Space.Location.TYPE_PARAMETERS, JRightPadded.Location.TYPE_PARAMETER);

        private final Space.Location beforeLocation;
        private final JRightPadded.Location elemLocation;

        Location(Space.Location beforeLocation, JRightPadded.Location elemLocation) {
            this.beforeLocation = beforeLocation;
            this.elemLocation = elemLocation;
        }

        public Space.Location getBeforeLocation() {
            return beforeLocation;
        }

        public JRightPadded.Location getElemLocation() {
            return elemLocation;
        }
    }

    public Padding<T> getPadding() {
        if (padding == null) {
            this.padding = new Padding<>(this);
        }
        return padding;
    }

    @RequiredArgsConstructor
    public static class Padding<T> {
        private final JContainer<T> c;

        public List<JRightPadded<T>> getElems() {
            return c.elems;
        }

        public JContainer<T> withElems(List<JRightPadded<T>> elem) {
            return c.elems == elem ? c : build(c.before, elem, c.markers);
        }
    }

    @Nullable
    public static <J2 extends J> JContainer<J2> withElemsNullable(@Nullable JContainer<J2> before, @Nullable List<J2> elems) {
        if (before == null) {
            if (elems == null || elems.isEmpty()) {
                return null;
            }
            return JContainer.build(Space.EMPTY, JRightPadded.withElems(emptyList(), elems), Markers.EMPTY);
        }
        if (elems == null || elems.isEmpty()) {
            return null;
        }
        return before.getPadding().withElems(JRightPadded.withElems(before.elems, elems));
    }

    public static <J2 extends J> JContainer<J2> withElems(JContainer<J2> before, @Nullable List<J2> elems) {
        if (elems == null) {
            return before.getPadding().withElems(emptyList());
        }
        return before.getPadding().withElems(JRightPadded.withElems(before.elems, elems));
    }

    @Override
    public String toString() {
        return "JContainer(before=" + before + ", elemCount=" + elems.size() + ')';
    }
}
