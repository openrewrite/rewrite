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
import java.util.function.BiFunction;
import java.util.function.Function;

public final class ListUtils {
    private ListUtils() {
    }

    public static <T> List<T> mapLast(List<T> ls, Function<T, T> mapLast) {
        if(ls.isEmpty()) {
            return ls;
        }
        T last = ls.get(ls.size() - 1);
        T newLast = mapLast.apply(last);
        if (last != newLast) {
            List<T> newLs = new ArrayList<>(ls);
            newLs.set(ls.size() - 1, newLast);
            return newLs;
        }
        return ls;
    }

    public static <T> List<T> mapFirst(List<T> ls, Function<T, T> mapFirst) {
        if(ls.isEmpty()) {
            return ls;
        }
        T first = ls.iterator().next();
        T newFirst = mapFirst.apply(first);
        if (first != newFirst) {
            List<T> newLs = new ArrayList<>(ls);
            newLs.set(0, newFirst);
            return newLs;
        }
        return ls;
    }

    public static <T> List<T> map(List<T> ls, BiFunction<Integer, T, T> map) {
        if(ls.isEmpty()) {
            return ls;
        }
        List<T> newLs = ls;
        for (int i = 0; i < ls.size(); i++) {
            T tree = ls.get(i);
            T newTree = map.apply(i, tree);
            if(newTree != tree) {
                if (newLs == ls) {
                    newLs = new ArrayList<>(ls);
                }
                newLs.set(i, newTree);
            }
        }
        return newLs;
    }
}
