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
package org.openrewrite.javascript.internal.lock.bun;

import org.jspecify.annotations.Nullable;

/**
 * Port of bun's src/semver/intersects.rs (from 1.4.0, bun#38333), which the overrides' target ranges are matched
 * with ({@link BunRelease#readsScopedOverrides}).
 */
final class SemverIntersects {

    private SemverIntersects() {
    }

    private static final class Bound {
        final SemverVersion version;
        final boolean inclusive;

        Bound(SemverVersion version, boolean inclusive) {
            this.version = version;
            this.inclusive = inclusive;
        }
    }

    private static final class Interval {
        final BunRelease release;
        @Nullable Bound lower;
        @Nullable Bound upper;

        Interval(BunRelease release) {
            this.release = release;
        }

        Interval copy() {
            Interval i = new Interval(release);
            i.lower = lower;
            i.upper = upper;
            return i;
        }

        void raise(Bound b) {
            boolean replace;
            if (lower == null) {
                replace = true;
            } else {
                int order = b.version.orderWithoutBuild(lower.version, release);
                replace = order > 0 || order == 0 && !b.inclusive;
            }
            if (replace) {
                lower = b;
            }
        }

        void cap(Bound b) {
            boolean replace;
            if (upper == null) {
                replace = true;
            } else {
                int order = b.version.orderWithoutBuild(upper.version, release);
                replace = order < 0 || order == 0 && !b.inclusive;
            }
            if (replace) {
                upper = b;
            }
        }

        void narrow(SemverRange.Comparator c) {
            switch (c.op) {
                case UNSET:
                    break;
                case EQL:
                    raise(new Bound(c.version, true));
                    cap(new Bound(c.version, true));
                    break;
                case GT:
                    raise(new Bound(c.version, false));
                    break;
                case GTE:
                    raise(new Bound(c.version, true));
                    break;
                case LT:
                    cap(new Bound(c.version, false));
                    break;
                default:
                    cap(new Bound(c.version, true));
                    break;
            }
        }

        void andQuery(SemverQuery query) {
            for (SemverQuery q = query; q != null; q = q.next) {
                narrow(q.range.left);
                narrow(q.range.right);
            }
        }

        boolean isNonEmpty() {
            if (lower == null || upper == null) {
                return true;
            }
            int order = lower.version.orderWithoutBuild(upper.version, release);
            return order < 0 || order == 0 && lower.inclusive && upper.inclusive;
        }
    }

    /**
     * {@code Group::intersects}: whether some version satisfies both groups; prerelease-exclusion rules are not
     * modelled, comparators are compared directly.
     */
    static boolean intersects(SemverQuery.Group self, SemverQuery.Group other) {
        for (SemverQuery.List listA = self.head; listA != null; listA = listA.next) {
            Interval base = new Interval(self.release);
            base.andQuery(listA.head);
            if (!base.isNonEmpty()) {
                continue;
            }
            for (SemverQuery.List listB = other.head; listB != null; listB = listB.next) {
                Interval i = base.copy();
                i.andQuery(listB.head);
                if (i.isNonEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }
}
