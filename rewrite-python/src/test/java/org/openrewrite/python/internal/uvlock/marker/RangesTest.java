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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RangesTest {

    // segments in interval notation, such as "[1,3) (5,*)", with * for unbounded
    private static Ranges<Integer> r(String spec) {
        Ranges<Integer> ranges = Ranges.empty();
        for (String segment : spec.trim().split("\\s+")) {
            String[] ends = segment.substring(1, segment.length() - 1).split(",");
            Ranges.Bound<Integer> start = "*".equals(ends[0]) ? Ranges.Bound.unbounded() :
              segment.charAt(0) == '[' ? Ranges.Bound.included(Integer.valueOf(ends[0]))
                      : Ranges.Bound.excluded(Integer.valueOf(ends[0]));
            Ranges.Bound<Integer> end = "*".equals(ends[1]) ? Ranges.Bound.unbounded() :
              segment.charAt(segment.length() - 1) == ']' ? Ranges.Bound.included(Integer.valueOf(ends[1]))
                      : Ranges.Bound.excluded(Integer.valueOf(ends[1]));
            ranges = ranges.union(Ranges.fromRangeBounds(start, end));
        }
        return ranges;
    }

    @Test
    void complementFlipsEveryBound() {
        assertThat(Ranges.<Integer>empty().complement()).isEqualTo(Ranges.full());
        assertThat(Ranges.<Integer>full().complement()).isEqualTo(Ranges.empty());
        assertThat(r("[1,3) [5,8]").complement()).isEqualTo(r("(*,1) [3,5) (8,*)"));
        assertThat(r("(*,2]").complement()).isEqualTo(r("(2,*)"));
        assertThat(r("[4,4]").complement()).isEqualTo(r("(*,4) (4,*)"));
    }

    @Test
    void unionMergesTouchingSegmentsOnlyWithoutAGap() {
        assertThat(r("[1,3)").union(r("[3,5)"))).isEqualTo(r("[1,5)"));
        // both exclude 3, so 3 itself is a gap and the segments stay apart
        assertThat(r("[1,3)").union(r("(3,5)"))).isEqualTo(r("[1,3) (3,5)"));
        assertThat(r("[1,4] [6,7]").union(r("[3,6)"))).isEqualTo(r("[1,7]"));
        assertThat(r("[1,2]").union(Ranges.full())).isEqualTo(Ranges.full());
    }

    @Test
    void intersectionKeepsTheTighterBound() {
        assertThat(r("[1,5)").intersection(r("(3,8]"))).isEqualTo(r("(3,5)"));
        assertThat(r("[1,3) [5,9)").intersection(r("[2,6)"))).isEqualTo(r("[2,3) [5,6)"));
        assertThat(r("[1,3)").intersection(r("[3,5)"))).isEqualTo(Ranges.empty());
        assertThat(r("[3,3]").intersection(r("[3,5)"))).isEqualTo(r("[3,3]"));
    }

    @Test
    void disjointRespectsBoundKinds() {
        assertThat(r("[1,3)").isDisjoint(r("[3,5)"))).isTrue();
        assertThat(r("[1,3]").isDisjoint(r("[3,5)"))).isFalse();
    }

    @Test
    void boundsWithNoValueBetweenThemAreEmpty() {
        assertThat(Ranges.fromRangeBounds(Ranges.Bound.excluded(5),
                Ranges.Bound.included(5))).isEqualTo(Ranges.empty());
        assertThat(r("[4,4]")).isEqualTo(Ranges.singleton(4));
        assertThat(r("[1,3) (6,*)").boundingRange()).isEqualTo(r("[1,*)").segments().get(0));
    }
}
