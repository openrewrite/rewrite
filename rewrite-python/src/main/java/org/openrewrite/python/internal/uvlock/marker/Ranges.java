/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python.internal.uvlock.marker;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A set of values as sorted, non-empty segments with a non-empty gap between neighbours.
 * That normal form makes equal sets equal objects, which the marker interner relies on to share nodes.
 */
@EqualsAndHashCode
public final class Ranges<V extends Comparable<V>> {

    /** One end of a segment. */
    @Value
    @EqualsAndHashCode(doNotUseGetters = true)
    public static class Bound<V extends Comparable<V>> {
        enum Kind { INCLUDED, EXCLUDED, UNBOUNDED }

        Kind kind;
        @Nullable V value;

        public static <V extends Comparable<V>> Bound<V> included(V value) {
            return new Bound<>(Kind.INCLUDED, value);
        }

        public static <V extends Comparable<V>> Bound<V> excluded(V value) {
            return new Bound<>(Kind.EXCLUDED, value);
        }

        public static <V extends Comparable<V>> Bound<V> unbounded() {
            return new Bound<>(Kind.UNBOUNDED, null);
        }

        public boolean isIncluded() {
            return kind == Kind.INCLUDED;
        }

        public boolean isExcluded() {
            return kind == Kind.EXCLUDED;
        }

        public boolean isUnbounded() {
            return kind == Kind.UNBOUNDED;
        }

        public V getValue() {
            assert value != null : "an unbounded end has no value";
            return value;
        }

        private Bound<V> flipped() {
            return isIncluded() ? excluded(getValue()) : included(getValue());
        }
    }

    @Value
    public static class Segment<V extends Comparable<V>> {
        Bound<V> start;
        Bound<V> end;
    }

    private final List<Segment<V>> segments;

    private Ranges(List<Segment<V>> segments) {
        this.segments = segments;
    }

    public static <V extends Comparable<V>> Ranges<V> empty() {
        return new Ranges<>(Collections.emptyList());
    }

    public static <V extends Comparable<V>> Ranges<V> full() {
        return fromRangeBounds(Bound.<V>unbounded(), Bound.<V>unbounded());
    }

    public static <V extends Comparable<V>> Ranges<V> singleton(V value) {
        return fromRangeBounds(Bound.included(value), Bound.included(value));
    }

    public static <V extends Comparable<V>> Ranges<V> higherThan(V value) {
        return fromRangeBounds(Bound.included(value), Bound.<V>unbounded());
    }

    public static <V extends Comparable<V>> Ranges<V> strictlyHigherThan(V value) {
        return fromRangeBounds(Bound.excluded(value), Bound.<V>unbounded());
    }

    public static <V extends Comparable<V>> Ranges<V> lowerThan(V value) {
        return fromRangeBounds(Bound.<V>unbounded(), Bound.included(value));
    }

    public static <V extends Comparable<V>> Ranges<V> strictlyLowerThan(V value) {
        return fromRangeBounds(Bound.<V>unbounded(), Bound.excluded(value));
    }

    /** The values between the two ends, empty when no value lies between them. */
    public static <V extends Comparable<V>> Ranges<V> fromRangeBounds(Bound<V> start, Bound<V> end) {
        return holdsAValue(start, end) ?
                new Ranges<>(Collections.singletonList(new Segment<>(start, end))) :
                empty();
    }

    public boolean isEmpty() {
        return segments.isEmpty();
    }

    public List<Segment<V>> segments() {
        return segments;
    }

    /** From the lowest start to the highest end, or null when empty. */
    public @Nullable Segment<V> boundingRange() {
        return segments.isEmpty() ? null :
                new Segment<>(segments.get(0).getStart(), segments.get(segments.size() - 1).getEnd());
    }

    public Ranges<V> complement() {
        List<Segment<V>> out = new ArrayList<>();
        Bound<V> gapStart = Bound.unbounded();
        for (Segment<V> s : segments) {
            if (!s.getStart().isUnbounded()) {
                out.add(new Segment<>(gapStart, s.getStart().flipped()));
            }
            if (s.getEnd().isUnbounded()) {
                return new Ranges<>(out);
            }
            gapStart = s.getEnd().flipped();
        }
        out.add(new Segment<>(gapStart, Bound.unbounded()));
        return new Ranges<>(out);
    }

