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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

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


    public JContainer<T> addElement(T newElements) {
        return addElements(Collections.singletonList(newElements));
    }

    public JContainer<T> insertElement(int index, T newElements) {
        return insertElements(index, Collections.singletonList(newElements));
    }

    public JContainer<T> addElements(List<T> newElements) {

        return withElem(
                ListUtils.concatAll(elem,
                        newElements.stream()
                                .map(e -> new JRightPadded<>(e, Space.EMPTY, Markers.EMPTY))
                                .collect(Collectors.toList())
                )
        );
    }

    public JContainer<T> insertElements(int index, List<T> newElements) {

        return withElem(
                ListUtils.insertAll(elem, index,
                        newElements.stream()
                                .map(e -> new JRightPadded<>(e, Space.EMPTY, Markers.EMPTY))
                                .collect(Collectors.toList())
                )
        );
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
        ANNOTATION_ARGUMENT(JRightPadded.Location.ANNOTATION_ARGUMENT),
        CASE(JRightPadded.Location.CASE),
        IMPLEMENTS(JRightPadded.Location.IMPLEMENTS),
        METHOD_DECL_ARGUMENT(JRightPadded.Location.METHOD_DECL_ARGUMENT),
        METHOD_INVOCATION_ARGUMENT(JRightPadded.Location.METHOD_INVOCATION_ARGUMENT),
        NEW_ARRAY_INITIALIZER(JRightPadded.Location.NEW_ARRAY_INITIALIZER),
        NEW_CLASS_ARGS(JRightPadded.Location.NEW_CLASS_ARGS),
        THROWS(JRightPadded.Location.THROWS),
        TRY_RESOURCES(JRightPadded.Location.TRY_RESOURCES),
        TYPE_BOUND(JRightPadded.Location.TYPE_BOUND),
        TYPE_PARAMETER(JRightPadded.Location.TYPE_PARAMETER);

        private final JRightPadded.Location elemLocation;

        Location(JRightPadded.Location elemLocation) {
            this.elemLocation = elemLocation;
        }

        public JRightPadded.Location getElemLocation() {
            return elemLocation;
        }
    }
}
