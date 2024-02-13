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
import org.openrewrite.marker.RecipesThatMadeChanges;

import java.nio.file.Path;
import java.util.*;
import java.util.function.UnaryOperator;

public class InMemoryLargeSourceSet implements LargeSourceSet {
    /**
     * If null, then the initial state is this instance.
     */
    @Nullable
    private final InMemoryLargeSourceSet initialState;

    private final List<SourceFile> ls;

    @Nullable
    private Map<SourceFile, List<Recipe>> deletions;

    private List<Recipe> currentRecipeStack;

    public InMemoryLargeSourceSet(List<SourceFile> ls) {
        this(null, null, ls);
    }

    protected InMemoryLargeSourceSet(@Nullable InMemoryLargeSourceSet initialState,
                                   @Nullable Map<SourceFile, List<Recipe>> deletions,
                                   List<SourceFile> ls) {
        this.initialState = initialState;
        this.ls = ls;
        this.deletions = deletions;
    }

    protected InMemoryLargeSourceSet withChanges(@Nullable Map<SourceFile, List<Recipe>> deletions, List<SourceFile> mapped) {
        return new InMemoryLargeSourceSet(getInitialState(), deletions, mapped);
    }

    @Override
    public void setRecipe(List<Recipe> recipeStack) {
        this.currentRecipeStack = recipeStack;
    }

    @Override
    public LargeSourceSet edit(UnaryOperator<SourceFile> map) {
        List<SourceFile> mapped = ListUtils.map(ls, before -> {
            SourceFile after = map.apply(before);
            if (after == null) {
                if (deletions == null) {
                    deletions = new LinkedHashMap<>();
                }
                deletions.put(before, currentRecipeStack);
            }
            return after;
        });
        return mapped != ls ? withChanges(deletions, mapped) : this;
    }

    @Override
    public LargeSourceSet generate(@Nullable Collection<? extends SourceFile> t) {
        if (t == null || t.isEmpty()) {
            //noinspection ConstantConditions
            return this;
        } else if (ls.isEmpty()) {
            //noinspection unchecked
            return withChanges(deletions, (List<SourceFile>) t);
        }

        List<SourceFile> newLs = new ArrayList<>(ls);
        newLs.addAll(t);
        return withChanges(deletions, newLs);
    }

    protected InMemoryLargeSourceSet getInitialState() {
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
                } else {
                    Collection<List<Recipe>> recipes = s.getMarkers().findFirst(RecipesThatMadeChanges.class).map(RecipesThatMadeChanges::getRecipes).orElse(Collections.emptyList());
                    changes.add(new Result(null, s, recipes));
                }
            }
        }

        if (deletions != null) {
            for (Map.Entry<SourceFile, List<Recipe>> entry : deletions.entrySet()) {
                changes.add(new Result(entry.getKey(), null, Collections.singleton(entry.getValue())));
            }
        }

        return new InMemoryChangeset(changes);
    }

    @Nullable
    @Override
    public SourceFile getBefore(Path sourcePath) {
        List<SourceFile> sourceFiles = getInitialState().ls;
        for (SourceFile s : sourceFiles) {
            if (s.getSourcePath().equals(sourcePath)) {
                return s;
            }
        }
        return null;
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
            return change.subList(start, Math.min(change.size(), start + count));
        }

        @Override
        public List<Result> getAllResults() {
            return change;
        }
    }
}
