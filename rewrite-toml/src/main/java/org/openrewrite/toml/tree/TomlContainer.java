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
package org.openrewrite.toml.tree;

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
public class TomlContainer<T> {
    private transient Padding<T> padding;

    private static final TomlContainer<?> EMPTY = new TomlContainer<>(Space.EMPTY, emptyList(), Markers.EMPTY);

    private final Space before;

    private final List<TomlRightPadded<T>> elements;
    private final Markers markers;

    private TomlContainer(Space before, List<TomlRightPadded<T>> elements, Markers markers) {
        this.before = before;
        this.elements = elements;
        this.markers = markers;
    }

    public static <T> TomlContainer<T> build(List<TomlRightPadded<T>> elements) {
        return build(Space.EMPTY, elements, Markers.EMPTY);
    }

    @JsonCreator
    public static <T> TomlContainer<T> build(Space before, List<TomlRightPadded<T>> elements, Markers markers) {
        if (before.isEmpty() && elements.isEmpty()) {
            return empty();
        }
        return new TomlContainer<>(before, elements, markers);
    }

    @SuppressWarnings("unchecked")
    public static <T> TomlContainer<T> empty() {
        return (TomlContainer<T>) EMPTY;
    }

    public TomlContainer<T> withBefore(Space before) {
        return this.before == before ? this : build(before, elements, markers);
    }

    public TomlContainer<T> withElements(List<TomlRightPadded<T>> elements) {
        return this.elements == elements ? this : build(before, elements, markers);
    }

    public TomlContainer<T> withMarkers(Markers markers) {
        return this.markers == markers ? this : build(before, elements, markers);
    }

    public Markers getMarkers() {
        return markers;
    }

    public List<T> getElements() {
        return TomlRightPadded.getElements(elements);
    }

    public Space getBefore() {
        return before;
    }

    public TomlContainer<T> map(UnaryOperator<T> map) {
        return getPadding().withElements(ListUtils.map(elements, t -> t.map(map)));
    }

    public Space getLastSpace() {
        return elements.isEmpty() ? Space.EMPTY : elements.get(elements.size() - 1).getAfter();
    }

    public TomlContainer<T> withLastSpace(Space after) {
        return withElements(ListUtils.mapLast(elements, elem -> elem.withAfter(after)));
    }

    public Padding<T> getPadding() {
        if (padding == null) {
            this.padding = new Padding<>(this);
        }
        return padding;
    }

    @RequiredArgsConstructor
    public static class Padding<T> {
        private final TomlContainer<T> c;

        public List<TomlRightPadded<T>> getElements() {
            return c.elements;
        }

        public TomlContainer<T> withElements(List<TomlRightPadded<T>> elements) {
            return c.elements == elements ? c : build(c.before, c.elements, c.markers);
        }
    }

    @Nullable
    public static <P extends Toml> TomlContainer<P> withElementsNullable(@Nullable TomlContainer<P> before, @Nullable List<P> elements) {
        if (before == null) {
            if (elements == null || elements.isEmpty()) {
                return null;
            }
            return TomlContainer.build(Space.EMPTY, TomlRightPadded.withElements(emptyList(), elements), Markers.EMPTY);
        }
        if (elements == null || elements.isEmpty()) {
            return null;
        }
        return before.getPadding().withElements(TomlRightPadded.withElements(before.elements, elements));
    }

    public static <P extends Toml> TomlContainer<P> withElements(TomlContainer<P> before, @Nullable List<P> elements) {
        if (elements == null) {
            return before.getPadding().withElements(emptyList());
        }
        return before.getPadding().withElements(TomlRightPadded.withElements(before.elements, elements));
    }

    @Override
    public String toString() {
        return "TomlContainer(before=" + before + ", elementCount=" + elements.size() + ')';
    }
}
