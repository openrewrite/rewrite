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
package org.openrewrite.test;

import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.internal.TypeMatchingValidationService;
import org.openrewrite.internal.TypeMatchingValidationService.Respelling;
import org.openrewrite.scheduling.WorkingDirectoryExecutionContextView;

import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.fail;

/**
 * Reruns a recipe on copies of its sources that spell the code it changes differently but mean the same, to check that
 * it finds that code through type attribution.
 */
final class NameMatching {
    private static final RecipeSerializer SERIALIZER = new RecipeSerializer();

    /**
     * The least time a rerun gets. It also gets three times as long as the test's own run, so that how fast the
     * machine is doesn't decide whether a slow recipe is checked.
     */
    private static final Duration RUN_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration SOURCE_FILE_TIMEOUT = Duration.ofSeconds(5);
    private static final int TEST_RUNS_PER_RERUN = 3;

    /**
     * Messages that a recipe run puts on its execution context, which a rerun must not share.
     */
    private static final Set<String> RUN_STATE = new HashSet<>(Arrays.asList(
            ExecutionContext.CURRENT_CYCLE,
            ExecutionContext.CURRENT_RECIPE,
            Recipe.PANIC,
            DataTableExecutionContextView.DATA_TABLE_STORE,
            WorkingDirectoryExecutionContextView.WORKING_DIRECTORY_ROOT
    ));

    private final byte[] recipe;
    private final List<SourceFile> sources;

    /**
     * Everything the test parsed, including sources it skips, which can still declare types the others use.
     */
    private final List<SourceFile> parsed;

    private final Map<String, @Nullable Object> messages;

    /**
     * The test's own execution context, whose error handler decides whether an error in a rerun fails the test.
     */
    private final ExecutionContext recipeCtx;

    private final int cycles;
    private final int minCycles;
    private Duration testRunTime = Duration.ZERO;
    private final Function<SourceFile, List<Parser.Builder>> parsersFor;
    private final @Nullable Path relativeTo;
    private final ExecutionContext parseCtx;

    /**
     * What a rerun on the sources as they are makes of each source it changes, which shows that a rerun can
     * reproduce the test's own run.
     */
    private @Nullable Map<UUID, @Nullable SourceFile> control;

    private NameMatching(byte[] recipe, RewriteRunner.Context context, List<SourceFile> parsed,
                         Function<SourceFile, List<Parser.Builder>> parsersFor, @Nullable Path relativeTo,
                         ExecutionContext parseCtx) {
        this.recipe = recipe;
        this.sources = context.getParsedSources();
        this.parsed = parsed;
        this.messages = new HashMap<>(context.getExecutionContext().getMessages());
        this.messages.keySet().removeAll(RUN_STATE);
        this.recipeCtx = context.getExecutionContext();
        this.cycles = context.getCycles();
        this.minCycles = context.getExpectedCyclesThatMakeChanges() + 1;
        this.parsersFor = parsersFor;
        this.relativeTo = relativeTo;
        this.parseCtx = parseCtx;
    }

    private static boolean validatesTypes(TypeValidation validation) {
        return validation.classDeclarations() && validation.identifiers() && validation.methodDeclarations() &&
               validation.variableDeclarations() && validation.methodInvocations() &&
               validation.constructorInvocations();
    }

