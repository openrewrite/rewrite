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
package org.openrewrite.javascript.internal.lock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Port of V8 13.6's {@code Array.prototype.sort} (third_party/v8/builtins/array-sort.tq), the TimSort Node v24 runs,
 * whose order decides what an inconsistent comparator leaves.
 */
public final class V8Sort {

    private static final int MAX_MERGE_PENDING = 85;
    private static final int MIN_GALLOP_WINS = 7;
    private static final int SORT_STATE_TEMP_SIZE = 32;

    private final Comparator<Object> comparator;
    private final int[] pendingRuns = new int[2 * MAX_MERGE_PENDING];
    private final Object[] workArray;
    private Object[] tempArray = new Object[0];
    private int pendingRunsSize;
    private int minGallop = MIN_GALLOP_WINS;

    @SuppressWarnings("unchecked")
    private <T> V8Sort(Object[] workArray, Comparator<? super T> comparator) {
        this.workArray = workArray;
        this.comparator = (Comparator<Object>) comparator;
    }

    /**
     * Sorts in place: {@link EcmaScript#UNDEFINED} elements after the sorted ones, uncompared, and
     * {@link EcmaScript#HOLE}s last.
     */
    @SuppressWarnings("unchecked")
    public static <T> void sort(List<T> a, Comparator<? super T> c) {
        int length = a.size();
        if (length < 2) {
            return;
        }
        List<Object> work = new ArrayList<>(length);
        int numberOfUndefined = 0;
        for (T element : a) {
            if (element == EcmaScript.UNDEFINED) {
                numberOfUndefined++;
            } else if (element != EcmaScript.HOLE) {
                work.add(element);
            }
        }
        Object[] workArray = work.toArray();
        new V8Sort(workArray, c).arrayTimSortImpl(workArray.length);
        int index = 0;
        for (Object element : workArray) {
            a.set(index++, (T) element);
        }
        for (int end = index + numberOfUndefined; index < end; index++) {
            a.set(index, (T) EcmaScript.UNDEFINED);
        }
        for (; index < length; index++) {
            a.set(index, (T) EcmaScript.HOLE);
        }
    }

    private int compare(Object x, Object y) {
        return comparator.compare(x, y);
    }

    private void pushRun(int base, int length) {
        pendingRuns[pendingRunsSize << 1] = base;
        pendingRuns[(pendingRunsSize << 1) + 1] = length;
        pendingRunsSize++;
    }

    private int runBase(int run) {
        return pendingRuns[run << 1];
    }

    private int runLength(int run) {
        return pendingRuns[(run << 1) + 1];
    }

    private Object[] getTempArray(int requestedSize) {
        int minSize = Math.max(SORT_STATE_TEMP_SIZE, requestedSize);
        if (tempArray.length < minSize) {
            tempArray = new Object[minSize];
        }
        return tempArray;
    }

    private void binaryInsertionSort(int low, int startArg, int high) {
        for (int start = low == startArg ? startArg + 1 : startArg; start < high; ++start) {
            int left = low;
            int right = start;
            Object pivot = workArray[right];
            while (left < right) {
                int mid = left + ((right - left) >> 1);
                if (compare(pivot, workArray[mid]) < 0) {
                    right = mid;
                } else {
                    left = mid + 1;
                }
            }
            for (int p = start; p > left; --p) {
                workArray[p] = workArray[p - 1];
            }
            workArray[left] = pivot;
        }
    }

    private int countAndMakeRun(int lowArg, int high) {
        int low = lowArg + 1;
        if (low == high) {
            return 1;
        }
        int runLength = 2;
        Object elementLow = workArray[low];
        boolean isDescending = compare(elementLow, workArray[low - 1]) < 0;
        Object previousElement = elementLow;
        for (int idx = low + 1; idx < high; ++idx) {
            Object currentElement = workArray[idx];
            int order = compare(currentElement, previousElement);
            if (isDescending ? order >= 0 : order < 0) {
                break;
            }
            previousElement = currentElement;
            ++runLength;
        }
        if (isDescending) {
            reverseRange(lowArg, lowArg + runLength);
        }
        return runLength;
    }

    private void reverseRange(int from, int to) {
        int low = from;
        int high = to - 1;
        while (low < high) {
            Object elementLow = workArray[low];
            workArray[low++] = workArray[high];
            workArray[high--] = elementLow;
        }
    }

