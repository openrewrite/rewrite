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
 * Reruns a recipe on copies of its sources to check that it finds the code it changes through type attribution:
 * copies that spell that code differently but mean the same, and copies without type attribution.
 */
final class NameMatching {
    private static final RecipeSerializer SERIALIZER = new RecipeSerializer();
    private static final Duration RUN_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration SOURCE_FILE_TIMEOUT = Duration.ofSeconds(5);

    /**
     * Recipes that match type names as text on purpose, because the types they rename may be missing from the classpath.
     */
    private static final Set<String> MATCH_TYPE_NAMES = new HashSet<>(Arrays.asList(
            "org.openrewrite.java.ChangeType",
            "org.openrewrite.java.ChangePackage"
    ));

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
    private final int cycles;
    private final int minCycles;
    private final boolean equivalentSpellings;
    private final boolean typeAttributedMatching;
    private final Function<SourceFile, List<Parser.Builder>> parsersFor;
    private final @Nullable Path relativeTo;
    private final ExecutionContext parseCtx;

    /**
     * What a rerun on the sources as they are makes of each source it changes, which shows that a rerun can
     * reproduce the test's own run.
     */
    private @Nullable Map<UUID, @Nullable SourceFile> control;

    private NameMatching(byte[] recipe, RewriteRunner.Context context, List<SourceFile> parsed, boolean equivalentSpellings,
                         boolean typeAttributedMatching, Function<SourceFile, List<Parser.Builder>> parsersFor,
                         @Nullable Path relativeTo, ExecutionContext parseCtx) {
        this.recipe = recipe;
        this.sources = context.getParsedSources();
        this.parsed = parsed;
        this.messages = new HashMap<>(context.getExecutionContext().getMessages());
        this.messages.keySet().removeAll(RUN_STATE);
        this.cycles = context.getCycles();
        this.minCycles = context.getExpectedCyclesThatMakeChanges() + 1;
        this.equivalentSpellings = equivalentSpellings;
        this.typeAttributedMatching = typeAttributedMatching;
        this.parsersFor = parsersFor;
        this.relativeTo = relativeTo;
        this.parseCtx = parseCtx;
    }

