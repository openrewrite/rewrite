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
package org.openrewrite;

import lombok.Getter;
import org.openrewrite.jgit.lib.FileMode;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.internal.InMemoryDiffEntry;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.RecipesThatMadeChanges;
import org.openrewrite.marker.SearchResult;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class Result {
    /**
     * Possible {@code null} if a new file is being created.
     */
    @Getter
    @Nullable
    private final SourceFile before;

    /**
     * Possibly {@code null} if the change results in the file being deleted.
     */
    @Getter
    @Nullable
    private final SourceFile after;

    @Getter
    private final Collection<List<Recipe>> recipes;

    @Getter
    @Nullable
    private final Duration timeSavings;

    public Result(@Nullable SourceFile before, @Nullable SourceFile after, Collection<List<Recipe>> recipes) {
        this.before = before;
        this.after = after;
        this.recipes = recipes;

        Duration timeSavings = null;
        for (List<Recipe> recipesStack : recipes) {
            if (recipesStack != null && !recipesStack.isEmpty()) {
                Duration perOccurrence = recipesStack.get(recipesStack.size() - 1).getEstimatedEffortPerOccurrence();
                if (perOccurrence != null) {
                    timeSavings = perOccurrence;
                    break;
                }
            }
        }

        this.timeSavings = timeSavings;
    }

    public Result(@Nullable SourceFile before, SourceFile after) {
        this(before, after, after.getMarkers()
                .findFirst(RecipesThatMadeChanges.class)
                .orElseThrow(() -> new IllegalStateException(
                        String.format(
                                "Source file changed but no recipe " +
                                "reported making a change. %s",
                                explainWhatChanged(before, after)
                        )
                ))
                .getRecipes());
    }

    private static String explainWhatChanged(@Nullable SourceFile before, SourceFile after) {
        if (before == null) {
            return String.format("A new file %s was generated but no recipe reported generating it. This is likely a bug in OpenRewrite itself.",
                    after.getSourcePath());
        }
        Map<UUID, Tree> beforeTrees = new HashMap<>();
        new TreeVisitor<Tree, Integer>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, Integer integer) {
                if (tree != null) {
                    beforeTrees.put(tree.getId(), tree);
                }
                return super.visit(tree, integer);
            }
        }.visit(before, 0);

        SourceFile changesMarked = (SourceFile) new TreeVisitor<Tree, Integer>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, Integer p) {
                if (tree != null &&
                    beforeTrees.get(tree.getId()) != tree &&
                    !subtreeChanged(tree, beforeTrees)) {
                    return SearchResult.found(tree);
                }
                return super.visit(tree, p);
            }
        }.visitNonNull(after, 0);

        String diff = diff(before.printAllTrimmed(), changesMarked.printAllTrimmed(), after.getSourcePath());
        return "The following diff highlights the places where unexpected changes were made:\n" +
               Arrays.stream(requireNonNull(diff).split("\n"))
                       .map(l -> "  " + l)
                       .collect(Collectors.joining("\n"));
    }

    private static boolean subtreeChanged(Tree root, Map<UUID, Tree> beforeTrees) {
        return new TreeVisitor<Tree, AtomicBoolean>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, AtomicBoolean changed) {
                if (tree != null && tree != root) {
                    if (beforeTrees.get(tree.getId()) != tree) {
                        changed.set(true);
                    }
                }
                return super.visit(tree, changed);
            }
        }.reduce(root, new AtomicBoolean(false)).get();
    }

    /**
     * Return a list of recipes that have made changes as a hierarchy of descriptors.
     * The method transforms the flat, stack-based representation into descriptors where children are grouped under their common parents.
     */
    public List<RecipeDescriptor> getRecipeDescriptorsThatMadeChanges() {
        List<RecipeDescriptor> recipesToDisplay = new ArrayList<>();

        for (List<Recipe> currentStack : recipes) {
            Recipe root;
            if (currentStack.size() > 1) {
                // The first recipe is typically an Environment.CompositeRecipe and should not be included in the list of RecipeDescriptors
                root = currentStack.get(1);
            } else {
                root = currentStack.get(0);
            }
            RecipeDescriptor rootDescriptor = root.getDescriptor().withRecipeList(new ArrayList<>());

            RecipeDescriptor index;
            if (recipesToDisplay.contains(rootDescriptor)) {
                index = recipesToDisplay.get(recipesToDisplay.indexOf(rootDescriptor));
            } else {
                recipesToDisplay.add(rootDescriptor);
                index = rootDescriptor;
            }

            for (int i = 2; i < currentStack.size(); i++) {
                RecipeDescriptor nextDescriptor = currentStack.get(i).getDescriptor().withRecipeList(new ArrayList<>());
                if (index.getRecipeList().contains(nextDescriptor)) {
                    index = index.getRecipeList().get(index.getRecipeList().indexOf(nextDescriptor));
                } else {
                    index.getRecipeList().add(nextDescriptor);
                    index = nextDescriptor;
                }
            }
        }
        return recipesToDisplay;
    }

    /**
     * @return Git-style patch diff representing the changes to this compilation unit.
     */
    public String diff() {
        return diff(null);
    }

    /**
     * @param relativeTo Optional relative path that is used to relativize file paths of reported differences.
     * @return Git-style patch diff representing the changes to this compilation unit.
     */
    public String diff(@Nullable Path relativeTo) {
        return diff(relativeTo, null);
    }

    public String diff(@Nullable Path relativeTo, @Nullable PrintOutputCapture.MarkerPrinter markerPrinter) {
        return diff(relativeTo, markerPrinter, false);
    }

    @Incubating(since = "7.34.0")
    public String diff(@Nullable Path relativeTo, @Nullable PrintOutputCapture.MarkerPrinter markerPrinter, @Nullable Boolean ignoreAllWhitespace) {
        Path beforePath = before == null ? null : before.getSourcePath();
        Path afterPath = null;
        if (before == null && after == null) {
            afterPath = (relativeTo == null ? Paths.get(".") : relativeTo).resolve("partial-" + System.nanoTime());
        } else if (after != null) {
            afterPath = after.getSourcePath();
        }

        PrintOutputCapture<Integer> out = markerPrinter == null ?
                new PrintOutputCapture<>(0) :
                new PrintOutputCapture<>(0, markerPrinter);

        FileMode beforeMode = before != null && before.getFileAttributes() != null && before.getFileAttributes().isExecutable() ? FileMode.EXECUTABLE_FILE : FileMode.REGULAR_FILE;
        FileMode afterMode = after != null && after.getFileAttributes() != null && after.getFileAttributes().isExecutable() ? FileMode.EXECUTABLE_FILE : FileMode.REGULAR_FILE;

        Set<Recipe> recipeSet = new HashSet<>(recipes.size());
        for (List<Recipe> rs : recipes) {
            if (!rs.isEmpty()) {
                recipeSet.add(rs.get(0));
            }
        }

        try (InMemoryDiffEntry diffEntry = new InMemoryDiffEntry(
                beforePath,
                afterPath,
                relativeTo,
                before == null ? "" : before.printAll(out),
                after == null ? "" : after.printAll(out.clone()),
                recipeSet,
                beforeMode,
                afterMode
        )) {
            return diffEntry.getDiff(ignoreAllWhitespace);
        }
    }

    @Nullable
    public static String diff(String before, String after, Path path) {
        String diff = null;
        try (InMemoryDiffEntry diffEntry = new InMemoryDiffEntry(
                path,
                path,
                null,
                before,
                after,
                Collections.emptySet(),
                FileMode.REGULAR_FILE,
                FileMode.REGULAR_FILE
        )) {
            diff = diffEntry.getDiff(Boolean.FALSE);
        } catch (Exception ignored) {
        }
        return diff;
    }

    @Override
    public String toString() {
        return diff();
    }
}