    /**
     * @return null when the test turned these checks off or its run can't be repeated faithfully.
     */
    static @Nullable NameMatching of(Recipe recipe, RewriteRunner runner, RewriteRunner.Context context,
                                     List<SourceFile> parsed, Function<SourceFile, List<Parser.Builder>> parsersFor,
                                     @Nullable Path relativeTo, ExecutionContext parseCtx) {
        RecipeSpec methodSpec = context.getTestMethodSpec();
        RecipeSpec classSpec = context.getTestClassSpec();
        // the reruns are on type attributed code, so the test has to attribute what the recipe gets and makes
        boolean typesValidated = validatesTypes(TypeValidation.before(methodSpec, classSpec)) &&
                                 validatesTypes(TypeValidation.after(methodSpec, classSpec));
        if (!RecipeQualityValidation.of(methodSpec, classSpec).matchByTypeNotName() || !typesValidated ||
            recipe.equals(Recipe.noop()) ||
            runner != RewriteRunner.IN_PROCESS ||
            methodSpec.getSourceSet() != null || classSpec.getSourceSet() != null ||
            !methodSpec.getBeforeRecipes().isEmpty() || !classSpec.getBeforeRecipes().isEmpty() ||
            isTestScaffolding(recipe)) {
            return null;
        }
        boolean checkable = false;
        for (SourceFile source : context.getParsedSources()) {
            if (service(source) != null) {
                checkable = true;
                break;
            }
        }
        if (!checkable) {
            return null;
        }
        try {
            // serialized before the run, so that every rerun starts from a recipe in its initial state
            return new NameMatching(SERIALIZER.write(recipe), context, parsed, parsersFor, relativeTo, parseCtx);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Ad hoc recipes and recipes declared inside a test class are scaffolding for the test, not recipes under test.
     */
    static boolean isTestScaffolding(Recipe recipe) {
        Class<?> recipeClass = recipe.getClass();
        if (recipe instanceof AdHocRecipe || recipe instanceof AdHocScanningRecipe ||
            recipeClass.isAnonymousClass() || recipeClass.isLocalClass()) {
            return true;
        }
        for (Class<?> c = recipeClass.getEnclosingClass(); c != null; c = c.getEnclosingClass()) {
            if (RewriteTest.class.isAssignableFrom(c)) {
                return true;
            }
        }
        try {
            for (Recipe child : recipe.getRecipeList()) {
                if (isTestScaffolding(child)) {
                    return true;
                }
            }
        } catch (Exception e) {
            return true;
        }
        return false;
    }

    void assertMatchedByType(List<Result> results, Duration testRunTime) {
        this.testRunTime = testRunTime;
        List<Result> changed = new ArrayList<>();
        for (Result result : results) {
            if (result.getBefore() != null && result.getAfter() != null && service(result.getBefore()) != null &&
                !result.getBefore().printAll().equals(result.getAfter().printAll())) {
                changed.add(result);
            }
        }
        if (changed.isEmpty()) {
            return;
        }
        String failure = respellingFailures(changed);
        if (failure != null) {
            fail(failure);
        }
    }

    private @Nullable String respellingFailures(List<Result> changed) {
        StringBuilder found = new StringBuilder();
        StringBuilder threw = new StringBuilder();
        for (Result result : changed) {
            SourceFile before = Objects.requireNonNull(result.getBefore());
            SourceFile after = Objects.requireNonNull(result.getAfter());
            List<SourceFile> others = new ArrayList<>();
            for (SourceFile source : parsed) {
                if (!source.getId().equals(before.getId()) && source.getClass() == before.getClass()) {
                    others.add(source);
                }
            }
            int index = sources.indexOf(before);
            TypeMatchingValidationService service = Objects.requireNonNull(service(before));
            List<Respelling> respellings;
            try {
                respellings = index < 0 ? Collections.emptyList() :
                        service.respell(before, after, respelled -> {
                            SourceFile reparsed = AddedMarkers.reparse(respelled, others, parsersFor,
                                    relativeTo, parseCtx);
                            if (reparsed == null) {
                                return null;
                            }
                            // the same file in the same project, only spelled differently
                            SourceFile sameFile = reparsed.withId(before.getId());
                            return sameFile.withMarkers(before.getMarkers());
                        });
            } catch (Exception e) {
                continue;
            }
            for (Respelling respelling : respellings) {
                Rerun rerun = rerun(respelledSources(respelling, index));
                if (rerun.thrown != null && reproducible(result) && Probes.rejects(recipeCtx, rerun.thrown)) {
                    threw.append("  ").append(before.getSourcePath()).append(changedBy(result)).append(", ")
                            .append(respelling.getDescription()).append(":\n")
                            .append(Probes.describe(rerun.thrown, "    ", SERIALIZER.read(recipe))).append('\n');
                    break;
                }
                if (misses(rerun, respelling, before, service) && reproducible(result)) {
                    found.append("  ").append(before.getSourcePath()).append(changedBy(result))
                            .append(": ").append(missedPlaces(respelling, before, index, service)).append('\n');
                    break;
                }
            }
        }
        List<String> failures = new ArrayList<>(2);
        if (threw.length() > 0) {
            failures.add("Check the shape of the code the recipe edits before casting it, so that code written " +
                         "another way is changed or left as it is instead of throwing. The recipe threw on these " +
                         "sources once the code it changed was respelled to mean the same:\n" + threw +
                         "A statically imported call has no select, and a fully qualified type name is a " +
                         "J.FieldAccess, not a J.Identifier. Check `instanceof` before a cast and null before a " +
                         "dereference, and don't catch the exception to make this pass.");
        }
        if (found.length() > 0) {
            failures.add("Match the code the recipe changes by its type attribution, with MethodMatcher, " +
                         "J.MethodInvocation#getMethodType() or TypeUtils, not by names, by whether a call has a " +
                         "select, or by how code prints. The recipe changed these sources but left them unchanged " +
                         "once that code was respelled to mean the same:\n" + found +
                         "A static import and a qualified call name the same method, and an imported and a fully " +
                         "qualified name the same type, so a recipe that changes one spelling must change the other. " +
                         "If only one spelling is meant to change, add " +
                         "spec.recipeQualityValidation(RecipeQualityValidation.builder().matchByTypeNotName(false)" +
                         ".build()).");
        }
        return failures.isEmpty() ? null : String.join("\n\n", failures);
    }

    private List<SourceFile> respelledSources(Respelling respelling, int index) {
        List<SourceFile> respelledSources = new ArrayList<>(sources);
        respelledSources.set(index, respelling.getSourceFile());
        return respelledSources;
    }

    /**
     * Whether the recipe, run again with the respelled source in place of {@code before}, leaves its code unchanged.
     */
    private boolean misses(Rerun rerun, Respelling respelling, SourceFile before,
                           TypeMatchingValidationService service) {
        // a respelling can leave an import unused, so a change to imports or formatting alone doesn't count
        return rerun.changed != null && !(rerun.changed.containsKey(before.getId()) &&
                                          service.changesCode(respelling.getSourceFile(),
                                                  rerun.changed.get(before.getId())));
    }

    /**
     * The places of a respelling that the recipe misses on their own, or the whole respelling when it misses none
     * of them alone.
     */
    private String missedPlaces(Respelling respelling, SourceFile before, int index,
                                TypeMatchingValidationService service) {
        List<String> missed = new ArrayList<>();
        for (Respelling place : respelling.getEachPlace().get()) {
            if (misses(rerun(respelledSources(place, index)), place, before, service)) {
                missed.add(place.getDescription());
            }
        }
        return missed.isEmpty() ? respelling.getDescription() : String.join(", ", missed);
    }

    /**
     * Whether a rerun on the sources as they are makes the test's own change, so that a rerun that doesn't make it
     * on a copy shows something about the recipe and not about the rerun.
     */
    private boolean reproducible(Result result) {
        if (control == null) {
            Map<UUID, @Nullable SourceFile> rerun = rerun(sources).changed;
            control = rerun == null ? Collections.emptyMap() : rerun;
        }
        return printsLike(Objects.requireNonNull(result.getAfter()), control,
                Objects.requireNonNull(result.getBefore()).getId());
    }

    private static boolean printsLike(SourceFile after, Map<UUID, @Nullable SourceFile> rerun, UUID id) {
        SourceFile rerunAfter = rerun.get(id);
        return rerunAfter != null && after.printAll().equals(rerunAfter.printAll());
    }

    private static Duration longer(Duration a, Duration b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    /**
     * Runs a fresh copy of the recipe in a fresh execution context with a time limit.
     */
    private Rerun rerun(List<SourceFile> sources) {
        List<Throwable> errors = new ArrayList<>();
        InMemoryExecutionContext ctx = new InMemoryExecutionContext(errors::add);
        messages.forEach(ctx::putMessage);
        Duration asLongAsTheTest = testRunTime.multipliedBy(TEST_RUNS_PER_RERUN);
        ctx.putMessage(ExecutionContext.RUN_TIMEOUT, longer(RUN_TIMEOUT, asLongAsTheTest));
        ctx.putMessage(ExecutionContext.SOURCE_FILE_TIMEOUT, longer(SOURCE_FILE_TIMEOUT, asLongAsTheTest));
        DataTableExecutionContextView.view(ctx).setDataTableStore(DataTableStore.noop());
        try {
            RecipeRun run = SERIALIZER.read(recipe).run(new InMemoryLargeSourceSet(sources), ctx, cycles, minCycles);
            Map<UUID, @Nullable SourceFile> changed = new HashMap<>();
            for (Result result : run.getChangeset().getAllResults()) {
                SourceFile before = result.getBefore();
                SourceFile after = result.getAfter();
                if (before != null && (after == null || !after.printAll().equals(before.printAll()))) {
                    changed.put(before.getId(), after);
                }
            }
            return errors.isEmpty() ? new Rerun(changed, null) : new Rerun(null, Probes.firstError(errors));
        } catch (Exception | StackOverflowError | AssertionError e) {
            errors.add(e);
            return new Rerun(null, Probes.firstError(errors));
        } catch (LinkageError e) {
            return new Rerun(null, null);
        }
    }

    /**
     * What a rerun makes of each source it changes (null when it deletes one) by the source's id, or what it threw.
     * Neither is known when it ran out of time.
     */
    private static final class Rerun {
        final @Nullable Map<UUID, @Nullable SourceFile> changed;
        final @Nullable Throwable thrown;

        Rerun(@Nullable Map<UUID, @Nullable SourceFile> changed, @Nullable Throwable thrown) {
            this.changed = changed;
            this.thrown = thrown;
        }
    }

    private static Set<String> leafRecipes(Result result) {
        Set<String> leaves = new LinkedHashSet<>();
        for (List<Recipe> stack : result.getRecipes()) {
            if (!stack.isEmpty()) {
                leaves.add(stack.get(stack.size() - 1).getName());
            }
        }
        return leaves;
    }

    static String changedBy(Result result) {
        Set<String> leaves = leafRecipes(result);
        return leaves.isEmpty() ? "" : " (changed by " + String.join(", ", leaves) + ")";
    }

    private static @Nullable TypeMatchingValidationService service(SourceFile sourceFile) {
        try {
            return sourceFile.service(TypeMatchingValidationService.class);
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }
}
