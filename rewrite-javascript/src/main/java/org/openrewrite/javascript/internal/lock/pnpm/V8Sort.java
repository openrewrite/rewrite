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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * V8's {@code Array.prototype.sort} (array-sort.tq) for fewer than 64 elements, whose order pnpm's inconsistent
 * comparators depend on.
 */
final class V8Sort {

    private V8Sort() {
    }

    /**
     * Sorts in place; cannot-know for 64 elements or more, where V8 merges runs.
     */
    static <T> void sort(List<T> a, Comparator<T> c, String what) {
        int n = a.size();
        if (n < 2) {
            return;
        }
        if (n >= 64) {
            throw new CannotKnow("the order V8 sorts " + n + " " + what + " in");
        }
        int runHi;
        if (c.compare(a.get(1), a.get(0)) < 0) {
            runHi = 2;
            while (runHi < n && c.compare(a.get(runHi), a.get(runHi - 1)) < 0) {
                runHi++;
            }
            Collections.reverse(a.subList(0, runHi));
        } else {
            runHi = 2;
            while (runHi < n && c.compare(a.get(runHi), a.get(runHi - 1)) >= 0) {
                runHi++;
            }
        }
        for (int start = runHi; start < n; start++) {
            T pivot = a.get(start);
            int left = 0;
            int right = start;
            while (left < right) {
                int mid = left + ((right - left) >> 1);
                if (c.compare(pivot, a.get(mid)) < 0) {
                    right = mid;
                } else {
                    left = mid + 1;
                }
            }
            for (int p = start; p > left; p--) {
                a.set(p, a.get(p - 1));
            }
            a.set(left, pivot);
        }
    }
}
