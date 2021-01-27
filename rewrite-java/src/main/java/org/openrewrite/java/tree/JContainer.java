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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.Markable;
import org.openrewrite.marker.Markers;

import java.util.List;
import java.util.function.Function;

import static java.util.Collections.emptyList;

/**
 * AST elements that contain lists of trees with some delimiter like parentheses, e.g. method arguments,
 * annotation arguments, catch variable declarations.
 *
 * Sometimes the delimiter surrounds the list. Parentheses surround method arguments. Sometimes the delimiter only
 * precedes the list. Throws statements on method declarations are preceded by the "throws" keyword.
 *
 * Sometimes containers are optional in the grammar, as in the
 * case of annotation arguments. Sometimes they are required, as in the case of method invocation arguments.
 *
 * @param <T> The type of the inner list of elements.
 */
public class JContainer<T> implements Markable {
    private static final JContainer<?> EMPTY = new JContainer<>(Space.EMPTY, emptyList(), Markers.EMPTY);

    private final Space before;
    private final List<JRightPadded<T>> elem;
    private final Markers markers;

    private JContainer(Space before, List<JRightPadded<T>> elem, Markers markers) {
        this.before = before;
        this.elem = elem;
        this.markers = markers;
    }

    @JsonCreator
    public static <T> JContainer<T> build(
            @JsonProperty("before") Space before,
            @JsonProperty("elem") List<JRightPadded<T>> elem,
            @JsonProperty("markers") Markers markers) {
        if (before.isEmpty() && elem.isEmpty()) {
            return empty();
        }
        return new JContainer<>(before, elem, markers);
    }

    @SuppressWarnings("unchecked")
    public static <T> JContainer<T> empty() {
        return (JContainer<T>) EMPTY;
    }

    public JContainer<T> withBefore(Space before) {
        return build(before, elem, markers);
    }

    public JContainer<T> withElem(List<JRightPadded<T>> elem) {
        return build(getBefore(), elem, markers);
    }

    @SuppressWarnings("unchecked")
    public JContainer<T> withMarkers(Markers markers) {
        return build(getBefore(), elem, markers);
    }

    public List<JRightPadded<T>> getElem() {
        return elem;
    }

    public Space getBefore() {
        return before;
    }

    @Override
    public Markers getMarkers() {
        return markers;
    }

    public JContainer<T> map(Function<T, T> map) {
        return withElem(ListUtils.map(elem, t -> t.map(map)));
    }

    @JsonIgnore
    public Space getLastSpace() {
        return elem.isEmpty() ? Space.EMPTY : elem.get(elem.size() - 1).getAfter();
    }

    public enum Location {
        ANNOTATION_ARGUMENTS(Space.Location.ANNOTATION_ARGUMENTS, JRightPadded.Location.ANNOTATION_ARGUMENT),
        CASE(Space.Location.CASE, JRightPadded.Location.CASE),
        IMPLEMENTS(Space.Location.IMPLEMENTS, JRightPadded.Location.IMPLEMENTS),
        METHOD_DECL_ARGUMENTS(Space.Location.METHOD_DECL_ARGUMENTS, JRightPadded.Location.METHOD_DECL_ARGUMENT),
        METHOD_INVOCATION_ARGUMENTS(Space.Location.METHOD_INVOCATION_ARGUMENTS, JRightPadded.Location.METHOD_INVOCATION_ARGUMENT),
        NEW_ARRAY_INITIALIZER(Space.Location.NEW_ARRAY_INITIALIZER, JRightPadded.Location.NEW_ARRAY_INITIALIZER),
        NEW_CLASS_ARGS(Space.Location.NEW_CLASS_ARGS, JRightPadded.Location.NEW_CLASS_ARGS),
        THROWS(Space.Location.THROWS, JRightPadded.Location.THROWS),
        TRY_RESOURCES(Space.Location.TRY_RESOURCES, JRightPadded.Location.TRY_RESOURCES),
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
}
