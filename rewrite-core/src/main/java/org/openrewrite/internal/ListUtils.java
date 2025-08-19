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

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * Utility class for list transformations and manipulations, providing a variety of methods to modify or
 * extend lists in a functional style while preserving immutability principles.
 * <p>
 * If the transformation does not involve modifying LST elements or broader set of stream operations
 * is needed, the Java Streams API may be more suitable.
 *
 * @implNote Most transformation methods in this class accept endomorphic mapping functions, i.e., functions
 * of the form {@code f: T -> T}. The primary goal of these methods is to produce minimal change in a list’s structure,
 * ensuring unnecessary memory allocations are avoided,
 * See also <a href="https://docs.openrewrite.org/authoring-recipes/recipe-conventions-and-best-practices#recipes-must-not-mutate-lsts">Recipes must not mutate LSTs</a>.
 */
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
     *
     * @param ls    The original list.
     * @param t     The element to add.
     * @param index index at which the specified element is to be inserted
     * @param <T>   The type of elements in the list.
     * @return A new list with the element inserted at the specified position.
     */
    public static <T> List<T> insert(@Nullable List<T> ls, @Nullable T t, int index) {
        //noinspection DuplicatedCode
        if (ls == null && t == null) {
            return emptyList();
        } else if (t == null) {
            return ls;
        }

        List<T> newLs = ls == null ? new ArrayList<>(1) : new ArrayList<>(ls);
        newLs.add(index, t);
        return newLs;
    }

    /**
     * Applies the specified mapping function to the last element of the list.
     * If the resulting element is null, the last element is removed from the list.
     *
     * @param ls      The list to modify.
     * @param mapLast The mapping function to apply to the last element.
     * @param <T>     The type of elements in the list.
     * @return A new list with the modified last element, or the original list if unchanged.
     */
    @Contract("null, _ -> null; !null, _ -> !null")
    public static <T> @Nullable List<T> mapLast(@Nullable List<T> ls, Function<T, @Nullable T> mapLast) {
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

    /**
     * For backwards compatibility; prefer {@link #mapLast(List, Function)}.
     */
    public static <T> @Nullable List<T> mapLast(@Nullable List<T> ls, UnaryOperator<@Nullable T> mapLast) {
        return mapLast(ls, (Function<T, T>) mapLast);
    }

    /**
     * Applies the specified mapping function to the first element of the list.
     * If the resulting element is null, the first element is removed from the list.
     *
     * @param ls       The list to modify.
     * @param mapFirst The mapping function to apply to the first element.
     * @param <T>      The type of elements in the list.
     * @return A new list with the modified first element, or the original list if unchanged.
     */
    @Contract("null, _ -> null; !null, _ -> !null")
    public static <T> @Nullable List<T> mapFirst(@Nullable List<T> ls, Function<T, @Nullable T> mapFirst) {
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

    /**
     * For backwards compatibility; prefer {@link #mapFirst(List, Function)}.
     */
    public static <T> @Nullable List<T> mapFirst(@Nullable List<T> ls, UnaryOperator<T> mapFirst) {
        return mapFirst(ls, (Function<T, T>) mapFirst);
    }

    /**
     * Applies a mapping function to each element in the list along with its index.
     * If any mapped element is null, it will be removed from the resulting list.
     *
     * @param ls  The list to modify.
     * @param map The mapping function that takes an index and an element.
     * @param <T> The type of elements in the list.
     * @return A new list with modified elements, or the original list if unchanged.
     */
    @Contract("null, _ -> null; !null, _ -> !null")
    public static <T> @Nullable List<T> map(@Nullable List<T> ls, BiFunction<Integer, T, @Nullable T> map) {
        if (ls == null || ls.isEmpty()) {
            //noinspection ConstantConditions
            return ls;
        }

        List<@Nullable T> newLs = ls;
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

        //noinspection NullableProblems
        return newLs;
    }

    /**
     * Applies a mapping function to each element in the list. If any mapped element is null, it will be removed.
     *
     * @param ls  The list to modify.
     * @param map The mapping function to apply to each element.
     * @param <T> The type of elements in the list.
     * @return A new list with modified elements, or the original list if unchanged.
     */
    // inlined version of `map(List, BiFunction)` for memory efficiency (no overhead for lambda)
    @Contract("null, _ -> null; !null, _ -> !null")
    public static <T> @Nullable List<T> map(@Nullable List<T> ls, Function<T, @Nullable T> map) {
        if (ls == null || ls.isEmpty()) {
            //noinspection ConstantConditions
            return ls;
        }

        List<@Nullable T> newLs = ls;
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

        //noinspection NullableProblems
        return newLs;
    }

    /**
     * For backwards compatibility; prefer {@link #map(List, Function)}.
     */
    @Contract("null, _ -> null; !null, _ -> !null")
    public static <T> @Nullable List<T> map(@Nullable List<T> ls, UnaryOperator<T> map) {
        return map(ls, (Function<T, T>) map);
    }

    /**
     * Applies a flat-mapping function to each element in the list with its index.
     * Each element may map to a single object or an iterable of objects.
     * If any mapped element is null, it will be removed from the list.
     *
     * @param ls      The list to modify.
     * @param flatMap The flat-mapping function that takes an index and an element.
     * @param <T>     The type of elements in the list.
     * @return A new list with expanded or modified elements, or the original list if unchanged.
     */
    @Contract("null, _ -> null; !null, _ -> !null")
    public static <T> @Nullable List<T> flatMap(@Nullable List<T> ls, BiFunction<Integer, T, @Nullable Object> flatMap) {
        if (ls == null || ls.isEmpty()) {
            //noinspection ConstantConditions
            return ls;
        }
        List<@Nullable T> newLs = ls;
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

        //noinspection NullableProblems
        return newLs;
    }

    /**
     * Applies a flat-mapping function to each element in the list. Each element may map to a single object or an iterable.
     *
     * @param ls      The list to modify.
     * @param flatMap The flat-mapping function to apply to each element.
     * @param <T>     The type of elements in the list.
     * @return A new list with expanded or modified elements, or the original list if unchanged.
     */
    @Contract("null, _ -> null; !null, _ -> !null")
    public static <T> @Nullable List<T> flatMap(@Nullable List<T> ls, Function<T, @Nullable Object> flatMap) {
        return flatMap(ls, (i, t) -> flatMap.apply(t));
    }

    /**
     * Filters a list based on the given predicate. If no elements are removed, the original list is returned.
     *
     * @param ls        The list to filter.
     * @param predicate The predicate to test each element.
     * @param <T>       The type of elements in the list.
     * @return A new list with filtered elements, or the original list if unchanged.
     */
    @Contract("null, _ -> null; !null, _ -> !null")
    public static <T> @Nullable List<T> filter(@Nullable List<T> ls, Predicate<T> predicate) {
        if (ls == null || ls.isEmpty()) {
            //noinspection ConstantConditions
            return ls;
        }

        List<T> newLs = ls;
        int j = 0;
        for (int i = 0; i < ls.size(); i++, j++) {
            T elem = ls.get(i);
            if (!predicate.test(elem)) {
                if (newLs == ls) {
                    newLs = new ArrayList<>(ls);
                }
                newLs.remove(j);
                j--;
            }
        }

        return newLs;
    }

    /**
     * Concatenates a single element to the end of the list. If the element is null, returns the original list.
     *
     * @param ls  The original list.
     * @param t   The element to concatenate.
     * @param <T> The type of elements in the list.
     * @return A new list with the added element, or an empty list if the element or list is null.
     */
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

    /**
     * Concatenates a single element to the beginning of the list. If the element is null, returns the original list.
     *
     * @param t   The element to add.
     * @param ls  The original list.
     * @param <T> The type of elements in the list.
     * @return A new list with the added element at the start, the original list if the element is null or a null
     * object if both element and list are null.
     */
    @Contract("null, null -> null; !null, _ -> !null; _, !null -> !null")
    public static <T> @Nullable List<T> concat(@Nullable T t, @Nullable List<T> ls) {
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

    /**
     * Concatenates two lists. If both are null, returns null. If only one is null, returns the other.
     *
     * @param ls  The original list.
     * @param t   The list to concatenate.
     * @param <T> The type of elements in the lists.
     * @return A new list containing both lists, or one of the lists if the other is null.
     */
    @Contract("null, null -> null; !null, _ -> !null; _, !null -> !null")
    public static <T> @Nullable List<T> concatAll(@Nullable List<T> ls, @Nullable List<? extends T> t) {
        if (ls == null && t == null) {
            return null;
        } else if (t == null || t.isEmpty()) {
            return ls;
        } else if (ls == null || ls.isEmpty()) {
            //noinspection unchecked
            return (List<T>) t;
        }

        List<T> newLs = new ArrayList<>(ls);
        newLs.addAll(t);

        return newLs;
    }

    /**
     * Inserts all elements of a list at the specified position in another list.
     *
     * @param ls    The original list.
     * @param index The position to insert the elements.
     * @param t     The list of elements to insert.
     * @param <T>   The type of elements in the list.
     * @return A new list with the elements inserted, or the original list if `t` is null or empty.
     */
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

    /**
     * Returns null if the list is empty; otherwise, returns the list.
     *
     * @param ls  The list to check.
     * @param <T> The type of elements in the list.
     * @return Null if the list is empty; otherwise, the list.
     */
    public static <T> @Nullable List<T> nullIfEmpty(@Nullable List<T> ls) {
        return ls == null || ls.isEmpty() ? null : ls;
    }

    /**
     * Converts a list to an array if the list is non-empty; otherwise, returns null.
     *
     * @param list  The list to convert.
     * @param array The array type to populate.
     * @param <T>   The type of elements in the list.
     * @return The array representation of the list, or null if the list is empty.
     */
    public static <T> T @Nullable [] arrayOrNullIfEmpty(@Nullable List<T> list, T[] array) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.toArray(array);
    }

    /**
     * Returns null if the array is empty; otherwise, returns the array.
     *
     * @param array The array to check.
     * @param <T>   The type of elements in the array.
     * @return Null if the array is empty; otherwise, the array.
     */
    public static <T> T @Nullable [] nullIfEmpty(T @Nullable [] array) {
        return array == null || array.length == 0 ? null : array;
    }

}
