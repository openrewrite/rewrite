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
package org.openrewrite.protobuf.tree;

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
public final class ProtoContainer<T> {
    private transient Padding<T> padding;

    private static final ProtoContainer<?> EMPTY = new ProtoContainer<>(Space.EMPTY, emptyList(), Markers.EMPTY);

    private final Space before;
    private final List<ProtoRightPadded<T>> elements;
    private final Markers markers;

    private ProtoContainer(Space before, List<ProtoRightPadded<T>> elements, Markers markers) {
        this.before = before;
        this.elements = elements;
        this.markers = markers;
    }

    public static <T> ProtoContainer<T> build(List<ProtoRightPadded<T>> elements) {
        return build(Space.EMPTY, elements, Markers.EMPTY);
    }

    @JsonCreator
    public static <T> ProtoContainer<T> build(Space before, List<ProtoRightPadded<T>> elements, Markers markers) {
        if (before.isEmpty() && elements.isEmpty()) {
            return empty();
        }
        return new ProtoContainer<>(before, elements, markers);
    }

    @SuppressWarnings("unchecked")
    public static <T> ProtoContainer<T> empty() {
        return (ProtoContainer<T>) EMPTY;
    }

    public ProtoContainer<T> withBefore(Space before) {
        return getBefore() == before ? this : build(before, elements, markers);
    }

    public ProtoContainer<T> withMarkers(Markers markers) {
        return getMarkers() == markers ? this : build(before, elements, markers);
    }

    public Markers getMarkers() {
        return markers;
    }

    public List<T> getElements() {
        return ProtoRightPadded.getElements(elements);
    }

    public Space getBefore() {
        return before;
    }

    public ProtoContainer<T> map(UnaryOperator<T> map) {
        return getPadding().withElements(ListUtils.map(elements, t -> t.map(map)));
    }

    public Space getLastSpace() {
        return elements.isEmpty() ? Space.EMPTY : elements.get(elements.size() - 1).getAfter();
    }

    public Padding<T> getPadding() {
        if (padding == null) {
            this.padding = new Padding<>(this);
        }
        return padding;
    }

    @RequiredArgsConstructor
    public static class Padding<T> {
        private final ProtoContainer<T> c;

        public List<ProtoRightPadded<T>> getElements() {
            return c.elements;
        }

        public ProtoContainer<T> withElements(List<ProtoRightPadded<T>> elements) {
            return c.elements == elements ? c : build(c.before, elements, c.markers);
        }
    }

    @Nullable
    public static <P extends Proto> ProtoContainer<P> withElementsNullable(@Nullable ProtoContainer<P> before, @Nullable List<P> elements) {
        if (before == null) {
            if (elements == null || elements.isEmpty()) {
                return null;
            }
            return ProtoContainer.build(Space.EMPTY, ProtoRightPadded.withElements(emptyList(), elements), Markers.EMPTY);
        }
        if (elements == null || elements.isEmpty()) {
            return null;
        }
        return before.getPadding().withElements(ProtoRightPadded.withElements(before.elements, elements));
    }

    public static <P extends Proto> ProtoContainer<P> withElements(ProtoContainer<P> before, @Nullable List<P> elements) {
        if (elements == null) {
            return before.getPadding().withElements(emptyList());
        }
        return before.getPadding().withElements(ProtoRightPadded.withElements(before.elements, elements));
    }

    @Override
    public String toString() {
        return "ProtoContainer(before=" + before + ", elementCount=" + elements.size() + ')';
    }
}
