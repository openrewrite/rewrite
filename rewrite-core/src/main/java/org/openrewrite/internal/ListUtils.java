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
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptyList;
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
    public static <T> List<T> insertInOrder(@Nullable List<T> ls, T insert, Comparator<T> naturalOrdering) {
        if (ls == null || ls.isEmpty()) {
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

    /**
     * Insert element to a list at the specified position in the list.
     * Throws the same exceptions as List.add()
     * @param ls The original list.
     * @param t The element to add.
     * @param index index at which the specified element is to be inserted
     * @param <T> The type of elements in the list.
     * @return A new list with the element inserted at the specified position.
     */
    public static <T> List<T> insert(@Nullable List<T> ls, @Nullable T t, int index) {
        //noinspection DuplicatedCode
        if (ls == null && t == null) {
            return emptyList();
        } else if (t == null) {
            return ls;
        }

        List<T> newLs = ls == null ?  new ArrayList<>(1) : new ArrayList<>(ls);
        newLs.add(index, t);
        return newLs;
    }

    public static <T> List<T> mapLast(@Nullable List<T> ls, UnaryOperator<T> mapLast) {
        if (ls == null || ls.isEmpty()) {
            //noinspection ConstantConditions
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

    public static <T> List<T> mapFirst(@Nullable List<T> ls, UnaryOperator<T> mapFirst) {
        if (ls == null || ls.isEmpty()) {
            //noinspection ConstantConditions
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

    public static <T> List<T> map(@Nullable List<T> ls, BiFunction<Integer, T, T> map) {
        if (ls == null || ls.isEmpty()) {
            //noinspection ConstantConditions
            return ls;
        }

        List<T> newLs = ls;
        boolean nullEncountered = false;
        for (int i = 0; i < ls.size(); i++) {
            T tree = ls.get(i);
            T newTree = map.apply(i, tree);
            if (newTree != tree) {
                if (newLs == ls) {
                    newLs = new ArrayList<>(ls);
                }
                newLs.set(i, newTree);
            }
            nullEncountered |= newTree == null;
        }

        if (newLs != ls && nullEncountered) {
            //noinspection StatementWithEmptyBody
            while (newLs.remove(null)) ;
        }

        return newLs;
    }

    // inlined version of `map(List, BiFunction)` for memory efficiency (no overhead for lambda)
    public static <T> List<T> map(@Nullable List<T> ls, UnaryOperator<T> map) {
        if (ls == null || ls.isEmpty()) {
            //noinspection ConstantConditions
            return ls;
        }

        List<T> newLs = ls;
        boolean nullEncountered = false;
        for (int i = 0; i < ls.size(); i++) {
            T tree = ls.get(i);
            T newTree = map.apply(tree);
            if (newTree != tree) {
                if (newLs == ls) {
                    newLs = new ArrayList<>(ls);
                }
                newLs.set(i, newTree);
            }
            nullEncountered |= newTree == null;
        }

        if (newLs != ls && nullEncountered) {
            //noinspection StatementWithEmptyBody
            while (newLs.remove(null)) ;
        }

        return newLs;
    }

    public static <T> List<T> flatMap(@Nullable List<T> ls, BiFunction<Integer, T, Object> flatMap) {
        if (ls == null || ls.isEmpty()) {
            //noinspection ConstantConditions
            return ls;
        }
        List<T> newLs = ls;
        int j = 0;
        for (int i = 0; i < ls.size(); i++, j++) {
            T tree = ls.get(i);
            Object newTreeOrTrees = flatMap.apply(i, tree);
            if (newTreeOrTrees != tree) {
                if (newLs == ls) {
                    newLs = new ArrayList<>(ls);
                }

                if (newTreeOrTrees instanceof Iterable) {
                    boolean addedTree = false;
                    //noinspection unchecked
                    for (T newTree : (Iterable<T>) newTreeOrTrees) {
                        if (j >= newLs.size()) {
                            newLs.add(newTree);
                        } else if (!addedTree) {
                            newLs.set(j, newTree);
                        } else {
                            newLs.add(j, newTree);
                        }
                        addedTree = true;
                        j++;
                    }
                    if (addedTree) {
                        j--;
                    } else {
                        newLs.set(j, null);
                    }
                } else {
                    if (j >= newLs.size()) {
                        //noinspection unchecked
                        newLs.add((T) newTreeOrTrees);
                    } else {
                        //noinspection unchecked
                        newLs.set(j, (T) newTreeOrTrees);
                    }
                }
            }
        }

        if (newLs != ls) {
            boolean identical = newLs.size() == ls.size();
            for (int i = newLs.size() - 1; i >= 0; i--) {
                T newElem = newLs.get(i);
                identical = identical && newElem == ls.get(i);
                if (newElem == null) {
                    newLs.remove(i);
                }
            }
            if (identical) {
                newLs = ls;
            }
        }

        return newLs;
    }

    public static <T> List<T> flatMap(@Nullable List<T> ls, Function<T, Object> flatMap) {
        return flatMap(ls, (i, t) -> flatMap.apply(t));
    }

    public static <T> List<T> concat(@Nullable List<T> ls, @Nullable T t) {
        //noinspection DuplicatedCode
        if (t == null && ls == null) {
            return emptyList();
        } else if (t == null) {
            return ls;
        }
        List<T> newLs = ls == null ? new ArrayList<>(1) : new ArrayList<>(ls);
        newLs.add(t);
        return newLs;
    }

    public static <T> List<T> concat(@Nullable T t, @Nullable List<T> ls) {
        if (t == null && ls == null) {
            //noinspection ConstantConditions
            return null;
        } else if (t == null) {
            return ls;
        }
        List<T> newLs = ls == null ? new ArrayList<>(1) : new ArrayList<>(ls.size() + 1);
        newLs.add(t);
        if (ls != null) {
            newLs.addAll(ls);
        }
        return newLs;
    }

    public static <T> List<T> concatAll(@Nullable List<T> ls, @Nullable List<? extends T> t) {
        if (ls == null && t == null) {
            //noinspection ConstantConditions
            return null;
        } else if (t == null || t.isEmpty()) {
            //noinspection ConstantConditions
            return ls;
        } else if (ls == null || ls.isEmpty()) {
            //noinspection unchecked
            return (List<T>) t;
        }

        List<T> newLs = new ArrayList<>(ls);
        newLs.addAll(t);

        return newLs;
    }

    public static <T> List<T> insertAll(@Nullable List<T> ls, int index, @Nullable List<T> t) {
        if (ls == null && t == null) {
            return emptyList();
        } else if (t == null || t.isEmpty()) {
            //noinspection ConstantConditions
            return ls;
        } else if (ls == null || ls.isEmpty()) {
            return t;
        }

        List<T> newLs = new ArrayList<>(ls);
        newLs.addAll(index, t);

        return newLs;
    }

    @Nullable
    public static <T> List<T> nullIfEmpty(@Nullable List<T> ls) {
        return ls == null || ls.isEmpty() ? null : ls;
    }

    public static <T> T @Nullable [] arrayOrNullIfEmpty(@Nullable List<T> list, T[] array) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.toArray(array);
    }

    public static <T> T @Nullable [] nullIfEmpty(T @Nullable [] list) {
        return list == null || list.length == 0 ? null : list;
    }

}
