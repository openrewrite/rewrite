/*
 * Copyright 2023 the original author or authors.
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

import lombok.RequiredArgsConstructor;
import org.openrewrite.LargeIterable;
import org.openrewrite.internal.lang.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;

@RequiredArgsConstructor
public class InMemoryLargeIterable<T> implements LargeIterable<T> {
    private final List<T> ls;

    @Override
    public LargeIterable<T> map(BiFunction<Integer, T, T> map) {
        List<T> mapped = ListUtils.map(ls, map);
        return mapped != ls ? new InMemoryLargeIterable<>(mapped) : this;
    }

    @Override
    public LargeIterable<T> flatMap(BiFunction<Integer, T, Object> flatMap) {
        List<T> mapped = ListUtils.flatMap(ls, flatMap);
        return mapped != ls ? new InMemoryLargeIterable<>(mapped) : this;
    }

    @Override
    public LargeIterable<T> concat(@Nullable T t) {
        List<T> mapped = ListUtils.concat(ls, t);
        return mapped != ls ? new InMemoryLargeIterable<>(mapped) : this;
    }

    @Override
    public LargeIterable<T> concatAll(@Nullable Collection<? extends T> t) {
        if (ls == null && t == null) {
            //noinspection ConstantConditions
            return null;
        } else if (t == null || t.isEmpty()) {
            //noinspection ConstantConditions
            return this;
        } else if (ls == null || ls.isEmpty()) {
            //noinspection unchecked
            return new InMemoryLargeIterable<>((List<T>) t);
        }

        List<T> newLs = new ArrayList<>(ls);
        newLs.addAll(t);
        return new InMemoryLargeIterable<>(newLs);
    }

    @Override
    public Iterator<T> iterator() {
        return ls.iterator();
    }
}
