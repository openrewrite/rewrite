/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {RecipeRef} from "../preconditions";
import {IsSourceFile} from "../search";
import {UsesMethod, UsesType} from "./search";

/**
 * Match source files by path glob.
 *
 * Returns a {@link RecipeRef} placeholder bundled with a native
 * {@link IsSourceFile} visitor for in-process evaluation. The framework
 * introspects a ``check(hasSourcePath(...), editor)`` wrapper at
 * PrepareRecipe time and emits the recipe identity directly in
 * ``editPreconditions``; the Java host's ``PreparedRecipeCache.instantiateVisitor``
 * constructs the recipe and uses its visitor — no extra RPC round-trip
 * needed at editor() construction time. Unit tests without an active
 * RPC connection fall back to the bundled native visitor for real
 * filtering.
 *
 * Delegates to {@code org.openrewrite.FindSourceFiles}.
 */
export function hasSourcePath(filePattern: string): RecipeRef {
    return new RecipeRef(
        "org.openrewrite.FindSourceFiles",
        {filePattern},
        new IsSourceFile(filePattern),
    );
}

/**
 * Match files using a specific method.
 *
 * Returns a {@link RecipeRef} placeholder bundled with a native
 * {@link UsesMethod} visitor for in-process evaluation; see
 * {@link hasSourcePath} for the introspection / lazy-evaluation pattern.
 *
 * ``methodPattern`` follows the OpenRewrite method-pattern syntax:
 * ``<receiver-type> <method-name>(<args>)`` — e.g.
 * ``"*..* tostring(..)"`` or ``"java.util.Collections emptyList()"``.
 *
 * Delegates to {@code org.openrewrite.java.search.HasMethod}.
 */
export function usesMethod(methodPattern: string, matchOverrides: boolean = false): RecipeRef {
    return new RecipeRef(
        "org.openrewrite.java.search.HasMethod",
        {methodPattern, matchOverrides},
        new UsesMethod(methodPattern),
    );
}

/**
 * Match files using a specific type.
 *
 * Returns a {@link RecipeRef} placeholder bundled with a native
 * {@link UsesType} visitor for in-process evaluation; see
 * {@link hasSourcePath} for the introspection / lazy-evaluation pattern.
 *
 * Delegates to {@code org.openrewrite.java.search.HasType}.
 */
export function usesType(fullyQualifiedTypeName: string, checkAssignability: boolean = false): RecipeRef {
    return new RecipeRef(
        "org.openrewrite.java.search.HasType",
        {fullyQualifiedTypeName, checkAssignability},
        new UsesType(fullyQualifiedTypeName),
    );
}

/**
 * Find and mark methods matching a pattern.
 *
 * Returns a {@link RecipeRef} placeholder bundled with a native
 * {@link UsesMethod} visitor for in-process evaluation; see
 * {@link hasSourcePath} for the introspection / lazy-evaluation pattern.
 *
 * Delegates to {@code org.openrewrite.java.search.FindMethods}.
 */
export function findMethods(methodPattern: string, matchOverrides: boolean = false): RecipeRef {
    return new RecipeRef(
        "org.openrewrite.java.search.FindMethods",
        {methodPattern, matchOverrides},
        new UsesMethod(methodPattern),
    );
}

/**
 * Find and mark usages of a type.
 *
 * Returns a {@link RecipeRef} placeholder bundled with a native
 * {@link UsesType} visitor for in-process evaluation; see
 * {@link hasSourcePath} for the introspection / lazy-evaluation pattern.
 *
 * Delegates to {@code org.openrewrite.java.search.FindTypes}.
 */
export function findTypes(fullyQualifiedTypeName: string): RecipeRef {
    return new RecipeRef(
        "org.openrewrite.java.search.FindTypes",
        {fullyQualifiedTypeName},
        new UsesType(fullyQualifiedTypeName),
    );
}
