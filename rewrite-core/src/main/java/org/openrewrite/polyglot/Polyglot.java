/*
 *  Copyright 2021 the original author or authors.
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  https://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openrewrite.polyglot;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.With;
import org.graalvm.polyglot.Value;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;


@SuppressWarnings("unchecked")
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
public interface Polyglot extends Serializable, Tree {

    UUID getId();

    Value getValue();

    default <T> Optional<T> as(Class<T> targetType) {
        try {
            return Optional.ofNullable(getValue().as(targetType));
        } catch (ClassCastException e) {
            return Optional.empty();
        }
    }

    @Override
    default <R extends Tree, P> @Nullable R accept(TreeVisitor<R, P> v, P p) {
        return v instanceof PolyglotVisitor ? (R) acceptPolyglot((PolyglotVisitor<P>) v, p) : v.defaultValue(this, p);
    }

    @Nullable
    default <P> Polyglot acceptPolyglot(PolyglotVisitor<P> pv, P p) {
        return pv.defaultValue(this, p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v instanceof PolyglotVisitor;
    }

    interface Member extends Polyglot {
        Markers getMarkers();

        <P extends Polyglot> P withMarkers(Markers markers);

        String getName();

        Member withValue(Value value);

        @Override
        default @Nullable <P> Polyglot acceptPolyglot(PolyglotVisitor<P> pv, P p) {
            return pv.visitMember(this, p);
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ValueMarker implements Marker {
        @EqualsAndHashCode.Include
        UUID id;

        Value value;
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Source implements Polyglot, SourceFile {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;

        Path sourcePath;

        Value value;

        Members members;

        public Source(UUID id, Markers markers, Path sourcePath, Value value) {
            this(id, markers, sourcePath, value, new Members(randomId(), Markers.EMPTY, sourcePath.toString(), value));
        }

        public Source(UUID id, Markers markers, Path sourcePath, Value value, Members members) {
            this.id = id;
            this.markers = markers;
            this.sourcePath = sourcePath;
            this.value = value;
            this.members = members;
        }

        @Override
        public @Nullable <P> Polyglot acceptPolyglot(PolyglotVisitor<P> pv, P p) {
            return pv.visitSource(this, p);
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Members implements Member {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;

        String name;

        Value value;

        List<String> names;

        List<Member> members;

        public Members(UUID id, Markers markers, String name, Value value) {
            this.id = id;
            this.markers = markers;
            this.name = name;
            this.value = value;
            this.names = value.getMemberKeys().stream()
                    .sorted()
                    .collect(toList());
            this.members = names.stream()
                    .map(key -> {
                        Value v = value.getMember(key);
                        v.getMetaObject().putMember("member", key);
                        if (v.canInstantiate()) {
                            return new Instantiable(randomId(), Markers.EMPTY, key, v);
                        } else if (v.canExecute()) {
                            return new Executable(randomId(), Markers.EMPTY, key, v);
                        } else if (v.hasMembers()) {
                            return new Members(randomId(), Markers.EMPTY, key, v);
                        }
                        return new Instance(randomId(), Markers.EMPTY, key, v);
                    })
                    .collect(toList());
        }

        public Members(UUID id, Markers markers, String name, Value value, List<String> names, List<Member> members) {
            this.id = id;
            this.markers = markers;
            this.name = name;
            this.value = value;
            this.names = names;
            this.members = members;
        }

        @Override
        public @Nullable <P> Polyglot acceptPolyglot(PolyglotVisitor<P> pv, P p) {
            return pv.visitMembers(this, p);
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Executable implements Member {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;

        String name;

        Value value;

        public Instance execute(Object... args) {
            String name = value.getMetaSimpleName();
            Value v;
            try {
                v = value.execute(args);
            } catch (Throwable t) {
                v = Value.asValue(t);
            }
            return new Instance(randomId(), Markers.EMPTY, name, v);
        }

        public Executable executeVoid(Object... args) {
            getValue().executeVoid(args);
            return this;
        }

        @Override
        public @Nullable <P> Polyglot acceptPolyglot(PolyglotVisitor<P> pv, P p) {
            return pv.visitExecutable(this, p);
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Error implements Polyglot {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;

        Value value;

        @Override
        public @Nullable <P> Polyglot acceptPolyglot(PolyglotVisitor<P> pv, P p) {
            return pv.visitError(this, p);
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Instance implements Member {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;

        String name;

        Value value;

        @Override
        public @Nullable <P> Polyglot acceptPolyglot(PolyglotVisitor<P> pv, P p) {
            return pv.visitInstance(this, p);
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Instantiable implements Member {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;

        String name;

        Value value;

        public Polyglot.Instance instantiate(Object... args) {
            Value v;
            try {
                v = value.newInstance(args);
                v.getMetaObject().putMember("member", name);
            } catch (Throwable t) {
                v = Value.asValue(t);
            }
            return new Instance(randomId(), Markers.EMPTY, value.getMetaSimpleName(), v);
        }

        @Override
        public @Nullable <P> Polyglot acceptPolyglot(PolyglotVisitor<P> pv, P p) {
            return pv.visitInstantiable(this, p);
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class StringValue implements Polyglot {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;

        String stringValue;

        @Override
        public Value getValue() {
            return Value.asValue(stringValue);
        }

        @Override
        public @Nullable <P> Polyglot acceptPolyglot(PolyglotVisitor<P> pv, P p) {
            return pv.visitStringValue(this, p);
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class NumberValue implements Polyglot {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;

        Number numberValue;

        @Override
        public Value getValue() {
            return Value.asValue(numberValue);
        }

        @Override
        public @Nullable <P> Polyglot acceptPolyglot(PolyglotVisitor<P> pv, P p) {
            return pv.visitNumberValue(this, p);
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class HashEntries implements Polyglot {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Markers markers;

        @Getter
        Value value;

        Set<String> keys;

        @Getter
        List<HashEntry> entries;

        public HashEntries(UUID id, Markers markers, Value value) {
            this(id, markers, value, value.getMemberKeys(), value.getMemberKeys().stream()
                    .map(key -> new HashEntry(randomId(), Markers.EMPTY, key, value.getMember(key)))
                    .collect(toList()));
        }

        public HashEntries(UUID id, Markers markers, Value value, Set<String> keys, List<HashEntry> entries) {
            this.id = id;
            this.markers = markers;
            this.value = value;
            this.keys = keys;
            this.entries = entries;
        }

        public HashEntries withValue(Value value) {
            if (value == getValue()) {
                return this;
            }
            return new HashEntries(randomId(), markers, value);
        }

        public HashEntries withEntries(List<HashEntry> entries) {
            return new HashEntries(randomId(), markers, value, entries.stream().map(HashEntry::getKey).collect(Collectors.toSet()), entries);
        }

        @Override
        public @Nullable <P> Polyglot acceptPolyglot(PolyglotVisitor<P> pv, P p) {
            return pv.visitHashEntries(this, p);
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class HashEntry implements Polyglot {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;

        String key;

        Value value;

        @Override
        public @Nullable <P> Polyglot acceptPolyglot(PolyglotVisitor<P> pv, P p) {
            return pv.visitHashEntry(this, p);
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ArrayElements implements Polyglot {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;

        Value value;

        List<ArrayElement> elements;

        public ArrayElements(UUID id, Markers markers, Value value) {
            this(id, markers, value, IntStream.range(0, ((Long) value.getArraySize()).intValue())
                    .mapToObj(i -> {
                        Value v = value.getArrayElement(i);
                        return new ArrayElement(randomId(), Markers.EMPTY, i, v);
                    })
                    .collect(toList()));
        }

        public ArrayElements(UUID id, Markers markers, Value value, List<ArrayElement> elements) {
            this.id = id;
            this.markers = markers;
            this.value = value;
            this.elements = elements;
        }

        @Override
        public @Nullable <P> Polyglot acceptPolyglot(PolyglotVisitor<P> pv, P p) {
            return pv.visitArrayElements(this, p);
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ArrayElement implements Polyglot {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;

        int index;

        Value value;

        @Override
        public @Nullable <P> Polyglot acceptPolyglot(PolyglotVisitor<P> pv, P p) {
            return pv.visitArrayElement(this, p);
        }
    }

}
