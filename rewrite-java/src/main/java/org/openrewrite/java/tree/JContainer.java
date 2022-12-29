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
public class JContainer<T> {
    private transient Padding<T> padding;

    private static final JContainer<?> EMPTY = new JContainer<>(Space.EMPTY, emptyList(), Markers.EMPTY);

    private final Space before;
    private final List<JRightPadded<T>> elements;
    private final Markers markers;

    private JContainer(Space before, List<JRightPadded<T>> elements, Markers markers) {
        this.before = before;
        this.elements = elements;
        this.markers = markers;
    }

    public static <T> JContainer<T> build(List<JRightPadded<T>> elements) {
        return build(Space.EMPTY, elements, Markers.EMPTY);
    }

    @JsonCreator
    public static <T> JContainer<T> build(Space before, List<JRightPadded<T>> elements, Markers markers) {
        if (before.isEmpty() && elements.isEmpty() && markers.getMarkers().isEmpty()) {
            return empty();
        }
        return new JContainer<>(before, elements, markers);
    }

    @SuppressWarnings("unchecked")
    public static <T> JContainer<T> empty() {
        return (JContainer<T>) EMPTY;
    }

    public JContainer<T> withBefore(Space before) {
        return getBefore() == before ? this : build(before, elements, markers);
    }

    public JContainer<T> withMarkers(Markers markers) {
        return getMarkers() == markers ? this : build(before, elements, markers);
    }

    public Markers getMarkers() {
        return markers;
    }

    public List<T> getElements() {
        return JRightPadded.getElements(elements);
    }

    public Space getBefore() {
        return before;
    }

    public JContainer<T> map(UnaryOperator<T> map) {
        return getPadding().withElements(ListUtils.map(elements, t -> t.map(map)));
    }

    public Space getLastSpace() {
        return elements.isEmpty() ? Space.EMPTY : elements.get(elements.size() - 1).getAfter();
    }

    public enum Location {
        ANNOTATION_ARGUMENTS(Space.Location.ANNOTATION_ARGUMENTS, JRightPadded.Location.ANNOTATION_ARGUMENT),
        CASE(Space.Location.CASE, JRightPadded.Location.CASE),
        CASE_EXPRESSION(Space.Location.CASE_EXPRESSION, JRightPadded.Location.CASE_EXPRESSION),
        IMPLEMENTS(Space.Location.IMPLEMENTS, JRightPadded.Location.IMPLEMENTS),
        PERMITS(Space.Location.PERMITS, JRightPadded.Location.PERMITS),
        LANGUAGE_EXTENSION(Space.Location.LANGUAGE_EXTENSION, JRightPadded.Location.LANGUAGE_EXTENSION),
        METHOD_DECLARATION_PARAMETERS(Space.Location.METHOD_DECLARATION_PARAMETERS, JRightPadded.Location.METHOD_DECLARATION_PARAMETER),
        METHOD_INVOCATION_ARGUMENTS(Space.Location.METHOD_INVOCATION_ARGUMENTS, JRightPadded.Location.METHOD_INVOCATION_ARGUMENT),
        NEW_ARRAY_INITIALIZER(Space.Location.NEW_ARRAY_INITIALIZER, JRightPadded.Location.NEW_ARRAY_INITIALIZER),
        NEW_CLASS_ARGUMENTS(Space.Location.NEW_CLASS_ARGUMENTS, JRightPadded.Location.NEW_CLASS_ARGUMENTS),
        RECORD_STATE_VECTOR(Space.Location.RECORD_STATE_VECTOR, JRightPadded.Location.RECORD_STATE_VECTOR),
        THROWS(Space.Location.THROWS, JRightPadded.Location.THROWS),
        TRY_RESOURCES(Space.Location.TRY_RESOURCES, JRightPadded.Location.TRY_RESOURCE),
        TYPE_BOUNDS(Space.Location.TYPE_BOUNDS, JRightPadded.Location.TYPE_BOUND),
        TYPE_PARAMETERS(Space.Location.TYPE_PARAMETERS, JRightPadded.Location.TYPE_PARAMETER);

        private final Space.Location beforeLocation;
        private final JRightPadded.Location elementLocation;

        Location(Space.Location beforeLocation, JRightPadded.Location elementLocation) {
            this.beforeLocation = beforeLocation;
            this.elementLocation = elementLocation;
        }

        public Space.Location getBeforeLocation() {
            return beforeLocation;
        }

        public JRightPadded.Location getElementLocation() {
            return elementLocation;
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

        public List<JRightPadded<T>> getElements() {
            return c.elements;
        }

        public JContainer<T> withElements(List<JRightPadded<T>> elements) {
            return c.elements == elements ? c : build(c.before, elements, c.markers);
        }
    }

    @Nullable
    public static <J2 extends J> JContainer<J2> withElementsNullable(@Nullable JContainer<J2> before, @Nullable List<J2> elements) {
        if (before == null) {
            if (elements == null || elements.isEmpty()) {
                return null;
            }
            return JContainer.build(Space.EMPTY, JRightPadded.withElements(emptyList(), elements), Markers.EMPTY);
        }
        if (elements == null || elements.isEmpty()) {
            return null;
        }
        return before.getPadding().withElements(JRightPadded.withElements(before.elements, elements));
    }

    public static <J2 extends J> JContainer<J2> withElements(JContainer<J2> before, @Nullable List<J2> elements) {
        if (elements == null) {
            return before.getPadding().withElements(emptyList());
        }
        return before.getPadding().withElements(JRightPadded.withElements(before.elements, elements));
    }

    @Override
    public String toString() {
        return "JContainer(before=" + before + ", elementCount=" + elements.size() + ')';
    }
}
