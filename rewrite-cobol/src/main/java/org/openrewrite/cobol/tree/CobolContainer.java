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
package org.openrewrite.cobol.tree;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.RequiredArgsConstructor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;

import java.util.List;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptyList;

/**
 * AST elements that contain lists of trees with some delimiter like function call arguments.
 *
 * @param <T> The type of the inner list of elements.
 */
public class CobolContainer<T> {
    private transient Padding<T> padding;

    private static final CobolContainer<?> EMPTY = new CobolContainer<>("", emptyList(), Markers.EMPTY);

    private final String before;
    private final List<CobolRightPadded<T>> elements;
    private final Markers markers;

    private CobolContainer(String before, List<CobolRightPadded<T>> elements, Markers markers) {
        this.before = before;
        this.elements = elements;
        this.markers = markers;
    }

    public static <T> CobolContainer<T> build(List<CobolRightPadded<T>> elements) {
        return build("", elements, Markers.EMPTY);
    }

    @JsonCreator
    public static <T> CobolContainer<T> build(String before, List<CobolRightPadded<T>> elements, Markers markers) {
        if (before.isEmpty() && elements.isEmpty()) {
            return empty();
        }
        return new CobolContainer<>(before, elements, markers);
    }

    @SuppressWarnings("unchecked")
    public static <T> CobolContainer<T> empty() {
        return (CobolContainer<T>) EMPTY;
    }

    public CobolContainer<T> withBefore(String before) {
        return getBefore() == before ? this : build(before, elements, markers);
    }

    public CobolContainer<T> withMarkers(Markers markers) {
        return getMarkers() == markers ? this : build(before, elements, markers);
    }

    public Markers getMarkers() {
        return markers;
    }

    public List<T> getElements() {
        return CobolRightPadded.getElements(elements);
    }

    public String getBefore() {
        return before;
    }

    public CobolContainer<T> map(UnaryOperator<T> map) {
        return getPadding().withElements(ListUtils.map(elements, t -> t.map(map)));
    }

    public String getLastSpace() {
        return elements.isEmpty() ? "" : elements.get(elements.size() - 1).getAfter();
    }

    public Padding<T> getPadding() {
        if (padding == null) {
            this.padding = new Padding<>(this);
        }
        return padding;
    }

    @RequiredArgsConstructor
    public static class Padding<T> {
        private final CobolContainer<T> c;

        public List<CobolRightPadded<T>> getElements() {
            return c.elements;
        }

        public CobolContainer<T> withElements(List<CobolRightPadded<T>> elements) {
            return c.elements == elements ? c : build(c.before, elements, c.markers);
        }
    }

    @Nullable
    public static <P extends Cobol> CobolContainer<P> withElementsNullable(@Nullable CobolContainer<P> before, @Nullable List<P> elements) {
        if (before == null) {
            if (elements == null || elements.isEmpty()) {
                return null;
            }
            return CobolContainer.build("", CobolRightPadded.withElements(emptyList(), elements), Markers.EMPTY);
        }
        if (elements == null || elements.isEmpty()) {
            return null;
        }
        return before.getPadding().withElements(CobolRightPadded.withElements(before.elements, elements));
    }

    public static <P extends Cobol> CobolContainer<P> withElements(CobolContainer<P> before, @Nullable List<P> elements) {
        if (elements == null) {
            return before.getPadding().withElements(emptyList());
        }
        return before.getPadding().withElements(CobolRightPadded.withElements(before.elements, elements));
    }

    @Override
    public String toString() {
        return "ProtoContainer(before=" + before + ", elementCount=" + elements.size() + ')';
    }
}