    private void mergeAt(int i) {
        int stackSize = pendingRunsSize;
        int baseA = runBase(i);
        int lengthA = runLength(i);
        int baseB = runBase(i + 1);
        int lengthB = runLength(i + 1);

        pendingRuns[(i << 1) + 1] = lengthA + lengthB;
        if (i == stackSize - 3) {
            pendingRuns[(i + 1) << 1] = runBase(i + 2);
            pendingRuns[((i + 1) << 1) + 1] = runLength(i + 2);
        }
        pendingRunsSize = stackSize - 1;

        int k = gallopRight(workArray, workArray[baseB], baseA, lengthA, 0);
        baseA = baseA + k;
        lengthA = lengthA - k;
        if (lengthA == 0) {
            return;
        }
        lengthB = gallopLeft(workArray, workArray[baseA + lengthA - 1], baseB, lengthB, lengthB - 1);
        if (lengthB == 0) {
            return;
        }
        if (lengthA <= lengthB) {
            mergeLow(baseA, lengthA, baseB, lengthB);
        } else {
            mergeHigh(baseA, lengthA, baseB, lengthB);
        }
    }

    private int gallopLeft(Object[] array, Object key, int base, int length, int hint) {
        int lastOfs = 0;
        int offset = 1;
        if (compare(array[base + hint], key) < 0) {
            int maxOfs = length - hint;
            while (offset < maxOfs) {
                if (compare(array[base + hint + offset], key) >= 0) {
                    break;
                }
                lastOfs = offset;
                offset = (offset << 1) + 1;
                if (offset <= 0) {
                    offset = maxOfs;
                }
            }
            if (offset > maxOfs) {
                offset = maxOfs;
            }
            lastOfs = lastOfs + hint;
            offset = offset + hint;
        } else {
            int maxOfs = hint + 1;
            while (offset < maxOfs) {
                if (compare(array[base + hint - offset], key) < 0) {
                    break;
                }
                lastOfs = offset;
                offset = (offset << 1) + 1;
                if (offset <= 0) {
                    offset = maxOfs;
                }
            }
            if (offset > maxOfs) {
                offset = maxOfs;
            }
            int tmp = lastOfs;
            lastOfs = hint - offset;
            offset = hint - tmp;
        }
        lastOfs++;
        while (lastOfs < offset) {
            int m = lastOfs + ((offset - lastOfs) >> 1);
            if (compare(array[base + m], key) < 0) {
                lastOfs = m + 1;
            } else {
                offset = m;
            }
        }
        return offset;
    }

    private int gallopRight(Object[] array, Object key, int base, int length, int hint) {
        int lastOfs = 0;
        int offset = 1;
        if (compare(key, array[base + hint]) < 0) {
            int maxOfs = hint + 1;
            while (offset < maxOfs) {
                if (compare(key, array[base + hint - offset]) >= 0) {
                    break;
                }
                lastOfs = offset;
                offset = (offset << 1) + 1;
                if (offset <= 0) {
                    offset = maxOfs;
                }
            }
            if (offset > maxOfs) {
                offset = maxOfs;
            }
            int tmp = lastOfs;
            lastOfs = hint - offset;
            offset = hint - tmp;
        } else {
            int maxOfs = length - hint;
            while (offset < maxOfs) {
                if (compare(key, array[base + hint + offset]) < 0) {
                    break;
                }
                lastOfs = offset;
                offset = (offset << 1) + 1;
                if (offset <= 0) {
                    offset = maxOfs;
                }
            }
            if (offset > maxOfs) {
                offset = maxOfs;
            }
            lastOfs = lastOfs + hint;
            offset = offset + hint;
        }
        lastOfs++;
        while (lastOfs < offset) {
            int m = lastOfs + ((offset - lastOfs) >> 1);
            if (compare(key, array[base + m]) < 0) {
                offset = m;
            } else {
                lastOfs = m + 1;
            }
        }
        return offset;
    }

