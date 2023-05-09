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
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Generated;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

public class InMemorySourceSet implements SourceSet {
    /**
     * If null, then the initial state is this instance.
     */
    @Nullable
    private final InMemorySourceSet initialState;

    private final List<SourceFile> ls;

    @Nullable
    private Map<SourceFile, List<Recipe>> deletions;

    public InMemorySourceSet(List<SourceFile> ls) {
        this(null, null, ls);
    }

    private InMemorySourceSet(@Nullable InMemorySourceSet initialState,
                              @Nullable Map<SourceFile, List<Recipe>> deletions,
                              List<SourceFile> ls) {
        this.initialState = initialState;
        this.ls = ls;
        this.deletions = deletions;
    }

    @Override
    public SourceSet map(UnaryOperator<SourceFile> map) {
        List<SourceFile> mapped = ListUtils.map(ls, map);
        return mapped != ls ? new InMemorySourceSet(initialState, deletions, mapped) : this;
    }

    @Override
    public SourceSet flatMap(BiFunction<Integer, SourceFile, Object> flatMap) {
        List<SourceFile> mapped = ListUtils.flatMap(ls, flatMap);
        return mapped != ls ? new InMemorySourceSet(initialState, deletions, mapped) : this;
    }

    @Override
    public SourceSet concat(@Nullable SourceFile sourceFile) {
        List<SourceFile> mapped = ListUtils.concat(ls, sourceFile);
        return mapped != ls ? new InMemorySourceSet(initialState, deletions, mapped) : this;
    }

    @Override
    public SourceSet concatAll(@Nullable Collection<? extends SourceFile> t) {
        if (ls == null && t == null) {
            //noinspection ConstantConditions
            return null;
        } else if (t == null || t.isEmpty()) {
            //noinspection ConstantConditions
            return this;
        } else if (ls == null || ls.isEmpty()) {
            //noinspection unchecked
            return new InMemorySourceSet(initialState, deletions, (List<SourceFile>) t);
        }

        List<SourceFile> newLs = new ArrayList<>(ls);
        newLs.addAll(t);
        return new InMemorySourceSet(initialState, deletions, newLs);
    }

    @Override
    public Iterator<SourceFile> iterator() {
        return ls.iterator();
    }

    @Override
    public InMemorySourceSet getInitialState() {
        return initialState == null ? this : initialState;
    }

    @Override
    public Changeset getChangeset() {
        Map<UUID, SourceFile> sourceFileIdentities = new HashMap<>();
        for (SourceFile sourceFile : getInitialState().ls) {
            sourceFileIdentities.put(sourceFile.getId(), sourceFile);
        }

        List<Result> changes = new ArrayList<>();

        // added or changed files
        for (SourceFile s : ls) {
            SourceFile original = sourceFileIdentities.get(s.getId());
            if (original != s) {
                if (original != null) {
                    if (original.getMarkers().findFirst(Generated.class).isPresent()) {
                        continue;
                    }
                    changes.add(new Result(original, s));
                }
            }
        }

        return new InMemoryChangeset(changes);
    }

    @Override
    public void onDelete(SourceFile sourceFile, List<Recipe> recipeStack) {
        if (deletions == null) {
            deletions = new LinkedHashMap<>();
        }
        deletions.put(sourceFile, recipeStack);
    }

    @RequiredArgsConstructor
    private static class InMemoryChangeset implements Changeset {
        final List<Result> change;

        @Override
        public int size() {
            return change.size();
        }

        @Override
        public List<Result> getPage(int start, int count) {
            return change.subList(start, start + count);
        }

        @Override
        public List<Result> getAllResults() {
            return change;
        }
    }
}
