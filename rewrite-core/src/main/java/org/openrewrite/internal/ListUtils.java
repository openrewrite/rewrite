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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class ListUtils {
    private ListUtils() {
    }

    public static <T> List<T> mapLast(List<T> ls, Function<T, T> mapLast) {
        if (ls.isEmpty()) {
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

    public static <T> List<T> mapFirst(List<T> ls, Function<T, T> mapFirst) {
        if (ls.isEmpty()) {
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
        if (ls.isEmpty()) {
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

    public static <T> List<T> map(List<T> ls, Function<T, T> map) {
        return map(ls, (i, t) -> map.apply(t));
    }

    public static <T> List<T> map(List<T> ls, ForkJoinPool pool, Function<T, T> map) {
        return map(ls, pool, (i, t) -> map.apply(t));
    }

    /**
     * Apply function to each element of the list. If any element has been modified then
     * a new list will be returned where the modifed elements have been replaced with their new version.
     */
    public static <T> List<T> map(List<T> ls, ForkJoinPool pool, BiFunction<Integer, T, T> map) {
        if (ls.isEmpty()) {
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

    public static <T> List<T> concat(List<T> ls, T t) {
        List<T> newLs = new ArrayList<>(ls);
        newLs.add(t);
        return newLs;
    }

    public static <T> List<T> concat(T t, List<T> ls) {
        List<T> newLs = new ArrayList<>(ls.size() + 1);
        newLs.add(t);
        newLs.addAll(ls);
        return newLs;
    }
    public static <T> List<T> concatAll(List<T> ls, List<T> t) {
        List<T> newLs = new ArrayList<>(ls);
        newLs.addAll(t);
        return newLs;
    }

    public static <T> List<T> insertAll(List<T> ls, int index, List<T> t) {
        List<T> newLs = new ArrayList<>(ls);
        newLs.addAll(index, t);
        return newLs;
    }
}