    public Ranges<V> union(Ranges<V> other) {
        List<Segment<V>> all = new ArrayList<>(segments);
        all.addAll(other.segments);
        all.sort((a, b) -> compareStarts(a.getStart(), b.getStart()));
        List<Segment<V>> out = new ArrayList<>();
        for (Segment<V> s : all) {
            Segment<V> last = out.isEmpty() ? null : out.get(out.size() - 1);
            if (last != null && touches(last.getEnd(), s.getStart())) {
                Bound<V> end = compareEnds(last.getEnd(), s.getEnd()) >= 0 ? last.getEnd() : s.getEnd();
                out.set(out.size() - 1, new Segment<>(last.getStart(), end));
            } else {
                out.add(s);
            }
        }
        return new Ranges<>(out);
    }

    public Ranges<V> intersection(Ranges<V> other) {
        List<Segment<V>> out = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < segments.size() && j < other.segments.size()) {
            Segment<V> a = segments.get(i);
            Segment<V> b = other.segments.get(j);
            Bound<V> start = compareStarts(a.getStart(), b.getStart()) >= 0 ? a.getStart() : b.getStart();
            int ends = compareEnds(a.getEnd(), b.getEnd());
            Bound<V> end = ends <= 0 ? a.getEnd() : b.getEnd();
            if (holdsAValue(start, end)) {
                out.add(new Segment<>(start, end));
            }
            if (ends <= 0) {
                i++;
            }
            if (ends >= 0) {
                j++;
            }
        }
        return new Ranges<>(out);
    }

    public boolean isDisjoint(Ranges<V> other) {
        return intersection(other).isEmpty();
    }

    // an unbounded start is lowest, and at one value an included start comes first
    static <V extends Comparable<V>> int compareStarts(Bound<V> a, Bound<V> b) {
        if (a.isUnbounded() || b.isUnbounded()) {
            return Boolean.compare(!a.isUnbounded(), !b.isUnbounded());
        }
        int c = a.getValue().compareTo(b.getValue());
        return c != 0 ? c : Boolean.compare(a.isExcluded(), b.isExcluded());
    }

    // an unbounded end is highest, and at one value an excluded end comes first
    static <V extends Comparable<V>> int compareEnds(Bound<V> a, Bound<V> b) {
        if (a.isUnbounded() || b.isUnbounded()) {
            return Boolean.compare(a.isUnbounded(), b.isUnbounded());
        }
        int c = a.getValue().compareTo(b.getValue());
        return c != 0 ? c : Boolean.compare(a.isIncluded(), b.isIncluded());
    }

    private static <V extends Comparable<V>> boolean holdsAValue(Bound<V> start, Bound<V> end) {
        if (start.isUnbounded() || end.isUnbounded()) {
            return true;
        }
        int c = start.getValue().compareTo(end.getValue());
        return c < 0 || c == 0 && start.isIncluded() && end.isIncluded();
    }

    // a segment ending at end and one starting at start leave no value between them
    private static <V extends Comparable<V>> boolean touches(Bound<V> end, Bound<V> start) {
        if (end.isUnbounded() || start.isUnbounded()) {
            return true;
        }
        int c = end.getValue().compareTo(start.getValue());
        return c > 0 || c == 0 && !(end.isExcluded() && start.isExcluded());
    }

    @Override
    public String toString() {
        if (segments.isEmpty()) {
            return "∅";
        }
        List<String> parts = new ArrayList<>();
        for (Segment<V> s : segments) {
            Bound<V> start = s.getStart();
            Bound<V> end = s.getEnd();
            if (start.isIncluded() && end.isIncluded() && start.getValue().compareTo(end.getValue()) == 0) {
                parts.add("==" + start.getValue());
            } else if (start.isUnbounded() && end.isUnbounded()) {
                parts.add("*");
            } else {
                List<String> ends = new ArrayList<>();
                if (!start.isUnbounded()) {
                    ends.add((start.isIncluded() ? ">=" : ">") + start.getValue());
                }
                if (!end.isUnbounded()) {
                    ends.add((end.isIncluded() ? "<=" : "<") + end.getValue());
                }
                parts.add(String.join(", ", ends));
            }
        }
        return String.join(" | ", parts);
    }
}
