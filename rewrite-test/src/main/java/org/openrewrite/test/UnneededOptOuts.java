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

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.CursorValidatingExecutionContextView;
import org.openrewrite.ExecutionContext;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.openrewrite.ExecutionContext.SCANNING_MUTATIONS;

/**
 * Fails a test that turns off checks that pass on its sources, when the system property {@value #PROPERTY} asks for
 * it as {@link RecipeSpec#typeValidationOptions} describes.
 * <p>
 * The checks a test turns off run on the side while it runs. Once it has passed, they are turned back on one at a
 * time, and each that still passes stays on. What is still turned off at the end passes, and is what the test needs.
 */
final class UnneededOptOuts {
    static final String PROPERTY = "org.openrewrite.test.failOnUnneededOptOuts";

    private static final String SERIALIZATION = "validateRecipeSerialization";

    /**
     * Recipe quality checks that run only when the test validates the type attribution of the recipe's input and
     * output.
     */
    private static final Set<String> NEED_TYPES = new HashSet<>(asList("matchByTypeNotName", "parserTypedTrees"));

    /**
     * The deprecated option of {@link TypeValidation} that has no effect, so turning it off is never reported.
     */
    private static final String NO_EFFECT = "cursorAcyclic";

    /**
     * The deprecated options of {@link TypeValidation} that turn off a recipe quality check, by that check.
     */
    private static final Map<String, String> DEPRECATED_OPTIONS = new LinkedHashMap<>();

    static {
        DEPRECATED_OPTIONS.put("executionContextUnchanged", "immutableExecutionContext");
        DEPRECATED_OPTIONS.put("noEditsInScanningPhase", "immutableScanning");
    }

    private static final Map<Class<?>, List<Field>> OPTION_FIELDS = new HashMap<>();
    private static final Set<String> KNOWN = new LinkedHashSet<>();

    static {
        OPTION_FIELDS.put(TypeValidation.class, declaredOptions(TypeValidation.class));
        OPTION_FIELDS.put(RecipeQualityValidation.class, declaredOptions(RecipeQualityValidation.class));
        for (Field option : options(TypeValidation.class)) {
            KNOWN.add(option.getName());
        }
        for (Field option : options(RecipeQualityValidation.class)) {
            KNOWN.add(option.getName());
        }
        KNOWN.add(SERIALIZATION);
    }

    private final String property;
    private final Set<String> checked;
    private final Options asRun;
    private final List<Consumer<TypeValidation>> beforeValidations = new ArrayList<>();
    private final List<Consumer<TypeValidation>> afterValidations = new ArrayList<>();
    private final Map<String, Runnable> checksOfTheRun = new HashMap<>();
    private final Map<String, Runnable> checksOnTheSide = new HashMap<>();
    private final Map<String, Boolean> passedOnTheSide = new HashMap<>();

    private UnneededOptOuts(String property, Set<String> checked, Options asRun) {
        this.property = property;
        this.checked = checked;
        this.asRun = asRun;
    }

