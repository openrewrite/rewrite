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
package org.openrewrite.javascript.internal.lock.pnpm;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Port of semver-range-intersect 0.3.1 over the comparators of node-semver's {@code Range.set} (semver 7.8,
 * which pnpm 11.2.2 bundles).
 */
final class SemverRangeIntersect {

    private SemverRangeIntersect() {
    }

    /**
     * A {@code semver.Comparator}: its operator and version, both empty for the {@code ANY} comparator.
     */
    private static final class Comparator {
        final String operator;
        final @Nullable String version;

        Comparator(String operator, @Nullable String version) {
            this.operator = operator;
            this.version = version;
        }

        static Comparator parse(String value) {
            if (value.isEmpty() || "*".equals(value)) {
                return new Comparator("", null);
            }
            int i = 0;
            while (i < value.length() && "<>=".indexOf(value.charAt(i)) >= 0) {
                i++;
            }
            String operator = value.substring(0, i);
            String version = NodeSemver.valid(value.substring(i), NodeSemver.Options.DEFAULT);
            return new Comparator("=".equals(operator) ? "" : operator, version);
        }

        boolean any() {
            return version == null;
        }

        String value() {
            return version == null ? "" : operator + version;
        }

        String rangeString() {
            return version == null ? "*" : value();
        }

        boolean intersects(Comparator other) {
            return NodeSemver.intersects(rangeString(), other.rangeString(), NodeSemver.Options.DEFAULT);
        }
    }

    private interface Single {
        @Nullable
        Single intersect(Single other);

        @Nullable
        Single merge(Single other);
    }

    private static final class SingleVer implements Single {
        final Comparator comp;

        SingleVer(Comparator comp) {
            this.comp = comp;
        }

        @Override
        public String toString() {
            return comp.value();
        }

        @Override
        public @Nullable Single intersect(Single other) {
            return semverIntersects(toString(), other.toString()) ? this : null;
        }

        @Override
        public @Nullable Single merge(Single other) {
            return semverIntersects(toString(), other.toString()) ? other : null;
        }
    }

    private static final class SingleRange implements Single {
        final Comparator lowerBound;
        final Comparator upperBound;

        SingleRange(Comparator lowerBound, Comparator upperBound) {
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
            if (!lowerBound.intersects(upperBound)) {
                throw new IllegalArgumentException("Invalid range; version range does not intersect: " + this);
            }
        }

        @Override
        public String toString() {
            List<String> parts = new ArrayList<>();
            for (String v : Arrays.asList(lowerBound.value(), upperBound.value())) {
                if (!v.isEmpty()) {
                    parts.add(v);
                }
            }
            return String.join(" ", parts);
        }

        @Override
        public @Nullable Single intersect(Single other) {
            if (!semverIntersects(toString(), other.toString())) {
                return null;
            }
            if (other instanceof SingleVer) {
                return other;
            }
            SingleRange range = (SingleRange) other;
            List<Comparator> lowers = new ArrayList<>(Arrays.asList(lowerBound, range.lowerBound));
            List<Comparator> uppers = new ArrayList<>(Arrays.asList(upperBound, range.upperBound));
            List<Comparator> lowerList = new ArrayList<>(lowers);
            for (Comparator c : uppers) {
                if (!c.any()) {
                    lowerList.add(c);
                }
            }
            List<Comparator> upperList = new ArrayList<>(uppers);
            for (Comparator c : lowers) {
                if (!c.any()) {
                    upperList.add(c);
                }
            }
            Comparator lower = getLowerBoundComparator(lowerList, false);
            Comparator upper = getUpperBoundComparator(upperList, false);
            if (isSameVersionEqualsLikeComparator(lower, upper)) {
                return new SingleVer(stripComparatorOperator(lower));
            }
            return new SingleRange(lower, upper);
        }

        @Override
        public @Nullable Single merge(Single other) {
            if (!semverIntersects(toString(), other.toString())) {
                return null;
            }
            if (other instanceof SingleVer) {
                return this;
            }
            SingleRange range = (SingleRange) other;
            Comparator lower = mergeLower(lowerBound, range.lowerBound);
            Comparator upper = mergeUpper(upperBound, range.upperBound);
            return lower != null && upper != null ? new SingleRange(lower, upper) : null;
        }

        private static @Nullable Comparator mergeLower(Comparator a, Comparator b) {
            if (a.any()) {
                return isPrerelease(b) ? null : a;
            } else if (b.any()) {
                return isPrerelease(a) ? null : b;
            }
            int cmpMain = compareMain(a.version, b.version);
            if (cmpMain < 0 && isPrerelease(b) || cmpMain > 0 && isPrerelease(a)) {
                return null;
            }
            int semverCmp = compare(a.version, b.version);
            if (a.operator.equals(b.operator) || semverCmp != 0) {
                return semverCmp < 0 ? a : b;
            }
            return ">=".equals(a.operator) ? a : b;
        }

