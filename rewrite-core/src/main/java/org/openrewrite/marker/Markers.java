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
package org.openrewrite.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BinaryOperator;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;

@Value
@With
public class Markers implements Tree {
    public static final Markers EMPTY = new Markers(randomId(), emptyList());

    UUID id;

    @With
    /*~~>*/List<Marker> markers;

    public static Markers build(Collection<? extends Marker> markers) {
        /*~~>*/List<Marker> markerList;
        if (markers instanceof /*~~>*/List) {
            //noinspection unchecked
            markerList = (/*~~>*/List<Marker>) markers;
        } else {
            markerList = new ArrayList<>(markers.size());
            markerList.addAll(markers);
        }
        return markers.isEmpty() ? EMPTY : new Markers(randomId(), markerList);
    }

    /**
     * {@link TreeVisitor} may respond to a marker to determine whether to act on
     * a source file or not.
     *
     * @return A marker collection containing any additional context about the containing {@link Tree} element.
     */
    public Collection<? extends Marker> entries() {
        return markers;
    }

    /**
     * Adds a new marker element to the collection.
     *
     * @param marker The data to add or update.
     * @return A new {@link Markers} with an added marker.
     */
    public Markers add(Marker marker) {
        for (Marker m : markers) {
            if (marker.equals(m)) {
                return this;
            }
        }
        /*~~>*/List<Marker> updatedmarker = new ArrayList<>(markers);
        updatedmarker.add(marker);
        return new Markers(id, updatedmarker);
    }

    /**
     * Add a new marker or update some existing marker.
     *
     * @param identity          A new marker to add if it doesn't already exist. Existence is determined by type equality.
     * @param remappingFunction The function that merges an existing marker.
     * @param <M>               The type of marker.
     * @return A new {@link Markers} with an added or updated marker.
     */
    public <M extends Marker> Markers computeByType(M identity, BinaryOperator<M> remappingFunction) {
        AtomicBoolean updated = new AtomicBoolean(false);
        /*~~>*/List<Marker> markers = ListUtils.map(/*~~>*/this.markers, m -> {
            if (m.getClass().equals(identity.getClass())) {
                updated.set(true);

                //noinspection unchecked
                return remappingFunction.apply((M) m, identity);
            }
            return m;
        });
        return withMarkers(!updated.get() ? ListUtils.concat(markers, identity) : markers);
    }

    public Markers removeByType(Class<? extends Marker> type) {
        return withMarkers(ListUtils.map(/*~~>*/this.markers, m -> type.equals(m.getClass()) ? null : m));
    }

    public <M extends Marker> Markers setByType(M m) {
        return computeByType(m, (replacement, existing) -> replacement);
    }

    /**
     * Add a new marker or update some existing marker.
     *
     * @param identity          A new marker to add if it doesn't already exist. Existence is determined by regular equality.
     * @param remappingFunction The function that merges an existing marker with the new marker.
     * @param <M>               The type of marker.
     * @return A new {@link Markers} with an added or updated marker.
     */
    public <M extends Marker> Markers compute(M identity, BinaryOperator<M> remappingFunction) {
        AtomicBoolean foundEqualMarker = new AtomicBoolean(false);
        /*~~>*/List<Marker> updatedMarkers = ListUtils.map(markers, m -> {
            if (m.equals(identity)) {
                foundEqualMarker.set(true);
                //noinspection unchecked
                return remappingFunction.apply((M) m, identity);
            }
            return m;
        });

        if (!foundEqualMarker.get()) {
            updatedMarkers = ListUtils.concat(updatedMarkers, identity);
        }

        return withMarkers(updatedMarkers);
    }

    /**
     * Add a new marker or update some existing marker.
     *
     * @param m   A marker, which may or may not already exist already.
     * @param <M> The marker type.
     * @return If a marker already exists that matches by object equality, an unchanged markers reference
     * is returned. Otherwise, the supplied marker is added.
     */
    public <M extends Marker> Markers addIfAbsent(M m) {
        return compute(m, (m1, m2) -> m1);
    }

    public <M extends Marker> /*~~>*/List<M> findAll(Class<M> markerType) {
        return markers.stream()
                .filter(markerType::isInstance)
                .map(markerType::cast)
                .collect(toList());
    }

    public <M extends Marker> Optional<M> findFirst(Class<M> markerType) {
        return markers.stream()
                .filter(markerType::isInstance)
                .map(markerType::cast)
                .findFirst();
    }

    public Markers searchResult() {
        return searchResult(null);
    }

    public Markers searchResult(@Nullable String description) {
        return computeByType(new SearchResult(randomId(), description), (s1, s2) -> s1 == null ? s2 : s1);
    }

    @Override
    public <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return false;
    }
}
