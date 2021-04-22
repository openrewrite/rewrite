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
package org.openrewrite.internal;

import org.openrewrite.internal.lang.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;

public final class ListUtils {
    private ListUtils() {
    }

    /**
     * Insert into as-near of a location representing the natural ordering of the list without assuming
     * the list is already sorted according to its natural ordering AND without changing the position of any
     * other element. This provides a means of both inserting in a natural order but also making the least invasive
     * change.
     *
     * @param ls              The original list, which may not be ordered.
     * @param insert          The element to add.
     * @param naturalOrdering The natural or idiomatic ordering of the list.
     * @param <T>             The type of elements in the list.
     * @return A new list with the element inserted in an approximately ordered place.
     */
    public static <T> List<T> insertInOrder(List<T> ls, T insert, Comparator<T> naturalOrdering) {
        if(ls == null || ls.isEmpty()) {
            return singletonList(insert);
        }

        List<T> ordered = new ArrayList<>(ls);
        ordered.add(insert);
        ordered.sort(naturalOrdering);

        T comesAfter = null;
        for (T t : ordered) {
            if (t == insert) {
                break;
            }
            comesAfter = t;
        }

        List<T> newLs = new ArrayList<>(ls);
        if (comesAfter == null) {
            newLs.add(0, insert);
        } else {
            for (int i = 0; i < newLs.size(); i++) {
                if (newLs.get(i) == comesAfter) {
                    newLs.add(i + 1, insert);
                }
            }
        }

        return newLs;
    }

    public static <T> List<T> mapLast(List<T> ls, UnaryOperator<T> mapLast) {
        if (ls == null || ls.isEmpty()) {
            return ls;
        }
        T last = ls.get(ls.size() - 1);
        T newLast = mapLast.apply(last);
        if (last != newLast) {
            List<T> newLs = new ArrayList<>(ls);
            if (newLast == null) {
                newLs.remove(ls.size() - 1);
            } else {
                newLs.set(ls.size() - 1, newLast);
            }
            return newLs;
        }
        return ls;
    }

    public static <T> List<T> mapFirst(List<T> ls, UnaryOperator<T> mapFirst) {
        if (ls == null || ls.isEmpty()) {
            return ls;
        }
        T first = ls.iterator().next();
        T newFirst = mapFirst.apply(first);
        if (first != newFirst) {
            List<T> newLs = new ArrayList<>(ls);
            if (newFirst == null) {
                newLs.remove(0);
            } else {
                newLs.set(0, newFirst);
            }
            return newLs;
        }
        return ls;
    }

    public static <T> List<T> map(List<T> ls, BiFunction<Integer, T, T> map) {
        if (ls == null || ls.isEmpty()) {
            return ls;
        }
        List<T> newLs = ls;
        for (int i = 0; i < ls.size(); i++) {
            T tree = ls.get(i);
            T newTree = map.apply(i, tree);
            if (newTree != tree) {
                if (newLs == ls) {
                    newLs = new ArrayList<>(ls);
                }
                newLs.set(i, newTree);
            }
        }

        if (newLs != ls) {
            //noinspection StatementWithEmptyBody
            while (newLs.remove(null)) ;
        }

        return newLs;
    }

    public static <T> List<T> map(List<T> ls, UnaryOperator<T> map) {
        return map(ls, (i, t) -> map.apply(t));
    }

    public static <T> List<T> map(List<T> ls, ForkJoinPool pool, UnaryOperator<T> map) {
        return map(ls, pool, (i, t) -> map.apply(t));
    }

    /**
     * Apply function to each element of the list. If any element has been modified then
     * a new list will be returned where the modified elements have been replaced with their new version.
     *
     * @param ls   The original list
     * @param pool A pool to parallelize the mapping operation
     * @param map  The mapping function. If a null value is returned, the item is dropped from the resultant list
     * @param <T>  The type of the list
     * @return The original list if no element has changed, or a new list.
     */
    public static <T> List<T> map(List<T> ls, ForkJoinPool pool, BiFunction<Integer, T, T> map) {
        if (ls == null || ls.isEmpty()) {
            return ls;
        }

        AtomicReference<List<T>> newLs = new AtomicReference<>(ls);
        for (int i = 0; i < ls.size(); i++) {
            int index = i;

            ForkJoinTask<?> task = ForkJoinTask.adapt(() -> {
                T tree = ls.get(index);
                T newTree = map.apply(index, tree);
                if (newTree != tree) {
                    newLs.updateAndGet(l -> l == ls ? new ArrayList<>(ls) : l)
                            .set(index, newTree);
                }
            });

            pool.invoke(task);
            task.join();
        }

        if (newLs.get() != ls) {
            //noinspection StatementWithEmptyBody
            while (newLs.get().remove(null)) ;
        }

        return newLs.get();
    }

    public static <T> List<T> concat(@Nullable List<T> ls, T t) {
        List<T> newLs = ls == null ? new ArrayList<>(1) : new ArrayList<>(ls);
        newLs.add(t);
        return newLs;
    }

    public static <T> List<T> concat(T t, @Nullable List<T> ls) {
        List<T> newLs = ls == null ? new ArrayList<>(1) : new ArrayList<>(ls.size() + 1);
        newLs.add(t);
        if (ls != null) {
            newLs.addAll(ls);
        }
        return newLs;
    }

    public static <T> List<T> concatAll(@Nullable List<T> ls, List<T> t) {
        if (ls == null) {
            return t;
        }
        List<T> newLs = new ArrayList<>(ls);
        newLs.addAll(t);
        return newLs;
    }

    public static <T> List<T> insertAll(List<T> ls, int index, List<T> t) {
        if(ls == null) {
            return t;
        }
        List<T> newLs = new ArrayList<>(ls);
        newLs.addAll(index, t);
        return newLs;
    }
}