    private void mergeLow(int baseA, int lengthAArg, int baseB, int lengthBArg) {
        int lengthA = lengthAArg;
        int lengthB = lengthBArg;
        Object[] tempArray = getTempArray(lengthA);
        System.arraycopy(workArray, baseA, tempArray, 0, lengthA);

        int dest = baseA;
        int cursorTemp = 0;
        int cursorB = baseB;
        workArray[dest++] = workArray[cursorB++];

        boolean copyB;
        merge:
        {
            if (--lengthB == 0) {
                copyB = false;
                break merge;
            }
            if (lengthA == 1) {
                copyB = true;
                break merge;
            }
            int minGallop = this.minGallop;
            while (true) {
                int nofWinsA = 0;
                int nofWinsB = 0;
                while (true) {
                    if (compare(workArray[cursorB], tempArray[cursorTemp]) < 0) {
                        workArray[dest++] = workArray[cursorB++];
                        ++nofWinsB;
                        --lengthB;
                        nofWinsA = 0;
                        if (lengthB == 0) {
                            copyB = false;
                            break merge;
                        }
                        if (nofWinsB >= minGallop) {
                            break;
                        }
                    } else {
                        workArray[dest++] = tempArray[cursorTemp++];
                        ++nofWinsA;
                        --lengthA;
                        nofWinsB = 0;
                        if (lengthA == 1) {
                            copyB = true;
                            break merge;
                        }
                        if (nofWinsA >= minGallop) {
                            break;
                        }
                    }
                }

                ++minGallop;
                boolean firstIteration = true;
                while (nofWinsA >= MIN_GALLOP_WINS || nofWinsB >= MIN_GALLOP_WINS || firstIteration) {
                    firstIteration = false;
                    minGallop = Math.max(1, minGallop - 1);
                    this.minGallop = minGallop;

                    nofWinsA = gallopRight(tempArray, workArray[cursorB], cursorTemp, lengthA, 0);
                    if (nofWinsA > 0) {
                        System.arraycopy(tempArray, cursorTemp, workArray, dest, nofWinsA);
                        dest = dest + nofWinsA;
                        cursorTemp = cursorTemp + nofWinsA;
                        lengthA = lengthA - nofWinsA;
                        if (lengthA == 1) {
                            copyB = true;
                            break merge;
                        }
                        if (lengthA == 0) {
                            copyB = false;
                            break merge;
                        }
                    }
                    workArray[dest++] = workArray[cursorB++];
                    if (--lengthB == 0) {
                        copyB = false;
                        break merge;
                    }

                    nofWinsB = gallopLeft(workArray, tempArray[cursorTemp], cursorB, lengthB, 0);
                    if (nofWinsB > 0) {
                        System.arraycopy(workArray, cursorB, workArray, dest, nofWinsB);
                        dest = dest + nofWinsB;
                        cursorB = cursorB + nofWinsB;
                        lengthB = lengthB - nofWinsB;
                        if (lengthB == 0) {
                            copyB = false;
                            break merge;
                        }
                    }
                    workArray[dest++] = tempArray[cursorTemp++];
                    if (--lengthA == 1) {
                        copyB = true;
                        break merge;
                    }
                }
                ++minGallop;
                this.minGallop = minGallop;
            }
        }
        if (copyB) {
            System.arraycopy(workArray, cursorB, workArray, dest, lengthB);
            workArray[dest + lengthB] = tempArray[cursorTemp];
        } else if (lengthA > 0) {
            System.arraycopy(tempArray, cursorTemp, workArray, dest, lengthA);
        }
    }

