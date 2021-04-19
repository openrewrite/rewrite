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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import org.openrewrite.Incubating;
import org.openrewrite.Tree;
import org.openrewrite.TreePrinter;
import org.openrewrite.TreeVisitor;

import java.util.*;
import java.util.function.BinaryOperator;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;

@Incubating(since = "7.0.0")
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
public class Markers implements Tree {
    public static final Markers EMPTY = new Markers(randomId(), emptyList()) {
        @Override
        public String toString() {
            return "Markers{EMPTY}";
        }
    };

    private final UUID id;
    private final Collection<? extends Marker> markers;

    private Markers(UUID id, Collection<? extends Marker> markers) {
        this.id = id;
        this.markers = markers;
    }

    @JsonCreator
    public static Markers build(Collection<? extends Marker> markers) {
        return markers.isEmpty() ? EMPTY : new Markers(randomId(), markers);
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
        if (markers.stream().anyMatch(marker::equals)) {
            return this;
        } else {
            List<Marker> updatedmarker = new ArrayList<>(markers);
            updatedmarker.add(marker);
            return new Markers(id, updatedmarker);
        }
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
        List<Marker> updatedmarker = new ArrayList<>(markers.size() + 1);
        boolean updated = false;
        for (Marker m : this.markers) {
            if (m.getClass().equals(identity.getClass())) {
                //noinspection unchecked
                updatedmarker.add(remappingFunction.apply((M) m, identity));
                updated = true;
            } else {
                updatedmarker.add(m);
            }
        }
        if (!updated) {
            updatedmarker.add(identity);
        }
        return new Markers(id, updatedmarker);
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
        List<Marker> updatedmarker = new ArrayList<>(markers.size() + 1);
        boolean updated = false;
        for (Marker m : this.markers) {
            if (m.equals(identity)) {
                //noinspection unchecked
                updatedmarker.add(remappingFunction.apply((M) m, identity));
                updated = true;
            } else {
                updatedmarker.add(m);
            }
        }
        if (!updated) {
            updatedmarker.add(identity);
        }
        return new Markers(id, updatedmarker);
    }

    /**
     * Add a new marker or update some existing marker.
     * @param identity
     * @param <M>
     * @return
     */
    public <M extends Marker> Markers addOrUpdate(M identity) {
        return compute(identity, (m1, m2) -> m2);
    }

    public <M extends Marker> List<M> findAll(Class<M> markerType) {
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

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return false;
    }

    @Override
    public <P> String print(TreePrinter<P> printer, P p) {
        return "";
    }
}