        private static @Nullable Comparator mergeUpper(Comparator a, Comparator b) {
            if (a.any()) {
                return isPrerelease(b) ? null : a;
            } else if (b.any()) {
                return isPrerelease(a) ? null : b;
            }
            int cmpMain = compareMain(a.version, b.version);
            if (cmpMain > 0 && isPrerelease(b) || cmpMain < 0 && isPrerelease(a)) {
                return null;
            }
            int semverCmp = compare(a.version, b.version);
            if (a.operator.equals(b.operator) || semverCmp != 0) {
                return semverCmp > 0 ? a : b;
            }
            return "<=".equals(a.operator) ? a : b;
        }
    }

    /**
     * {@code intersect(...ranges)}: the range every given range admits, {@code *} for no bound, null when they do not
     * intersect or one does not parse.
     */
    static @Nullable String intersect(List<String> ranges) {
        List<String> formatted = new ArrayList<>();
        try {
            for (String range : ranges) {
                String valid = NodeSemver.validRange(range, NodeSemver.Options.DEFAULT);
                if (valid == null) {
                    return null;
                }
                formatted.add(valid);
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
        for (int i = 0; i < formatted.size(); i++) {
            for (int j = i + 1; j < formatted.size(); j++) {
                if (!NodeSemver.intersects(formatted.get(i), formatted.get(j), NodeSemver.Options.DEFAULT)) {
                    return null;
                }
            }
        }
        List<Single> result = null;
        for (String range : formatted) {
            List<Single> multi = multiRange(range);
            result = result == null ? multi : intersect(result, multi);
        }
        if (result == null || result.isEmpty()) {
            return null;
        }
        Set<String> unique = new LinkedHashSet<>();
        for (Single single : result) {
            unique.add(single.toString());
        }
        String joined = String.join(" || ", unique);
        return joined.isEmpty() ? "*" : joined;
    }

    private static List<Single> multiRange(String formatted) {
        List<@Nullable Single> singles = new ArrayList<>();
        for (String set : formatted.split("\\|\\|", -1)) {
            List<Comparator> comparators = new ArrayList<>();
            for (String value : set.trim().split(" ")) {
                comparators.add(Comparator.parse(value));
            }
            singles.add(createSingleRange(comparators));
        }
        return withoutNulls(normalizeSingleRangeList(singles));
    }

    // MultiRange.intersect of two valid ranges, an empty one standing for the reduce's start
    private static List<Single> intersect(List<Single> a, List<Single> b) {
        if (a.isEmpty()) {
            return b;
        }
        if (b.isEmpty()) {
            return a;
        }
        List<@Nullable Single> singles = new ArrayList<>();
        for (Single x : a) {
            for (Single y : b) {
                Single i = x.intersect(y);
                if (i != null) {
                    singles.add(i);
                }
            }
        }
        return withoutNulls(normalizeSingleRangeList(singles));
    }

    private static List<Single> withoutNulls(List<@Nullable Single> list) {
        List<Single> out = new ArrayList<>();
        for (Single s : list) {
            if (s != null) {
                out.add(s);
            }
        }
        return out;
    }

    private static List<@Nullable Single> normalizeSingleRangeList(List<@Nullable Single> singleRangeList) {
        List<@Nullable Single> acc = new ArrayList<>();
        for (Single singleRange : singleRangeList) {
            if (singleRange == null) {
                acc.add(null);
                continue;
            }
            boolean insertFirst = false;
            List<Integer> removeIndexList = new ArrayList<>();
            Single append = singleRange;
            for (int index = 0; index < acc.size(); index++) {
                Single inserted = acc.get(index);
                if (inserted != null && append != null) {
                    Single merged = inserted.merge(append);
                    if (merged != null) {
                        if (merged.toString().equals(inserted.toString())) {
                            append = null;
                            continue;
                        }
                        removeIndexList.add(index);
                        if (inserted instanceof SingleRange && append instanceof SingleRange) {
                            insertFirst = true;
                        }
                        append = merged;
                    }
                }
            }
            List<@Nullable Single> removed = new ArrayList<>();
            for (int index = 0; index < acc.size(); index++) {
                if (!removeIndexList.contains(index)) {
                    removed.add(acc.get(index));
                }
            }
            if (append != null) {
                if (insertFirst) {
                    removed.add(0, append);
                } else {
                    removed.add(append);
                }
            }
            acc = removed;
        }
        return acc;
    }

    private static @Nullable Single createSingleRange(List<Comparator> comparators) {
        List<Comparator> equals = new ArrayList<>();
        for (Comparator c : comparators) {
            if (!c.any() && c.operator.isEmpty()) {
                boolean seen = false;
                for (Comparator e : equals) {
                    seen |= e.value().equals(c.value());
                }
                if (!seen) {
                    equals.add(c);
                }
            }
        }
        switch (equals.size()) {
            case 0: {
                Comparator lower = getLowerBoundComparator(comparators, true);
                Comparator upper = getUpperBoundComparator(comparators, true);
                if (isSameVersionEqualsLikeComparator(lower, upper)) {
                    return new SingleVer(stripComparatorOperator(lower));
                }
                try {
                    return new SingleRange(lower, upper);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
            case 1:
                return new SingleVer(equals.get(0));
            default:
                return null;
        }
    }

    private static Comparator getLowerBoundComparator(List<Comparator> comparatorList, boolean singleRange) {
        List<Comparator> valid = new ArrayList<>();
        List<String> le = new ArrayList<>();
        for (Comparator c : comparatorList) {
            if (">".equals(c.operator) || ">=".equals(c.operator) || c.any()) {
                valid.add(c);
            }
            if ("<=".equals(c.operator)) {
                le.add(c.version == null ? "" : c.version);
            }
        }
        if (valid.isEmpty()) {
            return new Comparator("", null);
        }
        Comparator acc = valid.get(0);
        for (int i = 1; i < valid.size(); i++) {
            acc = lower(acc, valid.get(i), singleRange, le);
        }
        return acc;
    }

    private static Comparator lower(Comparator a, Comparator b, boolean singleRange, List<String> le) {
        if (a.any()) {
            if (!singleRange && isPrerelease(b) && !(">=".equals(b.operator) && le.contains(b.version))) {
                return new Comparator(">=", stripPrerelease(b.version));
            }
            return b;
        } else if (b.any()) {
            if (!singleRange && isPrerelease(a) && !(">=".equals(a.operator) && le.contains(a.version))) {
                return new Comparator(">=", stripPrerelease(a.version));
            }
            return a;
        }
        int semverCmp = compare(a.version, b.version);
        if (a.operator.equals(b.operator) || semverCmp != 0) {
            if (!singleRange) {
                int cmpMain = compareMain(a.version, b.version);
                if (cmpMain != 0 && isPrerelease(a) && isPrerelease(b)) {
                    return cmpMain > 0 ? new Comparator(a.operator, stripPrerelease(a.version)) :
                            new Comparator(b.operator, stripPrerelease(b.version));
                }
            }
            return semverCmp > 0 ? a : b;
        }
        return ">".equals(a.operator) ? a : b;
    }

    private static Comparator getUpperBoundComparator(List<Comparator> comparatorList, boolean singleRange) {
        List<Comparator> valid = new ArrayList<>();
        List<String> ge = new ArrayList<>();
        for (Comparator c : comparatorList) {
            if ("<".equals(c.operator) || "<=".equals(c.operator) || c.any()) {
                valid.add(c);
            }
            if (">=".equals(c.operator)) {
                ge.add(c.version == null ? "" : c.version);
            }
        }
        if (valid.isEmpty()) {
            return new Comparator("", null);
        }
        Comparator acc = valid.get(0);
        for (int i = 1; i < valid.size(); i++) {
            acc = upper(acc, valid.get(i), singleRange, ge);
        }
        return acc;
    }

    private static Comparator upper(Comparator a, Comparator b, boolean singleRange, List<String> ge) {
        if (a.any()) {
            if (!singleRange && isPrerelease(b) && !("<=".equals(b.operator) && ge.contains(b.version))) {
                return new Comparator("<", stripPrerelease(b.version));
            }
            return b;
        } else if (b.any()) {
            if (!singleRange && isPrerelease(a) && !("<=".equals(a.operator) && ge.contains(a.version))) {
                return new Comparator("<", stripPrerelease(a.version));
            }
            return a;
        }
        int semverCmp = compare(a.version, b.version);
        if (a.operator.equals(b.operator) || semverCmp != 0) {
            if (!singleRange) {
                int cmpMain = compareMain(a.version, b.version);
                if (cmpMain != 0 && isPrerelease(a) && isPrerelease(b)) {
                    return cmpMain < 0 ? new Comparator("<", stripPrerelease(a.version)) :
                            new Comparator("<", stripPrerelease(b.version));
                }
            }
            return semverCmp < 0 ? a : b;
        }
        return "<".equals(a.operator) ? a : b;
    }

    private static boolean isSameVersionEqualsLikeComparator(Comparator a, Comparator b) {
        return a.version != null && b.version != null && a.version.equals(b.version) &&
               (a.operator.isEmpty() || a.operator.contains("=")) &&
               (b.operator.isEmpty() || b.operator.contains("="));
    }

    private static Comparator stripComparatorOperator(Comparator comparator) {
        return comparator.operator.isEmpty() ? comparator : new Comparator("", comparator.version);
    }

    private static boolean semverIntersects(String a, String b) {
        return NodeSemver.intersects(a.isEmpty() ? "*" : a, b.isEmpty() ? "*" : b, NodeSemver.Options.DEFAULT);
    }

    private static boolean isPrerelease(Comparator c) {
        return c.version != null && c.version.indexOf('-') >= 0;
    }

    private static String stripPrerelease(@Nullable String version) {
        if (version == null) {
            return "";
        }
        int plus = version.indexOf('+');
        String core = plus < 0 ? version : version.substring(0, plus);
        int dash = core.indexOf('-');
        return dash < 0 ? version : core.substring(0, dash);
    }

    private static int compare(@Nullable String a, @Nullable String b) {
        return NodeSemver.compare(a, b, NodeSemver.Options.DEFAULT);
    }

    private static int compareMain(@Nullable String a, @Nullable String b) {
        return NodeSemver.compare(stripPrerelease(a), stripPrerelease(b), NodeSemver.Options.DEFAULT);
    }
}
