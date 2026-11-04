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
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.internal.RecipeRunException;
import org.openrewrite.scheduling.WorkingDirectoryExecutionContextView;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * What the checks that run a recipe on sources the test doesn't give it share: a time budget per recipe, a fresh
 * execution context and how they tell what the recipe threw.
 */
final class Probes {

    static final Duration TIMEOUT = Duration.ofSeconds(10);

    /**
     * Probes of recipes that resolve dependencies take seconds each, so a recipe stops being probed in this JVM once
     * its probes have taken this long.
     */
    private static final long TIME_CAP_NANOS = Duration.ofSeconds(20).toNanos();

    private static final Map<String, Long> PROBE_NANOS = new ConcurrentHashMap<>();

    /**
     * Messages that belong to the test's own run, or that would let the test see the probe.
     */
    private static final Set<String> NOT_COPIED = new HashSet<>(Arrays.asList(
            DataTableExecutionContextView.DATA_TABLE_STORE,
            ExecutionContext.CURRENT_CYCLE,
            ExecutionContext.CURRENT_RECIPE,
            ExecutionContext.RUN_TIMEOUT,
            Recipe.PANIC,
            WorkingDirectoryExecutionContextView.WORKING_DIRECTORY_ROOT,
            "org.openrewrite.internal.treeObservers",
            "org.openrewrite.core.parsingListener"
    ));

    private Probes() {
    }

    static boolean overBudget(Recipe recipe) {
        return PROBE_NANOS.getOrDefault(timeCapKey(recipe), 0L) > TIME_CAP_NANOS;
    }

    static void charge(Recipe recipe, long nanos) {
        PROBE_NANOS.merge(timeCapKey(recipe), nanos, Long::sum);
    }

    /**
     * What a recipe's probes share a time cap by, since every composite has the same name.
     */
    private static String timeCapKey(Recipe recipe) {
        if (!(recipe instanceof CompositeRecipe)) {
            return recipe.getName();
        }
        StringJoiner children = new StringJoiner(",", "[", "]");
        for (Recipe child : recipe.getRecipeList()) {
            children.add(timeCapKey(child));
        }
        return children.toString();
    }

    static boolean isAdHoc(Recipe recipe) {
        if (recipe instanceof AdHocRecipe || recipe instanceof AdHocScanningRecipe) {
            return true;
        }
        for (Recipe child : recipe.getRecipeList()) {
            if (isAdHoc(child)) {
                return true;
            }
        }
        return false;
    }

    static ExecutionContext copy(ExecutionContext from, Consumer<Throwable> onError) {
        ExecutionContext ctx = new InMemoryExecutionContext(onError, TIMEOUT);
        for (Map.Entry<String, @Nullable Object> message : from.getMessages().entrySet()) {
            if (message.getValue() != null && !NOT_COPIED.contains(message.getKey())) {
                ctx.putMessage(message.getKey(), message.getValue());
            }
        }
        return ctx;
    }

    /**
     * The first error from running the recipe on the sources, other than running out of time.
     */
    static @Nullable Throwable run(Recipe recipe, List<SourceFile> sources, ExecutionContext recipeCtx, int cycles) {
        List<Throwable> errors = new CopyOnWriteArrayList<>();
        ExecutionContext ctx = copy(recipeCtx, errors::add);
        ctx.putMessage(ExecutionContext.SOURCE_FILE_TIMEOUT, TIMEOUT);
        try {
            recipe.run(new InMemoryLargeSourceSet(sources), ctx, cycles, 1);
        } catch (Throwable t) {
            errors.add(t);
        }
        return firstError(errors);
    }

    static @Nullable Throwable firstError(List<Throwable> errors) {
        for (Throwable error : errors) {
            if (!isTimeout(error)) {
                return error;
            }
        }
        return null;
    }

    private static boolean isTimeout(Throwable t) {
        for (Throwable cause = t; cause != null; cause = cause.getCause() == cause ? null : cause.getCause()) {
            if (cause instanceof SourceFileTimeoutException || cause instanceof RecipeTimeoutException) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the test's own error handler fails on the throwable, as the default one does.
     */
    static boolean rejects(ExecutionContext recipeCtx, Throwable thrown) {
        try {
            recipeCtx.getOnError().accept(thrown);
            return false;
        } catch (Throwable rejected) {
            return true;
        }
    }

    /**
     * The exception the recipe threw, with the first line of its message, the first frame outside the JDK and, when
     * that frame isn't the recipe's, the first frame that is.
     */
    static String describe(Throwable thrown, String indent, Recipe recipe) {
        Throwable cause = thrown instanceof RecipeRunException && thrown.getCause() != null ?
                thrown.getCause() : thrown;
        StringBuilder description = new StringBuilder(indent).append(cause.getClass().getName());
        String causeMessage = cause.getMessage();
        if (causeMessage != null && !causeMessage.trim().isEmpty()) {
            String firstLine = causeMessage.trim().split("\\R", 2)[0];
            description.append(": ")
                    .append(firstLine.length() > 200 ? firstLine.substring(0, 200) + "..." : firstLine);
        }
        Set<String> recipeClasses = new HashSet<>();
        addClasses(recipe, recipeClasses);
        boolean outsideJdk = false;
        for (StackTraceElement frame : cause.getStackTrace()) {
            String className = frame.getClassName();
            int nested = className.indexOf('$');
            boolean ofRecipe = recipeClasses.contains(nested < 0 ? className : className.substring(0, nested));
            if (!outsideJdk && !className.startsWith("java.") && !className.startsWith("jdk.") &&
                !className.startsWith("sun.")) {
                outsideJdk = true;
                description.append('\n').append(indent).append("    at ").append(frame);
                if (ofRecipe) {
                    break;
                }
            } else if (outsideJdk && ofRecipe) {
                description.append('\n').append(indent).append("    at ").append(frame);
                break;
            }
        }
        return description.toString();
    }

    private static void addClasses(Recipe recipe, Set<String> classes) {
        if (!classes.add(recipe.getClass().getName())) {
            return;
        }
        try {
            for (Recipe child : recipe.getRecipeList()) {
                addClasses(child, classes);
            }
        } catch (Exception ignored) {
            // a recipe that can't list its children is still named
        }
    }

    static String typeName(Class<?> type) {
        String name = type.getName();
        return name.substring(name.lastIndexOf('.') + 1).replace('$', '.');
    }
}