    static UnneededOptOuts of(RecipeSpec testMethodSpec, RecipeSpec testClassSpec) {
        String property = System.getProperty(PROPERTY, "").trim();
        Set<String> checked = new LinkedHashSet<>();
        List<String> unknown = new ArrayList<>();
        for (String name : property.split(",")) {
            name = name.trim();
            if ("all".equals(name)) {
                checked.addAll(KNOWN);
            } else if (KNOWN.contains(name)) {
                checked.add(name);
            } else if (!name.isEmpty()) {
                unknown.add(name);
            }
        }
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException(PROPERTY + " names checks that don't exist: " +
                                               String.join(", ", unknown) + ". Name all, or some of: " +
                                               String.join(", ", KNOWN) + ".");
        }
        DEPRECATED_OPTIONS.forEach((check, deprecated) -> {
            if (checked.contains(check) || checked.contains(deprecated)) {
                checked.add(check);
                checked.add(deprecated);
            }
        });
        return new UnneededOptOuts(property, checked, new Options(testMethodSpec, testClassSpec));
    }

    /**
     * Whether a recipe quality check or the recipe's serialization checks, which the test turns off, run on the side to
     * find whether the test needs them turned off.
     */
    boolean runsOnTheSide(String check) {
        if (SERIALIZATION.equals(check)) {
            return checked.contains(check) && !asRun.serializes();
        }
        return (checked.contains(check) || NEED_TYPES.contains(check) && checksTypeValidation()) &&
               !asRun.runs(check);
    }

    /**
     * Runs the assertion if the test asks for the check, or else runs it on the side.
     */
    void check(String check, boolean on, Runnable assertion) {
        if (on) {
            assertion.run();
        } else {
            checkOnTheSide(check, assertion);
        }
    }

    private void checkOnTheSide(String check, Runnable assertion) {
        if (runsOnTheSide(check)) {
            passedOnTheSide.put(check, passes(assertion));
        }
    }

    /**
     * Keeps the assertion to run on the side only if the opt-outs being turned back on need it. What it reads has to
     * stay as it is until the test ends.
     */
    void checkOnTheSideLater(String check, Runnable assertion) {
        if (runsOnTheSide(check)) {
            checksOnTheSide.put(check, assertion);
        }
    }

    /**
     * @param validation Validates a source the test parsed with the type validation it is given.
     */
    void validatesBefore(Consumer<TypeValidation> validation) {
        if (checksTypeValidation()) {
            beforeValidations.add(validation);
        }
    }

    /**
     * @param validation Validates a source the recipe made with the type validation it is given.
     */
    void validatesAfter(Consumer<TypeValidation> validation) {
        if (checksTypeValidation()) {
            afterValidations.add(validation);
        }
    }

    private boolean checksTypeValidation() {
        for (Field option : options(TypeValidation.class)) {
            if (checked.contains(option.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * The view of the execution context the recipe runs with, which records the messages the recipe puts on it when
     * that check runs on the side.
     */
    CursorValidatingExecutionContextView executionContextView(ExecutionContext ctx) {
        if (!runsOnTheSide("executionContextUnchanged")) {
            return CursorValidatingExecutionContextView.view(ctx);
        }
        ExecutionContextChanges changes = new ExecutionContextChanges(ctx);
        checksOfTheRun.put("executionContextUnchanged", () -> assertThat(changes.keys).isEmpty());
        return changes;
    }

    /**
     * Has the scanning phase report the edits of scanners when that check runs on the side.
     */
    void recordScanningMutations(ExecutionContext ctx) {
        // a context that the tests of a class share may still hold an earlier test's collection
        ctx.pollMessage(SCANNING_MUTATIONS);
        if (runsOnTheSide("noEditsInScanningPhase")) {
            Collection<String> mutations = new CopyOnWriteArrayList<>();
            ctx.putMessage(SCANNING_MUTATIONS, mutations);
            checksOfTheRun.put("noEditsInScanningPhase", () -> assertThat(mutations).isEmpty());
        }
    }

    void assertNoneUnneeded() {
        if (checked.isEmpty()) {
            return;
        }
        // RewriteTest's reruns of the recipe, which the real checks also see, are over, and those of side checks aren't
        checksOfTheRun.forEach(this::checkOnTheSide);
        List<OptOut> unneeded = new ArrayList<>();
        Map<Boolean, Set<String>> turnedOffInstead = new HashMap<>();
        Options current = asRun;
        for (OptOut optOut : asRun.optOuts(checked)) {
            Options candidate = current.turnOn(optOut);
            List<String> failed = failures(candidate, current);
            // the type checks may pass while the checks that need types, which they kept from running, fail
            if (!failed.isEmpty() && NEED_TYPES.containsAll(failed) &&
                !current.validatesTypes() && candidate.validatesTypes()) {
                boolean inDefaults = candidate.qualityInDefaults(optOut.isInDefaults());
                Options instead = candidate.turnOff(failed, inDefaults);
                if (failures(instead, current).isEmpty()) {
                    candidate = instead;
                    turnedOffInstead.computeIfAbsent(inDefaults, d -> new LinkedHashSet<>()).addAll(failed);
                    failed = emptyList();
                }
            }
            if (failed.isEmpty()) {
                current = candidate;
                unneeded.add(optOut);
            }
        }
        if (!unneeded.isEmpty()) {
            fail(message(unneeded, turnedOffInstead, current));
        }
    }

    /**
     * The checks that fail with the candidate's options. The current options pass, so only what the candidate changes
     * runs.
     */
    private List<String> failures(Options candidate, Options current) {
        List<String> failed = new ArrayList<>();
        TypeValidation before = candidate.before();
        if (!before.equals(current.before()) && !passes(beforeValidations, before)) {
            failed.add("before");
        }
        TypeValidation after = candidate.after();
        if (!after.equals(current.after()) && !passes(afterValidations, after)) {
            failed.add("after");
        }
        for (Field option : options(RecipeQualityValidation.class)) {
            String check = option.getName();
            if (candidate.runs(check) && !current.runs(check) && !passesOnTheSide(check)) {
                failed.add(check);
            }
        }
        if (candidate.serializes() && !current.serializes() && !passesOnTheSide(SERIALIZATION)) {
            failed.add(SERIALIZATION);
        }
        return failed;
    }

    private static boolean passes(List<Consumer<TypeValidation>> validations, TypeValidation typeValidation) {
        for (Consumer<TypeValidation> validation : validations) {
            if (!passes(() -> validation.accept(typeValidation))) {
                return false;
            }
        }
        return true;
    }

    private boolean passesOnTheSide(String check) {
        Boolean passed = passedOnTheSide.get(check);
        if (passed == null) {
            Runnable assertion = checksOnTheSide.get(check);
            if (assertion == null) {
                throw new IllegalStateException("RewriteTest didn't run " + check + " on the side");
            }
            passed = passes(assertion);
            passedOnTheSide.put(check, passed);
        }
        return passed;
    }

    private static boolean passes(Runnable assertion) {
        try {
            assertion.run();
            return true;
        } catch (Exception | AssertionError | LinkageError | StackOverflowError e) {
            return false;
        }
    }

    private String message(List<OptOut> unneeded, Map<Boolean, Set<String>> turnedOffInstead, Options needed) {
        StringBuilder message = new StringBuilder("The test turns off checks that pass on its sources, which ")
                .append(PROPERTY).append('=').append(property)
                .append(" reports so that the opt-outs can be removed.\n");
        for (boolean inDefaults : new boolean[]{false, true}) {
            String where = inDefaults ? "defaults(RecipeSpec)" : "the test's RecipeSpec";
            Map<String, List<String>> checksByOption = new LinkedHashMap<>();
            for (OptOut optOut : unneeded) {
                if (optOut.isInDefaults() == inDefaults) {
                    checksByOption.computeIfAbsent(optOut.getOption(), o -> new ArrayList<>()).add(optOut.getCheck());
                }
            }
            Set<String> instead = turnedOffInstead.getOrDefault(inDefaults, emptySet());
            if (checksByOption.isEmpty() && instead.isEmpty()) {
                continue;
            }
            message.append('\n');
            checksByOption.forEach((option, checks) -> message.append(option).append(" in ").append(where)
                    .append(" turns off checks that pass: ").append(String.join(", ", checks)).append(".\n"));
            if (!instead.isEmpty()) {
                message.append("With the type checks on, ").append(String.join(" and ", instead))
                        .append(instead.size() == 1 ? " runs and fails, so turn it" : " run and fail, so turn them")
                        .append(" off instead.\n");
            }
            RecipeSpec spec = needed.spec(inDefaults);
            Set<String> options = new LinkedHashSet<>(checksByOption.keySet());
            if (inDefaults) {
                options.addAll(asRun.optionsOnlyInDefaults());
            }
            List<String> calls = calls(spec, inDefaults ? null : needed.spec(true), options, !instead.isEmpty(),
                    needed.afterChangesWithout(inDefaults));
            if (inDefaults) {
                if (calls.isEmpty()) {
                    message.append("This test needs none of the opt-outs in defaults(RecipeSpec).\n");
                } else {
                    message.append("Of the opt-outs in defaults(RecipeSpec), this test needs only:\n");
                    append(message, calls);
                }
                message.append("defaults(RecipeSpec) needs only what the tests of its class need together, which ")
                        .append("each test that fails like this one lists.\n");
            } else {
                if (asRun.spec(false).recipeQualityValidation != null &&
                    calls.stream().anyMatch(call -> call.startsWith("spec.recipeQualityValidation("))) {
                    options.add("recipeQualityValidation(..)");
                }
                if (calls.isEmpty()) {
                    message.append("Remove ").append(String.join(" and ", options)).append(" from ").append(where)
                            .append(".\n");
                } else {
                    message.append("Replace ").append(String.join(" and ", options)).append(" in ").append(where)
                            .append(" with:\n");
                    append(message, calls);
                }
            }
        }
        return message.toString();
    }

    private static void append(StringBuilder message, List<String> calls) {
        calls.forEach(call -> message.append("    ").append(call).append('\n'));
        if (calls.stream().anyMatch(call -> call.contains("(..)"))) {
            message.append("Where these show .., keep the test's own argument.\n");
        }
    }

    /**
     * The calls on a recipe spec that set the given options as the spec has them, with the deprecated options of
     * {@link TypeValidation} that turn off a recipe quality check written as that check.
     *
     * @param overridden   The spec of the test's class when the spec is the test's, whose options the test's override.
     * @param afterNeeded Whether the after validation the spec sets has to stay, even when it is the default.
     */
    @SuppressWarnings("deprecation")
    private static List<String> calls(RecipeSpec spec, @Nullable RecipeSpec overridden, Set<String> options,
                                      boolean qualityChanged, boolean afterNeeded) {
        List<String> calls = new ArrayList<>();
        List<String> turnedOffByTypeValidation = new ArrayList<>();
        if (options.contains("typeValidationOptions(..)") && spec.typeValidation != null) {
            call(calls, "typeValidationOptions", render(spec.typeValidation),
                    overridden != null && overridden.typeValidation != null ? "TypeValidation.all()" : null);
            DEPRECATED_OPTIONS.forEach((check, deprecated) -> {
                if (!isDefault(spec.typeValidation, field(TypeValidation.class, deprecated))) {
                    turnedOffByTypeValidation.add(check);
                }
            });
        }
        if (options.contains("afterTypeValidationOptions(..)") && spec.afterTypeValidation != null) {
            call(calls, "afterTypeValidationOptions", render(spec.afterTypeValidation),
                    afterNeeded || overridden != null && overridden.afterTypeValidation != null ?
                            "TypeValidation.all()" : null);
        }
        if (options.contains("recipeQualityValidation(..)") || qualityChanged ||
            !turnedOffByTypeValidation.isEmpty()) {
            RecipeQualityValidation quality = spec.recipeQualityValidation != null ? spec.recipeQualityValidation :
                    overridden != null && overridden.recipeQualityValidation != null ?
                            overridden.recipeQualityValidation : RecipeQualityValidation.all();
            for (String check : turnedOffByTypeValidation) {
                quality = with(quality, check, false);
            }
            call(calls, "recipeQualityValidation", render(quality, afterNeeded),
                    overridden != null && overridden.recipeQualityValidation != null ?
                            "RecipeQualityValidation.builder().build()" : null);
        }
        if (options.contains("validateRecipeSerialization(false)") && !spec.serializationValidation) {
            calls.add("spec.validateRecipeSerialization(false)");
        }
        return calls;
    }

    /**
     * @param unchanged The argument that sets the option to its default, to keep overriding the option of the test's
     *                  class, or null to leave the option out when it is the default.
     */
    private static void call(List<String> calls, String method, @Nullable String argument,
                             @Nullable String unchanged) {
        String set = argument != null ? argument : unchanged;
        if (set != null) {
            calls.add("spec." + method + "(" + set + ")");
        }
    }

    private static @Nullable String render(TypeValidation typeValidation) {
        StringBuilder rendered = new StringBuilder("TypeValidation.all()");
        for (Field option : options(TypeValidation.class)) {
            // the deprecated options are written as the recipe quality checks they turn off
            if (!DEPRECATED_OPTIONS.containsValue(option.getName()) && !NO_EFFECT.equals(option.getName()) &&
                !isDefault(typeValidation, option)) {
                rendered.append('.').append(option.getName()).append('(').append(value(typeValidation, option))
                        .append(')');
            }
        }
        return rendered.length() == "TypeValidation.all()".length() ? null : rendered.toString();
    }

    private static @Nullable String render(RecipeQualityValidation quality, boolean typesNeeded) {
        StringBuilder rendered = new StringBuilder("RecipeQualityValidation.builder()");
        for (Field option : options(RecipeQualityValidation.class)) {
            if (!isDefault(quality, option)) {
                rendered.append('.').append(option.getName()).append('(').append(value(quality, option)).append(')');
            }
        }
        String types = quality.types() == null ? null : render(quality.types());
        if (types == null && quality.types() != null && typesNeeded) {
            types = "TypeValidation.all()";
        }
        if (types != null) {
            rendered.append(".types(").append(types).append(')');
        }
        return rendered.length() == "RecipeQualityValidation.builder()".length() ? null :
                rendered.append(".build()").toString();
    }

    private static String value(Object options, Field option) {
        Object value = get(options, option);
        return value instanceof Boolean ? value.toString() : "..";
    }

    /**
     * The options of {@link TypeValidation} or the recipe quality checks of {@link RecipeQualityValidation}.
     */
    private static List<Field> options(Class<?> type) {
        return OPTION_FIELDS.get(type);
    }

    private static List<Field> declaredOptions(Class<?> type) {
        List<Field> options = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic() &&
                field.getType() != TypeValidation.class) {
                field.setAccessible(true);
                options.add(field);
            }
        }
        return options;
    }

    private static Field field(Class<?> type, String name) {
        for (Field option : options(type)) {
            if (option.getName().equals(name)) {
                return option;
            }
        }
        throw new IllegalArgumentException(type.getSimpleName() + " has no option " + name);
    }

    private static List<String> turnedOff(Object options, Set<String> checked) {
        List<String> turnedOff = new ArrayList<>();
        for (Field option : options(options.getClass())) {
            if (checked.contains(option.getName()) && !NO_EFFECT.equals(option.getName()) &&
                !isDefault(options, option)) {
                turnedOff.add(option.getName());
            }
        }
        return turnedOff;
    }

    private static boolean isDefault(Object options, Field option) {
        return Objects.equals(get(options, option), get(defaults(options), option));
    }

    private static <T> T withDefault(T options, String option) {
        return with(options, option, get(defaults(options), field(options.getClass(), option)));
    }

    private static Object defaults(Object options) {
        return options instanceof TypeValidation ? TypeValidation.all() : RecipeQualityValidation.all();
    }

    private static <T> T with(T options, String option, @Nullable Object value) {
        try {
            @SuppressWarnings("unchecked")
            T copy = (T) options.getClass().getDeclaredConstructor().newInstance();
            for (Field field : options.getClass().getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    field.set(copy, option.equals(field.getName()) ? value : field.get(options));
                }
            }
            return copy;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static @Nullable Object get(Object options, Field option) {
        try {
            return option.get(options);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * A check that an option of a recipe spec turns off, and how to turn it back on.
     */
    @Value
    private static class OptOut {
        boolean inDefaults;
        String option;
        String check;
        UnaryOperator<RecipeSpec> turnOn;
    }

    /**
     * The options of a test and of its class that turn checks off, which decide the checks that run.
     */
    @SuppressWarnings("deprecation")
    private static final class Options {
        final RecipeSpec testMethodSpec;
        final RecipeSpec testClassSpec;

        Options(RecipeSpec testMethodSpec, RecipeSpec testClassSpec) {
            this.testMethodSpec = copy(testMethodSpec);
            this.testClassSpec = copy(testClassSpec);
        }

        private static RecipeSpec copy(RecipeSpec spec) {
            RecipeSpec copy = new RecipeSpec();
            copy.typeValidation = spec.typeValidation;
            copy.afterTypeValidation = spec.afterTypeValidation;
            copy.recipeQualityValidation = spec.recipeQualityValidation;
            copy.serializationValidation = spec.serializationValidation;
            return copy;
        }

        RecipeSpec spec(boolean inDefaults) {
            return inDefaults ? testClassSpec : testMethodSpec;
        }

        TypeValidation before() {
            return TypeValidation.before(testMethodSpec, testClassSpec);
        }

        TypeValidation after() {
            return TypeValidation.after(testMethodSpec, testClassSpec);
        }

        boolean validatesTypes() {
            return TypeValidation.validatesTypes(testMethodSpec, testClassSpec);
        }

        boolean serializes() {
            return testMethodSpec.serializationValidation && testClassSpec.serializationValidation;
        }

        boolean runs(String check) {
            RecipeQualityValidation quality = RecipeQualityValidation.of(testMethodSpec, testClassSpec);
            return Boolean.TRUE.equals(get(quality, field(RecipeQualityValidation.class, check))) &&
                   (!NEED_TYPES.contains(check) || validatesTypes());
        }

        /**
         * The checks the options turn off, of those to look at, in the order they are turned back on.
         */
        List<OptOut> optOuts(Set<String> checked) {
            List<OptOut> optOuts = new ArrayList<>();
            boolean beforeInDefaults = testMethodSpec.typeValidation == null;
            TypeValidation before = spec(beforeInDefaults).typeValidation;
            if (before != null) {
                for (String check : turnedOff(before, checked)) {
                    optOuts.add(new OptOut(beforeInDefaults, "typeValidationOptions(..)", check, spec -> {
                        spec.typeValidation = withDefault(spec.typeValidation, check);
                        return spec;
                    }));
                }
            }
            for (boolean inDefaults : new boolean[]{false, true}) {
                RecipeQualityValidation quality = spec(inDefaults).recipeQualityValidation;
                if (quality != null && quality.types() != null) {
                    for (String check : turnedOff(quality.types(), checked)) {
                        optOuts.add(new OptOut(inDefaults, "recipeQualityValidation(..)", "types." + check, spec -> {
                            spec.recipeQualityValidation = with(spec.recipeQualityValidation, "types",
                                    withDefault(spec.recipeQualityValidation.types(), check));
                            return spec;
                        }));
                    }
                    break;
                }
                TypeValidation after = spec(inDefaults).afterTypeValidation;
                if (after != null) {
                    for (String check : turnedOff(after, checked)) {
                        optOuts.add(new OptOut(inDefaults, "afterTypeValidationOptions(..)", check, spec -> {
                            spec.afterTypeValidation = withDefault(spec.afterTypeValidation, check);
                            return spec;
                        }));
                    }
                    break;
                }
            }
            boolean qualityInDefaults = testMethodSpec.recipeQualityValidation == null;
            RecipeQualityValidation quality = spec(qualityInDefaults).recipeQualityValidation;
            if (quality != null) {
                for (String check : turnedOff(quality, checked)) {
                    optOuts.add(new OptOut(qualityInDefaults, "recipeQualityValidation(..)", check, spec -> {
                        spec.recipeQualityValidation = withDefault(spec.recipeQualityValidation, check);
                        return spec;
                    }));
                }
            }
            for (boolean inDefaults : new boolean[]{false, true}) {
                if (!spec(inDefaults).serializationValidation && checked.contains(SERIALIZATION)) {
                    optOuts.add(new OptOut(inDefaults, "validateRecipeSerialization(false)", SERIALIZATION, spec -> {
                        spec.serializationValidation = true;
                        return spec;
                    }));
                }
            }
            return optOuts;
        }

        /**
         * The options set in the defaults of the test's class that the test doesn't override.
         */
        Set<String> optionsOnlyInDefaults() {
            Set<String> options = new LinkedHashSet<>();
            if (testClassSpec.typeValidation != null && testMethodSpec.typeValidation == null) {
                options.add("typeValidationOptions(..)");
            }
            if (testClassSpec.afterTypeValidation != null && testMethodSpec.afterTypeValidation == null) {
                options.add("afterTypeValidationOptions(..)");
            }
            if (testClassSpec.recipeQualityValidation != null && testMethodSpec.recipeQualityValidation == null) {
                options.add("recipeQualityValidation(..)");
            }
            if (!testClassSpec.serializationValidation) {
                options.add("validateRecipeSerialization(false)");
            }
            return options;
        }

        /**
         * Whether leaving out the after validation of the test or of its class changes the after validation, since
         * without one it falls back on the before validation.
         */
        boolean afterChangesWithout(boolean inDefaults) {
            RecipeSpec without = copy(spec(inDefaults));
            without.afterTypeValidation = null;
            if (without.recipeQualityValidation != null) {
                without.recipeQualityValidation = with(without.recipeQualityValidation, "types", null);
            }
            TypeValidation after = inDefaults ? TypeValidation.after(testMethodSpec, without) :
                    TypeValidation.after(without, testClassSpec);
            return !after.equals(after());
        }

        Options turnOn(OptOut optOut) {
            boolean inDefaults = optOut.isInDefaults();
            return new Options(
                    inDefaults ? testMethodSpec : optOut.getTurnOn().apply(copy(testMethodSpec)),
                    inDefaults ? optOut.getTurnOn().apply(copy(testClassSpec)) : testClassSpec);
        }

        /**
         * Whether recipe quality checks get turned off in the defaults of the test's class. That is where they are
         * set when only the defaults set them. When neither sets them, it is where the type validation is.
         */
        boolean qualityInDefaults(boolean typeValidationInDefaults) {
            return testMethodSpec.recipeQualityValidation == null &&
                   (testClassSpec.recipeQualityValidation != null || typeValidationInDefaults);
        }

        Options turnOff(Collection<String> checks, boolean inDefaults) {
            RecipeSpec spec = copy(spec(inDefaults));
            RecipeQualityValidation quality = spec.recipeQualityValidation == null ?
                    RecipeQualityValidation.all() : spec.recipeQualityValidation;
            for (String check : checks) {
                quality = with(quality, check, false);
            }
            spec.recipeQualityValidation = quality;
            return new Options(inDefaults ? testMethodSpec : spec, inDefaults ? spec : testClassSpec);
        }
    }

    /**
     * A view of the execution context that records the messages a recipe puts on it.
     */
    private static final class ExecutionContextChanges extends CursorValidatingExecutionContextView {
        final Set<String> keys = ConcurrentHashMap.newKeySet();

        ExecutionContextChanges(ExecutionContext delegate) {
            super(delegate);
        }

        @Override
        public void putMessage(String key, @Nullable Object value) {
            if (!isAlwaysMutable(key)) {
                keys.add(key);
            }
            super.putMessage(key, value);
        }
    }
}
