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
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.ImportValidationService;
import org.openrewrite.RecipeRun;

import java.util.*;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.fail;

/**
 * Checks that the files a recipe changes import what their code names and nothing else.
 */
final class ImportedNames {
    private final @Nullable ImportValidationService service;
    private final boolean importReferencedTypes;
    private final boolean removeUnusedImports;
    private List<String> undeclared = emptyList();

    private ImportedNames(@Nullable ImportValidationService service, boolean importReferencedTypes,
                          boolean removeUnusedImports) {
        this.service = service;
        this.importReferencedTypes = importReferencedTypes;
        this.removeUnusedImports = removeUnusedImports;
    }

    static ImportedNames of(RecipeQualityValidation quality, Recipe recipe, List<SourceFile> sources) {
        boolean removeUnusedImports = quality.removeUnusedImports() && !Probes.isAdHoc(recipe);
        if (quality.importReferencedTypes() || removeUnusedImports) {
            for (SourceFile source : sources) {
                ImportValidationService service = service(source);
                if (service != null) {
                    return new ImportedNames(service, quality.importReferencedTypes(), removeUnusedImports);
                }
            }
        }
        return new ImportedNames(null, false, false);
    }

    RecipeRun record(Supplier<RecipeRun> run) {
        if (service == null || !importReferencedTypes) {
            return run.get();
        }
        try (ImportValidationService.Recording recording = service.recordUndeclared()) {
            RecipeRun recipeRun = run.get();
            undeclared = recording.getUndeclared();
            return recipeRun;
        }
    }

    void assertImports(Collection<Result> results) {
        if (service == null) {
            return;
        }
        StringBuilder unimported = new StringBuilder();
        StringBuilder unused = new StringBuilder();
        for (Result result : results) {
            SourceFile after = result.getAfter();
            ImportValidationService afterService = after == null || after == result.getBefore() ? null :
                    service(after);
            if (afterService == null) {
                continue;
            }
            if (importReferencedTypes) {
                List<String> names = new ArrayList<>(afterService.unimported(after));
                if (result.getBefore() != null) {
                    names.removeAll(afterService.unimported(result.getBefore()));
                }
                append(unimported, result, names);
            }
            if (removeUnusedImports) {
                append(unused, result, afterService.newlyUnusedImports(result.getBefore(), after));
            }
        }
        List<String> failures = new ArrayList<>(3);
        if (unimported.length() > 0) {
            failures.add("Import the types and static members the code the recipe writes names, with " +
                         "maybeAddImport(\"java.util.List\") for a type or maybeAddImport(\"java.util.Collections\", " +
                         "\"emptyList\") for a static member, once the recipe has written code that names it. The " +
                         "recipe left these sources naming types and static members that nothing in them imports " +
                         "or declares:\n" + unimported +
                         "A JavaTemplate's imports(..) tells only the template's own parser about a type, not the " +
                         "file it is applied to.");
        }
        if (!undeclared.isEmpty()) {
            StringBuilder names = new StringBuilder();
            for (String name : undeclared) {
                names.append("  ").append(name).append('\n');
            }
            failures.add("Declare the types and static members a template names with imports(..) and " +
                         "staticImports(..) on its builder. These templates named them without declaring them, so " +
                         "they resolved only because the file the test applies them to imports them already, and " +
                         "on a file that doesn't, the template's code has no type:\n" + names +
                         "The file needs the import too, so also call maybeAddImport.");
        }
        if (!failures.isEmpty()) {
            failures.set(failures.size() - 1, failures.get(failures.size() - 1) + "\nIf the recipe names them " +
                         "this way on purpose, add spec.recipeQualityValidation(RecipeQualityValidation.builder()" +
                         ".importReferencedTypes(false).build()).");
        }
        if (unused.length() > 0) {
            failures.add("Remove the imports the recipe leaves unused, with maybeRemoveImport(\"java.util.List\") " +
                         "once it has removed the code that used a type, and add imports with maybeAddImport, " +
                         "which imports only what the file names. The recipe left these sources importing what " +
                         "nothing in them uses:\n" + unused +
                         "If the code still names it, the recipe wrote that code without its type, and a later " +
                         "RemoveUnusedImports would delete an import the code needs. If the recipe keeps them on " +
                         "purpose, add spec.recipeQualityValidation(RecipeQualityValidation.builder()" +
                         ".removeUnusedImports(false).build()).");
        }
        if (!failures.isEmpty()) {
            fail(String.join("\n\n", failures));
        }
    }

    private static void append(StringBuilder sources, Result result, List<String> names) {
        if (!names.isEmpty()) {
            sources.append("  ").append(requireNonNull(result.getAfter()).getSourcePath())
                    .append(NameMatching.changedBy(result)).append(": ").append(String.join(", ", names))
                    .append('\n');
        }
    }

    private static @Nullable ImportValidationService service(SourceFile sourceFile) {
        try {
            return sourceFile.service(ImportValidationService.class);
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }
}
