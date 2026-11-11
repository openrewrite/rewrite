/*
 * Copyright 2026 the original author or authors.
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
import {Recipe} from "../../recipe";
import {prepareJavaRecipe} from "../../rpc/java-recipe";

/**
 * Scope of an npm dependency entry. Maps to the keys in `package.json` that
 * the dependency-management recipes accept; `null` / undefined means the
 * recipe's default (typically `"dependencies"`).
 */
export type NpmDependencyScope =
    | "dependencies"
    | "devDependencies"
    | "peerDependencies"
    | "optionalDependencies";

export interface AddDependencyOptions {
    packageName: string;
    version: string;
    scope?: NpmDependencyScope;
    /** Add only to a `package.json` that declared a package matching one of these globs before the cycle's edits. */
    onlyIfDeclaring?: string[];
    /**
     * Add only to a `package.json` that resolves every `name@range` entry, e.g. `react@>=16.9.0 <18`: by the version
     * its lock resolves, else when the declared constraint admits nothing outside the range. An undeclared package is
     * taken from the nearest enclosing `package.json` that declares it.
     */
    onlyIfResolving?: string[];
}

/**
 * Add an npm dependency to `package.json`, regenerate the lock file, and
 * refresh the `NodeResolutionResult` marker. Delegates to the Java recipe
 * `org.openrewrite.javascript.AddDependency` via RPC.
 */
export function addDependency(options: AddDependencyOptions): Promise<Recipe> {
    return prepareJavaRecipe("org.openrewrite.javascript.AddDependency", options);
}

export interface RemoveDependencyOptions {
    packageName: string;
    scope?: NpmDependencyScope;
}

/**
 * Remove an npm dependency from `package.json`, regenerate the lock file, and
 * refresh the `NodeResolutionResult` marker. Delegates to the Java recipe
 * `org.openrewrite.javascript.RemoveDependency` via RPC.
 */
export function removeDependency(options: RemoveDependencyOptions): Promise<Recipe> {
    return prepareJavaRecipe("org.openrewrite.javascript.RemoveDependency", options);
}

export interface UpgradeDependencyVersionOptions {
    packageName?: string;
    packagePattern?: string;
    newVersion: string;
}

/**
 * Upgrade an npm dependency to a new version (exactly one of `packageName` or
 * `packagePattern` must be provided). Delegates to the Java recipe
 * `org.openrewrite.javascript.UpgradeDependencyVersion` via RPC.
 */
export function upgradeDependencyVersion(options: UpgradeDependencyVersionOptions): Promise<Recipe> {
    return prepareJavaRecipe("org.openrewrite.javascript.UpgradeDependencyVersion", options);
}

export interface UpgradeDependencyVersionsOptions {
    /** Nonempty package-name glob map. Overlapping patterns must specify the same target constraint. */
    versions: Record<string, string>;
    /** Verify the upgraded manifest before recovering from stale npm lock peer conflicts. Defaults to false. */
    reconcileNpmPeerDependencies?: boolean;
}

/**
 * Upgrade matching npm dependencies together and regenerate the lock file once.
 * Delegates to `org.openrewrite.javascript.UpgradeDependencyVersions` via RPC.
 */
export function upgradeDependencyVersions(options: UpgradeDependencyVersionsOptions): Promise<Recipe> {
    return prepareJavaRecipe("org.openrewrite.javascript.UpgradeDependencyVersions", options);
}

export interface ChangeDependencyOptions {
    oldPackageName: string;
    newPackageName: string;
    newVersion?: string;
    scope?: NpmDependencyScope;
}

/**
 * Rename a dependency (and optionally pin a new version) in `package.json`.
 * Delegates to the Java recipe `org.openrewrite.javascript.ChangeDependency`
 * via RPC.
 */
export function changeDependency(options: ChangeDependencyOptions): Promise<Recipe> {
    return prepareJavaRecipe("org.openrewrite.javascript.ChangeDependency", options);
}

export interface UpgradeTransitiveDependencyVersionOptions {
    packageName: string;
    newVersion: string;
    dependencyPath?: string;
}

/**
 * Upgrade a transitive npm dependency via an `overrides`/`resolutions` block.
 * Delegates to the Java recipe
 * `org.openrewrite.javascript.UpgradeTransitiveDependencyVersion` via RPC.
 */
export function upgradeTransitiveDependencyVersion(
    options: UpgradeTransitiveDependencyVersionOptions,
): Promise<Recipe> {
    return prepareJavaRecipe(
        "org.openrewrite.javascript.UpgradeTransitiveDependencyVersion",
        options,
    );
}
