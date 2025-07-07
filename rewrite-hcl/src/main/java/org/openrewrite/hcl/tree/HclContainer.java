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
package org.openrewrite.hcl.tree;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.Markers;

import java.util.List;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptyList;

/**
 * AST elements that contain lists of trees with some delimiter like function call arguments.
 *
 * @param <T> The type of the inner list of elements.
 */
public class HclContainer<T> {
    private transient Padding<T> padding;

    private static final HclContainer<?> EMPTY = new HclContainer<>(Space.EMPTY, emptyList(), Markers.EMPTY);

    private final Space before;
    private final List<HclRightPadded<T>> elements;
    private final Markers markers;

    private HclContainer(Space before, List<HclRightPadded<T>> elements, Markers markers) {
        this.before = before;
        this.elements = elements;
        this.markers = markers;
    }

    public static <T> HclContainer<T> build(List<HclRightPadded<T>> elements) {
        return build(Space.EMPTY, elements, Markers.EMPTY);
    }

    @JsonCreator
    public static <T> HclContainer<T> build(Space before, List<HclRightPadded<T>> elements, Markers markers) {
        if (before.isEmpty() && elements.isEmpty()) {
            return empty();
        }
        return new HclContainer<>(before, elements, markers);
    }

    @SuppressWarnings("unchecked")
    public static <T> HclContainer<T> empty() {
        return (HclContainer<T>) EMPTY;
    }

    public HclContainer<T> withBefore(Space before) {
        return getBefore() == before ? this : build(before, elements, markers);
    }

    public HclContainer<T> withMarkers(Markers markers) {
        return getMarkers() == markers ? this : build(before, elements, markers);
    }

    public Markers getMarkers() {
        return markers;
    }

    public List<T> getElements() {
        return HclRightPadded.getElements(elements);
    }

    public Space getBefore() {
        return before;
    }

    public HclContainer<T> map(UnaryOperator<T> map) {
        return getPadding().withElements(ListUtils.map(elements, t -> t.map(map)));
    }

    public Space getLastSpace() {
        return elements.isEmpty() ? Space.EMPTY : elements.get(elements.size() - 1).getAfter();
    }

    public HclContainer<T> withLastSpace(Space space) {
        final List<HclRightPadded<T>> newElements = ListUtils.map(elements, (i, elem) -> {
            if (i == elements.size() -1) {
                return elem.withAfter(space);
            }
            return elem;
        });
        return elements.isEmpty() || getLastSpace() == space ? this : build(before, newElements, markers);
    }

    public enum Location {
        FOR_VARIABLES(Space.Location.FOR_VARIABLES, HclRightPadded.Location.FOR_VARIABLE_ARGUMENT),
        FUNCTION_CALL_ARGUMENTS(Space.Location.FUNCTION_CALL_ARGUMENTS, HclRightPadded.Location.FUNCTION_CALL_ARGUMENT),
        OBJECT_VALUE_ATTRIBUTES(Space.Location.OBJECT_VALUE_ATTRIBUTES, HclRightPadded.Location.OBJECT_VALUE_ARGUMENT),
        TUPLE_VALUES(Space.Location.TUPLE_VALUES, HclRightPadded.Location.TUPLE_VALUE);

        private final Space.Location beforeLocation;
        private final HclRightPadded.Location elementLocation;

        Location(Space.Location beforeLocation, HclRightPadded.Location elementLocation) {
            this.beforeLocation = beforeLocation;
            this.elementLocation = elementLocation;
        }

        public Space.Location getBeforeLocation() {
            return beforeLocation;
        }

        public HclRightPadded.Location getElementLocation() {
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
        private final HclContainer<T> c;

        public List<HclRightPadded<T>> getElements() {
            return c.elements;
        }

        public HclContainer<T> withElements(List<HclRightPadded<T>> elements) {
            return c.elements == elements ? c : build(c.before, elements, c.markers);
        }
    }

    public static <H extends Hcl> @Nullable HclContainer<H> withElementsNullable(@Nullable HclContainer<H> before, @Nullable List<H> elements) {
        if (before == null) {
            if (elements == null || elements.isEmpty()) {
                return null;
            }
            return HclContainer.build(Space.EMPTY, HclRightPadded.withElements(emptyList(), elements), Markers.EMPTY);
        }
        if (elements == null || elements.isEmpty()) {
            return null;
        }
        return before.getPadding().withElements(HclRightPadded.withElements(before.elements, elements));
    }

    public static <H extends Hcl> HclContainer<H> withElements(HclContainer<H> before, @Nullable List<H> elements) {
        if (elements == null) {
            return before.getPadding().withElements(emptyList());
        }
        return before.getPadding().withElements(HclRightPadded.withElements(before.elements, elements));
    }

    @Override
    public String toString() {
        return "HclContainer(before=" + before + ", elementCount=" + elements.size() + ')';
    }
}