    private void mergeHigh(int baseA, int lengthAArg, int baseB, int lengthBArg) {
        int lengthA = lengthAArg;
        int lengthB = lengthBArg;
        Object[] tempArray = getTempArray(lengthB);
        System.arraycopy(workArray, baseB, tempArray, 0, lengthB);

        int dest = baseB + lengthB - 1;
        int cursorTemp = lengthB - 1;
        int cursorA = baseA + lengthA - 1;
        workArray[dest--] = workArray[cursorA--];

        boolean copyA;
        merge:
        {
            if (--lengthA == 0) {
                copyA = false;
                break merge;
            }
            if (lengthB == 1) {
                copyA = true;
                break merge;
            }
            int minGallop = this.minGallop;
            while (true) {
                int nofWinsA = 0;
                int nofWinsB = 0;
                while (true) {
                    if (compare(tempArray[cursorTemp], workArray[cursorA]) < 0) {
                        workArray[dest--] = workArray[cursorA--];
                        ++nofWinsA;
                        --lengthA;
                        nofWinsB = 0;
                        if (lengthA == 0) {
                            copyA = false;
                            break merge;
                        }
                        if (nofWinsA >= minGallop) {
                            break;
                        }
                    } else {
                        workArray[dest--] = tempArray[cursorTemp--];
                        ++nofWinsB;
                        --lengthB;
                        nofWinsA = 0;
                        if (lengthB == 1) {
                            copyA = true;
                            break merge;
                        }
                        if (nofWinsB >= minGallop) {
                            break;
                        }
                    }
                }

                ++minGallop;
                boolean firstIteration = true;
                while (nofWinsA >= MIN_GALLOP_WINS || nofWinsB >= MIN_GALLOP_WINS || firstIteration) {
                    firstIteration = false;
                    minGallop = Math.max(1, minGallop - 1);
                    this.minGallop = minGallop;

                    int k = gallopRight(workArray, tempArray[cursorTemp], baseA, lengthA, lengthA - 1);
                    nofWinsA = lengthA - k;
                    if (nofWinsA > 0) {
                        dest = dest - nofWinsA;
                        cursorA = cursorA - nofWinsA;
                        System.arraycopy(workArray, cursorA + 1, workArray, dest + 1, nofWinsA);
                        lengthA = lengthA - nofWinsA;
                        if (lengthA == 0) {
                            copyA = false;
                            break merge;
                        }
                    }
                    workArray[dest--] = tempArray[cursorTemp--];
                    if (--lengthB == 1) {
                        copyA = true;
                        break merge;
                    }

                    k = gallopLeft(tempArray, workArray[cursorA], 0, lengthB, lengthB - 1);
                    nofWinsB = lengthB - k;
                    if (nofWinsB > 0) {
                        dest = dest - nofWinsB;
                        cursorTemp = cursorTemp - nofWinsB;
                        System.arraycopy(tempArray, cursorTemp + 1, workArray, dest + 1, nofWinsB);
                        lengthB = lengthB - nofWinsB;
                        if (lengthB == 1) {
                            copyA = true;
                            break merge;
                        }
                        if (lengthB == 0) {
                            copyA = false;
                            break merge;
                        }
                    }
                    workArray[dest--] = workArray[cursorA--];
                    if (--lengthA == 0) {
                        copyA = false;
                        break merge;
                    }
                }
                ++minGallop;
                this.minGallop = minGallop;
            }
        }
        if (copyA) {
            dest = dest - lengthA;
            cursorA = cursorA - lengthA;
            System.arraycopy(workArray, cursorA + 1, workArray, dest + 1, lengthA);
            workArray[dest] = tempArray[cursorTemp];
        } else if (lengthB > 0) {
            System.arraycopy(tempArray, 0, workArray, dest - (lengthB - 1), lengthB);
        }
    }

    private static int computeMinRunLength(int nArg) {
        int n = nArg;
        int r = 0;
        while (n >= 64) {
            r = r | (n & 1);
            n = n >> 1;
        }
        return n + r;
    }

    private boolean runInvariantEstablished(int n) {
        if (n < 2) {
            return true;
        }
        return runLength(n - 2) > runLength(n - 1) + runLength(n);
    }

    private void mergeCollapse() {
        while (pendingRunsSize > 1) {
            int n = pendingRunsSize - 2;
            if (!runInvariantEstablished(n + 1) || !runInvariantEstablished(n)) {
                if (runLength(n - 1) < runLength(n + 1)) {
                    --n;
                }
                mergeAt(n);
            } else if (runLength(n) <= runLength(n + 1)) {
                mergeAt(n);
            } else {
                break;
            }
        }
    }

    private void mergeForceCollapse() {
        while (pendingRunsSize > 1) {
            int n = pendingRunsSize - 2;
            if (n > 0 && runLength(n - 1) < runLength(n + 1)) {
                --n;
            }
            mergeAt(n);
        }
    }

    private void arrayTimSortImpl(int length) {
        if (length < 2) {
            return;
        }
        int remaining = length;
        int low = 0;
        int minRunLength = computeMinRunLength(remaining);
        while (remaining != 0) {
            int currentRunLength = countAndMakeRun(low, low + remaining);
            if (currentRunLength < minRunLength) {
                int forcedRunLength = Math.min(minRunLength, remaining);
                binaryInsertionSort(low, low + currentRunLength, low + forcedRunLength);
                currentRunLength = forcedRunLength;
            }
            pushRun(low, currentRunLength);
            mergeCollapse();
            low = low + currentRunLength;
            remaining = remaining - currentRunLength;
        }
        mergeForceCollapse();
    }
}