    /**
     * @return null when the test turned these checks off or its run can't be repeated faithfully.
     */
    static @Nullable NameMatching of(Recipe recipe, RewriteRunner runner, RewriteRunner.Context context,
                                     List<SourceFile> parsed, Function<SourceFile, List<Parser.Builder>> parsersFor,
                                     @Nullable Path relativeTo, ExecutionContext parseCtx) {
        RecipeSpec methodSpec = context.getTestMethodSpec();
        RecipeSpec classSpec = context.getTestClassSpec();
        TypeValidation before = TypeValidation.before(methodSpec, classSpec);
        TypeValidation after = TypeValidation.after(methodSpec, classSpec);
        boolean equivalentSpellings = before.equivalentSpellings() && after.equivalentSpellings();
        boolean typeAttributedMatching = before.typeAttributedMatching() && after.typeAttributedMatching();
        boolean typesValidated = before.classDeclarations() && before.identifiers() && before.methodDeclarations() &&
                                 before.variableDeclarations() && before.methodInvocations() && before.constructorInvocations();
        if (!equivalentSpellings && !typeAttributedMatching || !typesValidated || recipe.equals(Recipe.noop()) ||
            runner != RewriteRunner.IN_PROCESS || methodSpec.getSourceSet() != null || classSpec.getSourceSet() != null ||
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
            return new NameMatching(SERIALIZER.write(recipe), context, parsed, equivalentSpellings,
                    typeAttributedMatching, parsersFor, relativeTo, parseCtx);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Ad hoc recipes and recipes declared inside a test class are scaffolding for the test, not recipes under test.
     */
    private static boolean isTestScaffolding(Recipe recipe) {
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

    void assertMatchedByType(List<Result> results) {
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
        List<String> failures = new ArrayList<>(2);
        if (equivalentSpellings) {
            String failure = respellingFailures(changed);
            if (failure != null) {
                failures.add(failure);
            }
        }
        if (typeAttributedMatching) {
            String failure = withoutTypesFailures(changed);
            if (failure != null) {
                failures.add(failure);
            }
        }
        if (!failures.isEmpty()) {
            fail(String.join("\n\n", failures));
        }
    }

    private @Nullable String respellingFailures(List<Result> changed) {
        StringBuilder found = new StringBuilder();
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
                            SourceFile reparsed = AddedMarkers.reparse(respelled, others, parsersFor, relativeTo, parseCtx);
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
                SourceFile respelled = respelling.getSourceFile();
                List<SourceFile> respelledSources = new ArrayList<>(sources);
                respelledSources.set(index, respelled);
                Map<UUID, @Nullable SourceFile> rerun = rerun(respelledSources);
                // a respelling can leave an import unused, so a change to imports or formatting alone doesn't count
                boolean changedCode = rerun != null && rerun.containsKey(before.getId()) &&
                                      service.changesCode(respelled, rerun.get(before.getId()));
                if (rerun != null && !changedCode && reproducible(result)) {
                    found.append("  ").append(before.getSourcePath()).append(changedBy(result))
                            .append(": ").append(respelling.getDescription()).append('\n');
                    break;
                }
            }
        }
        if (found.length() == 0) {
            return null;
        }
        return "Match the code the recipe changes by its type attribution, with MethodMatcher, " +
               "J.MethodInvocation#getMethodType() or TypeUtils, not by names, by whether a call has a select, or by " +
               "how code prints. The recipe changed these sources but left them unchanged once that code was respelled " +
               "to mean the same:\n" + found +
               "A static import and a qualified call name the same method, and an imported and a fully qualified name " +
               "the same type, so a recipe that changes one spelling must change the other. If only one spelling is " +
               "meant to change, add spec.typeValidationOptions(TypeValidation.all().equivalentSpellings(false)).";
    }

    private @Nullable String withoutTypesFailures(List<Result> changed) {
        List<SourceFile> stripped = new ArrayList<>(sources.size());
        Set<UUID> strippedIds = new HashSet<>();
        for (SourceFile source : sources) {
            TypeMatchingValidationService service = service(source);
            SourceFile withoutTypes = null;
            try {
                withoutTypes = service == null ? null : service.withoutTypes(source);
            } catch (Exception ignored) {
                // left with its types, and not judged
            }
            if (withoutTypes != null) {
                strippedIds.add(source.getId());
            }
            stripped.add(withoutTypes == null ? source : withoutTypes);
        }
        Map<UUID, @Nullable SourceFile> rerun = strippedIds.isEmpty() ? null : rerun(stripped);
        if (rerun == null) {
            return null;
        }
        StringBuilder found = new StringBuilder();
        for (Result result : changed) {
            SourceFile before = Objects.requireNonNull(result.getBefore());
            SourceFile after = Objects.requireNonNull(result.getAfter());
            if (!strippedIds.contains(before.getId()) || !printsLike(after, rerun, before.getId()) ||
                !result.getRecipes().isEmpty() && MATCH_TYPE_NAMES.containsAll(leafRecipes(result))) {
                continue;
            }
            List<String> references;
            try {
                references = Objects.requireNonNull(service(before)).referencesToOtherTypes(before, after, parsed);
            } catch (Exception e) {
                continue;
            }
            if (!references.isEmpty()) {
                found.append("  ").append(before.getSourcePath()).append(changedBy(result))
                        .append(": ").append(String.join(", ", references)).append('\n');
            }
        }
        if (found.length() == 0) {
            return null;
        }
        return "Identify the code to change through type attribution, not by name or printed text. The recipe made the " +
               "same change to these sources after all type attribution was removed from them:\n" + found +
               "A recipe that matches names also changes look-alike code, such as same-named methods or types from " +
               "other packages, and shadowed names. Use:\n" +
               "  - method calls: MethodMatcher, plus TypeUtils.isAssignableTo for the receiver\n" +
               "  - annotations: AnnotationMatcher or AnnotationService\n" +
               "  - type references: TypeUtils.isOfClassType\n" +
               "  - expressions: SemanticallyEqual\n" +
               "Don't compare getSimpleName(), toString() or print() output with strings. If the recipe targets no " +
               "particular type (formatting, control flow, literals) or must work without types, add " +
               "spec.typeValidationOptions(TypeValidation.all().typeAttributedMatching(false)).";
    }

    /**
     * Whether a rerun on the sources as they are makes the test's own change, so that a rerun that doesn't make it
     * on a copy shows something about the recipe and not about the rerun.
     */
    private boolean reproducible(Result result) {
        if (control == null) {
            Map<UUID, @Nullable SourceFile> rerun = rerun(sources);
            control = rerun == null ? Collections.emptyMap() : rerun;
        }
        return printsLike(Objects.requireNonNull(result.getAfter()), control, Objects.requireNonNull(result.getBefore()).getId());
    }

    private static boolean printsLike(SourceFile after, Map<UUID, @Nullable SourceFile> rerun, UUID id) {
        SourceFile rerunAfter = rerun.get(id);
        return rerunAfter != null && after.printAll().equals(rerunAfter.printAll());
    }

    /**
     * Runs a fresh copy of the recipe in a fresh execution context with a time limit.
     *
     * @return What the rerun makes of each source it changes (null when it deletes one), by the source's id, or null
     * if it failed or timed out.
     */
    private @Nullable Map<UUID, @Nullable SourceFile> rerun(List<SourceFile> sources) {
        List<Throwable> errors = new ArrayList<>();
        InMemoryExecutionContext ctx = new InMemoryExecutionContext(errors::add);
        messages.forEach(ctx::putMessage);
        ctx.putMessage(ExecutionContext.RUN_TIMEOUT, RUN_TIMEOUT);
        ctx.putMessage(ExecutionContext.SOURCE_FILE_TIMEOUT, SOURCE_FILE_TIMEOUT);
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
            return errors.isEmpty() ? changed : null;
        } catch (Exception | LinkageError | StackOverflowError | AssertionError e) {
            return null;
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

    private static String changedBy(Result result) {
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
