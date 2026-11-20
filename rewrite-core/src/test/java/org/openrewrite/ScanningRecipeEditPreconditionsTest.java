/*
 * Copyright 2026 the original author or authors.
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

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.config.RecipePreconditions;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.RecipesThatMadeChanges;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.quark.Quark;
import org.openrewrite.search.RepositoryContainsFile;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.Find;
import org.openrewrite.text.FindAndReplace;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.test.SourceSpecs.text;

class ScanningRecipeEditPreconditionsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.validateRecipeSerialization(false);
    }

    @DocumentExample
    @Test
    void gatesOwnEditorAndEveryDescendant() {
        Contains go = new Contains("go");
        rewriteRun(
          spec -> spec.recipe(new Guarded(singletonList(go), "own",
            new Tag("a"),
            new CompositeRecipe(List.of(new Tag("b"), new Tag("c"))),
            new Guarded(emptyList(), "nested", new Tag("d")))),
          text("go", "go[own][a][b][c][nested][d]"),
          text("stop")
        );
    }

    @Test
    void nestedDeclarationAddsItsOwnCondition() {
        rewriteRun(
          spec -> spec.recipe(new Guarded(singletonList(new Contains("x")), null,
            new Guarded(singletonList(new Contains("y")), null, new Tag("inner")),
            new Tag("outer"))),
          text("xy", "xy[inner][outer]"),
          text("x", "x[outer]"),
          text("y"),
          text("z")
        );
    }

    @Test
    void evaluatesOncePerFilePerCycle() {
        // given
        Contains go = new Contains("go");
        Guarded recipe = new Guarded(singletonList(go), "own",
          new Tag("a"), new Tag("b"), new Guarded(emptyList(), null, new Tag("c"), new Tag("d")));

        // when
        RecipeRun run = recipe.run(new InMemoryLargeSourceSet(List.of(
          plainText("go", "a.txt"), plainText("stop", "b.txt"), plainText("go", "c.txt"))), new InMemoryExecutionContext(), 2, 2);

        // then
        assertThat(texts(run)).containsExactly("go[own][a][b][c][d]", "go[own][a][b][c][d]");
        assertThat(go.evaluations).hasValue(3 * 2);
    }

    @Test
    void sourceFileRejectedByTheOwnEditorDoesNotReuseThePreviousResult() {
        // given
        Recipe recipe = new Guarded(singletonList(new Contains("go")), "own", new MarkEverySourceFile());

        // when
        RecipeRun run = recipe.run(new InMemoryLargeSourceSet(List.of(
          plainText("go", "a.txt"),
          new Quark(Tree.randomId(), Paths.get("b.bin"), Markers.EMPTY, null, null))), new InMemoryExecutionContext());

        // then
        assertThat(run.getChangeset().getAllResults())
          .extracting(r -> requireNonNull(r.getAfter()).getSourcePath().toString())
          .containsExactly("a.txt");
    }

    @Test
    void sourceFileRejectedByTheConditionDoesNotMatch() {
        rewriteRun(
          spec -> spec.recipe(new Guarded(singletonList(new ContainsInTxt("go")), null, new Tag("a"))),
          text("go", "go[a]", s -> s.path("a.txt")),
          text("go", s -> s.path("b.md"))
        );
    }

    @Test
    void conditionSeesAnEarlierSiblingsEditAndKeepsItsResultForLaterEditors() {
        rewriteRun(
          spec -> spec.recipe(new CompositeRecipe(List.of(
            new Replace("red", "green"),
            new Guarded(singletonList(new Contains("green")), null,
              new Replace("green", "blue"),
              new Tag("a"),
              new Tag("b"))))),
          text("red", "blue[a][b]"),
          text("gray")
        );
    }

    @Test
    void ownEditorRemovingTheMatchKeepsTheResultForDescendants() {
        rewriteRun(
          spec -> spec.recipe(new Guarded(singletonList(new Contains("green")), null, new Tag("a")) {
              @Override
              public TreeVisitor<?, ExecutionContext> getVisitor(Set<Path> acc) {
                  return new Replace("green", "blue").getVisitor();
              }
          }),
          text("green", "blue[a]")
        );
    }

    @Test
    void conditionsInTheListAreAllRequired() {
        rewriteRun(
          spec -> spec.recipe(new Guarded(List.of(new Contains("x"), new Contains("y")), null, new Tag("a"))),
          text("xy", "xy[a]"),
          text("x"),
          text("y")
        );
    }

    @Test
    void conditionWithARecipeListMatchesWhenAnyOfItMatches() {
        rewriteRun(
          spec -> spec.recipe(new Guarded(singletonList(new CompositeRecipe(List.of(new Contains("x"), new Contains("y")))),
            null, new Tag("a"))),
          text("x", "x[a]"),
          text("y", "y[a]"),
          text("z")
        );
    }

    @Test
    void negatedCondition() {
        rewriteRun(
          spec -> spec.recipe(new Guarded(singletonList(toRecipe(() -> Preconditions.not(new Contains("x").getVisitor()))),
            null, new Tag("a"))),
          text("x"),
          text("y", "y[a]")
        );
    }

    @Test
    void conditionThatIsItselfGuarded() {
        rewriteRun(
          spec -> spec.recipe(new Guarded(singletonList(new Guarded(singletonList(new Contains("x")), null, new Contains("y"))),
            null, new Tag("a"))),
          text("xy", "xy[a]"),
          text("y"),
          text("x")
        );
    }

    @Test
    void sameFixtureAsDeclarativeRecipe() {
        String yaml = """
          type: specs.openrewrite.org/v1beta/recipe
          name: org.openrewrite.Gated
          displayName: Gated
          description: Gated.
          preconditions:
            - org.openrewrite.text.Find:
                find: x
            - org.openrewrite.text.Find:
                find: y
          recipeList:
            - org.openrewrite.text.FindAndReplace:
                find: x
                replace: z
            - org.openrewrite.text.FindAndReplace:
                find: y
                replace: w
          """;
        Recipe java = new Guarded(List.of(find("x"), find("y")), null,
          new FindAndReplace("x", "z", null, null, null, null, null, null),
          new FindAndReplace("y", "w", null, null, null, null, null, null));

        for (Consumer<RecipeSpec> recipe : List.<Consumer<RecipeSpec>>of(
          spec -> spec.recipeFromYaml(yaml, "org.openrewrite.Gated"), spec -> spec.recipe(java))) {
            rewriteRun(
              recipe,
              text("xy", "zw"),
              text("x"),
              text("y")
            );
        }
    }

    @Test
    void scanningConditionSeesEveryFileBeforeTheFirstEdit() {
        rewriteRun(
          spec -> spec.recipe(new Guarded(singletonList(new RepositoryContainsFile("z-marker.txt")), "own=%d",
            new CountScanned("go"))),
          text("go", "go[own=2][go=1]", s -> s.path("a.txt")),
          text("marker", "marker[own=2][go=1]", s -> s.path("z-marker.txt"))
        );
    }

    @Test
    void recipeListCreatedOnEachCallKeepsItsAccumulators() {
        rewriteRun(
          spec -> spec.recipe(new Guarded(singletonList(new Contains("go")), null, new FreshRecipeList())),
          text("go", "go[go=1]"),
          text("stop")
        );
    }

    @Test
    void scanningConditionNotMet() {
        rewriteRun(
          spec -> spec.recipe(new Guarded(singletonList(new RepositoryContainsFile("z-marker.txt")), "own=%d",
            new CountScanned("go"))),
          text("go", s -> s.path("a.txt"))
        );
    }

    @Test
    void conditionsOfOneClassKeepTheirOwnAccumulators() {
        rewriteRun(
          spec -> spec.recipe(new CompositeRecipe(List.of(
            new Guarded(singletonList(new RepositoryContainsFile("a.marker")), null, new Tag("a")),
            new Guarded(singletonList(new RepositoryContainsFile("b.marker")), null, new Tag("b")),
            new Guarded(List.of(new RepositoryContainsFile("a.marker"), new RepositoryContainsFile("b.marker")), null,
              new Tag("both"))))),
          text("t", "t[a]", s -> s.path("t.txt")),
          text("m", "m[a]", s -> s.path("a.marker"))
        );
    }

    @Test
    void repeatedRunsOfTheSameRecipeDoNotShareConditionState() {
        // given
        Recipe recipe = new Guarded(singletonList(new RepositoryContainsFile("marker.txt")), null, new Tag("a"));
        ExecutionContext ctx = new InMemoryExecutionContext();

        // when
        RecipeRun withMarker = recipe.run(new InMemoryLargeSourceSet(List.of(
          plainText("t", "t.txt"), plainText("m", "marker.txt"))), ctx);
        RecipeRun withoutMarker = recipe.run(new InMemoryLargeSourceSet(List.of(plainText("t", "t.txt"))), ctx);
        RecipeRun withMarkerAgain = recipe.run(new InMemoryLargeSourceSet(List.of(
          plainText("t", "t.txt"), plainText("m", "marker.txt"))), ctx);

        // then
        assertThat(texts(withMarker)).containsExactly("t[a]", "m[a]");
        assertThat(texts(withoutMarker)).isEmpty();
        assertThat(texts(withMarkerAgain)).containsExactly("t[a]", "m[a]");
    }

    @Test
    void rejectedConditionStillScansAndGenerates() {
        rewriteRun(
          spec -> spec.recipe(new Guarded(singletonList(new Contains("never")), "own",
            new GenerateWithTwoArguments("two.txt", "go"),
            new GenerateWithThreeArguments("three.txt", "go"),
            new Tag("a")).generating("own.txt", "go")),
          text("go", s -> s.path("a.txt")),
          text(null, "go", s -> s.path("own.txt")),
          text(null, "go", s -> s.path("two.txt")),
          text(null, "go", s -> s.path("three.txt"))
        );
    }

    @Test
    void generatedSourcesAreEditedUnderTheSameCondition() {
        rewriteRun(
          spec -> spec.recipe(new Guarded(singletonList(new Contains("go")), "own",
            new GenerateWithTwoArguments("two.txt", "go"),
            new GenerateWithThreeArguments("three.txt", "stop"),
            new Tag("a")).generating("own.txt", "go")),
          text("stop", s -> s.path("a.txt")),
          text(null, "go[own][a]", s -> s.path("own.txt")),
          text(null, "go[own][a]", s -> s.path("two.txt")),
          text(null, "stop", s -> s.path("three.txt"))
        );
    }

    @Test
    void conditionIsEvaluatedAgainInEveryCycle() {
        // given
        Contains a = new Contains("a");
        Recipe recipe = new Guarded(singletonList(a), null, new AnotherCycle());

        // when
        RecipeRun run = recipe.run(new InMemoryLargeSourceSet(List.of(
          plainText("aaa", "a.txt"))), new InMemoryExecutionContext(), 3, 1);

        // then
        assertThat(texts(run)).containsExactly("bbb");
        assertThat(a.evaluations).hasValue(3);
    }

    @Test
    void noDeclarationBehavesAsBefore() {
        rewriteRun(
          spec -> spec.recipe(new Guarded(emptyList(), "own", new Tag("a"))),
          text("go", "go[own][a]"),
          text("stop", "stop[own][a]")
        );
    }

    @Test
    void informationalPreconditionsStayInformational() {
        Recipe recipe = new InformationalPreconditions(singletonList(new Contains("never")), new Tag("a"));
        rewriteRun(
          spec -> spec.recipe(recipe),
          text("go", "go[a]")
        );
        assertThat(recipe.getDescriptor().getPreconditions())
          .extracting(RecipeDescriptor::getName)
          .containsExactly(Contains.class.getName());
    }

    @Test
    void descriptorListsTheConditions() {
        // given
        Contains go = new Contains("go");
        Guarded recipe = new Guarded(singletonList(go), null, new Tag("a"));

        // when
        RecipeDescriptor descriptor = recipe.getDescriptor();

        // then
        assertThat(descriptor.getPreconditions()).extracting(RecipeDescriptor::getName)
          .containsExactly(Contains.class.getName());
        assertThat(descriptor.getRecipeList()).extracting(RecipeDescriptor::getName)
          .containsExactly(Tag.class.getName());
        assertThat(go.evaluations).hasValue(0);
    }

    @Test
    void changesAreAttributedToTheDeclaringRecipeAndItsDescendants() {
        // given
        Guarded recipe = new Guarded(singletonList(new Contains("go")), null, new Tag("a"));

        // when
        RecipeRun run = recipe.run(new InMemoryLargeSourceSet(List.of(plainText("go", "a.txt"))), new InMemoryExecutionContext());

        // then
        PlainText after = (PlainText) requireNonNull(run.getChangeset().getAllResults().get(0).getAfter());
        assertThat(after.getMarkers().findFirst(RecipesThatMadeChanges.class).orElseThrow().getRecipes())
          .singleElement()
          .satisfies(stack -> assertThat(stack).extracting(Recipe::getName)
            .containsExactly(Guarded.class.getName(), Tag.class.getName()));
        assertThat(run.getChangeset().getAllResults().get(0).getRecipeDescriptorsThatMadeChanges())
          .extracting(RecipeDescriptor::getName)
          .containsExactly(Tag.class.getName());
    }

    @Test
    void conditionsWriteNoDataTableRows() {
        rewriteRun(
          spec -> spec.recipe(new Guarded(singletonList(new RecordWord("condition")), null, new RecordWord("edit")))
            .dataTable(Words.Row.class, rows -> assertThat(rows).extracting(Words.Row::getWord).contains("edit").doesNotContain("condition")),
          text("go", "go[edit]")
        );
    }

    @Test
    void validatesTheConditions() {
        // given
        Recipe recipe = new Guarded(singletonList(new FindAndReplace(null, "b", null, null, null, null, null, null)), null);

        // when
        Collection<Validated<Object>> validated = recipe.validateAll();

        // then
        assertThat(validated).anyMatch(Validated::isInvalid);
    }

    private static Find find(String find) {
        return new Find(find, null, null, null, null, null, null, null);
    }

    private static PlainText plainText(String text, String path) {
        return PlainText.builder().sourcePath(Paths.get(path)).text(text).build();
    }

    private static List<String> texts(RecipeRun run) {
        return run.getChangeset().getAllResults().stream()
          .map(r -> ((PlainText) requireNonNull(r.getAfter())).getText())
          .toList();
    }

    static class Guarded extends ScanningRecipe<Set<Path>> {
        final List<Recipe> conditions;

        @Nullable
        final String ownTag;

        final List<Recipe> children;

        @Nullable
        String generatePath;

        @Nullable
        String generateText;

        Guarded(List<Recipe> conditions, @Nullable String ownTag, Recipe... children) {
            this.conditions = conditions;
            this.ownTag = ownTag;
            this.children = List.of(children);
        }

        Guarded generating(String path, String text) {
            this.generatePath = path;
            this.generateText = text;
            return this;
        }

        @Override
        public String getDisplayName() {
            return "Guarded";
        }

        @Override
        public String getDescription() {
            return "Edits only the source files that meet its conditions.";
        }

        @Override
        public List<Recipe> getEditPreconditions() {
            return conditions;
        }

        @Override
        public List<Recipe> getRecipeList() {
            return children;
        }

        @Override
        public Set<Path> getInitialValue(ExecutionContext ctx) {
            return new LinkedHashSet<>();
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getScanner(Set<Path> acc) {
            return new TreeVisitor<>() {
                @Override
                public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                    if (tree instanceof SourceFile) {
                        acc.add(((SourceFile) tree).getSourcePath());
                    }
                    return tree;
                }
            };
        }

        @Override
        public Collection<? extends SourceFile> generate(Set<Path> acc, Collection<SourceFile> generatedInThisCycle, ExecutionContext ctx) {
            if (generatePath == null || acc.contains(Paths.get(generatePath))) {
                return emptyList();
            }
            return singletonList(plainText(requireNonNull(generateText), generatePath));
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor(Set<Path> acc) {
            return ownTag == null ? TreeVisitor.noop() : new Tag(String.format(ownTag, acc.size())).getVisitor();
        }
    }

    static class InformationalPreconditions extends ScanningRecipe<AtomicBoolean> implements RecipePreconditions {
        final List<Recipe> preconditions;
        final List<Recipe> children;

        InformationalPreconditions(List<Recipe> preconditions, Recipe... children) {
            this.preconditions = preconditions;
            this.children = List.of(children);
        }

        @Override
        public String getDisplayName() {
            return "Informational preconditions";
        }

        @Override
        public String getDescription() {
            return "Lists preconditions without asking for them to be enforced.";
        }

        @Override
        public List<Recipe> getPreconditions() {
            return preconditions;
        }

        @Override
        public List<Recipe> getRecipeList() {
            return children;
        }

        @Override
        public AtomicBoolean getInitialValue(ExecutionContext ctx) {
            return new AtomicBoolean();
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getScanner(AtomicBoolean acc) {
            return TreeVisitor.noop();
        }
    }

    static class Contains extends Recipe {
        final String needle;
        final AtomicInteger evaluations = new AtomicInteger();

        Contains(String needle) {
            this.needle = needle;
        }

        @Override
        public String getDisplayName() {
            return "Contains";
        }

        @Override
        public String getDescription() {
            return "Finds text that contains a string.";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new PlainTextVisitor<>() {
                @Override
                public PlainText visitText(PlainText text, ExecutionContext ctx) {
                    evaluations.incrementAndGet();
                    return text.getText().contains(needle) ? SearchResult.found(text) : text;
                }
            };
        }
    }

    static class ContainsInTxt extends Contains {
        ContainsInTxt(String needle) {
            super(needle);
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            TreeVisitor<?, ExecutionContext> contains = super.getVisitor();
            return new TreeVisitor<>() {
                @Override
                public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                    return sourceFile.getSourcePath().toString().endsWith(".txt");
                }

                @Override
                public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                    return contains.visit(tree, ctx);
                }
            };
        }
    }

    static class Tag extends Recipe {
        final String tag;

        Tag(String tag) {
            this.tag = "[" + tag + "]";
        }

        @Override
        public String getDisplayName() {
            return "Tag";
        }

        @Override
        public String getDescription() {
            return "Appends a tag once.";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new PlainTextVisitor<>() {
                @Override
                public PlainText visitText(PlainText text, ExecutionContext ctx) {
                    return text.getText().contains(tag) ? text : text.withText(text.getText() + tag);
                }
            };
        }
    }

    static class Replace extends Recipe {
        final String from;
        final String to;

        Replace(String from, String to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public String getDisplayName() {
            return "Replace";
        }

        @Override
        public String getDescription() {
            return "Replaces the first occurrence of a string.";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new PlainTextVisitor<>() {
                @Override
                public PlainText visitText(PlainText text, ExecutionContext ctx) {
                    return text.withText(text.getText().replaceFirst(from, to));
                }
            };
        }
    }

    static class AnotherCycle extends Recipe {
        @Override
        public String getDisplayName() {
            return "Another cycle";
        }

        @Override
        public String getDescription() {
            return "Asks for another cycle when its recipe list makes a change.";
        }

        @Override
        public boolean causesAnotherCycle() {
            return true;
        }

        @Override
        public List<Recipe> getRecipeList() {
            return singletonList(new Replace("a", "b"));
        }
    }

    static class MarkEverySourceFile extends Recipe {
        @Override
        public String getDisplayName() {
            return "Mark every source file";
        }

        @Override
        public String getDescription() {
            return "Marks every source file, whatever its type.";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new TreeVisitor<>() {
                @Override
                public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                    return tree instanceof SourceFile ? SearchResult.found(tree) : tree;
                }
            };
        }
    }

    static class CountScanned extends ScanningRecipe<AtomicInteger> {
        final String needle;

        CountScanned(String needle) {
            this.needle = needle;
        }

        @Override
        public String getDisplayName() {
            return "Count scanned";
        }

        @Override
        public String getDescription() {
            return "Tags every file with the number of scanned files that contain a string.";
        }

        @Override
        public AtomicInteger getInitialValue(ExecutionContext ctx) {
            return new AtomicInteger();
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getScanner(AtomicInteger acc) {
            return new PlainTextVisitor<>() {
                @Override
                public PlainText visitText(PlainText text, ExecutionContext ctx) {
                    if (text.getText().startsWith(needle)) {
                        acc.incrementAndGet();
                    }
                    return text;
                }
            };
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor(AtomicInteger acc) {
            return new Tag(needle + "=" + acc.get()).getVisitor();
        }
    }

    static class FreshRecipeList extends Recipe {
        @Override
        public String getDisplayName() {
            return "Fresh recipe list";
        }

        @Override
        public String getDescription() {
            return "Creates its recipe list on each call.";
        }

        @Override
        public List<Recipe> getRecipeList() {
            return singletonList(new CountScanned("go"));
        }
    }

    abstract static class GenerateOnce extends ScanningRecipe<AtomicBoolean> {
        final String path;
        final String text;

        GenerateOnce(String path, String text) {
            this.path = path;
            this.text = text;
        }

        @Override
        public String getDisplayName() {
            return "Generate once";
        }

        @Override
        public String getDescription() {
            return "Generates a file unless it exists.";
        }

        @Override
        public AtomicBoolean getInitialValue(ExecutionContext ctx) {
            return new AtomicBoolean();
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getScanner(AtomicBoolean exists) {
            return new TreeVisitor<>() {
                @Override
                public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                    if (tree instanceof SourceFile && ((SourceFile) tree).getSourcePath().equals(Paths.get(path))) {
                        exists.set(true);
                    }
                    return tree;
                }
            };
        }
    }

    static class GenerateWithTwoArguments extends GenerateOnce {
        GenerateWithTwoArguments(String path, String text) {
            super(path, text);
        }

        @Override
        public Collection<? extends SourceFile> generate(AtomicBoolean exists, ExecutionContext ctx) {
            return exists.get() ? emptyList() : singletonList(plainText(text, path));
        }
    }

    static class GenerateWithThreeArguments extends GenerateOnce {
        GenerateWithThreeArguments(String path, String text) {
            super(path, text);
        }

        @Override
        public Collection<? extends SourceFile> generate(AtomicBoolean exists, Collection<SourceFile> generatedInThisCycle, ExecutionContext ctx) {
            return exists.get() ? emptyList() : singletonList(plainText(text, path));
        }
    }

    static class RecordWord extends Recipe {
        final String word;
        final transient Words words = new Words(this);

        RecordWord(String word) {
            this.word = word;
        }

        @Override
        public String getDisplayName() {
            return "Record word";
        }

        @Override
        public String getDescription() {
            return "Records a word in a data table and tags the text with it.";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new PlainTextVisitor<>() {
                @Override
                public PlainText visitText(PlainText text, ExecutionContext ctx) {
                    words.insertRow(ctx, new Words.Row(word));
                    String tag = "[" + word + "]";
                    return text.getText().contains(tag) ? text : text.withText(text.getText() + tag);
                }
            };
        }
    }

    static class Words extends DataTable<Words.Row> {
        Words(Recipe recipe) {
            super(recipe, "Words", "Words that recipes recorded.");
        }

        @Value
        static class Row {
            @Column(displayName = "Word", description = "The recorded word.")
            String word;
        }
    }
}
