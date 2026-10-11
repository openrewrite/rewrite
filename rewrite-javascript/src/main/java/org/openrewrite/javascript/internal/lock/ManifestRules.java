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
package org.openrewrite.javascript.internal.lock;

import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableList;

/**
 * What a package manager reads of the files the dependency recipes edit, as its port answers: where and under which
 * key it reads an override, the catalogs it reads, how its add saves a manifest, and the release that installs.
 */
public interface ManifestRules {

    /**
     * The rules of no package manager: overrides in {@code overrides}, no catalogs, a manifest saved as it is.
     */
    ManifestRules NONE = new ManifestRules() {
    };

    List<String> DEPENDENCY_FIELDS = unmodifiableList(asList("dependencies", "devDependencies",
            "optionalDependencies", "peerDependencies"));

    /**
     * Where a package manager reads an override of a transitive dependency.
     */
    enum OverridePlace {
        OVERRIDES, RESOLUTIONS, PNPM_FIELD, WORKSPACE_MANIFEST
    }

    /**
     * Where it reads an override at the workspace root {@code rootManifest} beside its workspace settings (null
     * without them), for its {@code release} major (-1 for none).
     */
    default OverridePlace overridePlace(String rootManifest, @Nullable Map<String, @Nullable Object> workspace,
                                        int release) {
        return OverridePlace.OVERRIDES;
    }

    /**
     * Why it would not apply an override of {@code packageName} along {@code path} as the key writes it; null when
     * it would.
     */
    default @Nullable String overrideDeclined(String packageName, List<DependencyPathSegment> path) {
        return null;
    }

    /**
     * The key of an override of {@code packageName} along {@code path} in a map of overrides whose keys are
     * {@code existing}, in their order.
     */
    default String overrideKey(String packageName, List<DependencyPathSegment> path, List<String> existing) {
        return packageName;
    }

    /**
     * The overrides among {@code keys}, those where the override is read, that it applies before a global override of
     * {@code packageName}, given the root's {@code resolutions} as JSON.parse reads them; null when there are none.
     */
    default @Nullable String outranked(String packageName, List<String> keys,
                                       Map<String, @Nullable Object> resolutions) {
        return null;
    }

    /**
     * The name of the file it reads catalogs from in the workspace root; null for a package manager without catalogs.
     */
    default @Nullable String catalogFile() {
        return null;
    }

    /**
     * The keys of the object holding the catalogs in the catalog file {@code text}; null when it reads no catalogs
     * from it.
     */
    default @Nullable List<String> catalogsAt(String text) {
        return null;
    }

    /**
     * The catalogs it reads from the catalog file's text, by catalog then alias, the entries holding a string; throws
     * {@link Rejected} where it rejects the file and {@link CannotKnow} where the host does not say what it reads.
     */
    default Map<String, Map<String, String>> catalogs(String text) {
        return emptyMap();
    }

    /**
     * Whether its own commands write the catalog file, so an edit of it is written as they write it.
     */
    default boolean writesCatalogFile() {
        return false;
    }

    /**
     * The catalog a dependency value names; null when it is no catalog reference.
     */
    default @Nullable String catalogOf(String value) {
        return null;
    }

    /**
     * The name of its workspace settings file, beside a workspace's root manifest; null for none.
     */
    default @Nullable String workspaceSettingsFile() {
        return null;
    }

    /**
     * The workspace root's directory it finds from {@code dir} by its settings file over the paths {@code holds}; null
     * when where it installs is the root. Throws {@link Rejected} where it rejects the file it finds.
     */
    default @Nullable String workspaceRoot(Predicate<String> holds, String dir) {
        return null;
    }

    /**
     * Its workspace settings file as it reads it, null for none; throws where it rejects the file.
     */
    default @Nullable Map<String, @Nullable Object> workspaceSettings(@Nullable String text) {
        return null;
    }

    /**
     * The release that installs the root {@code rootManifest} beside its workspace settings, where the recipes' writes
     * depend on it; null where they do not. Throws {@link CannotKnow} where the host does not say which runs.
     */
    default @Nullable String release(String rootManifest, @Nullable Map<String, @Nullable Object> workspace) {
        return null;
    }

    /**
     * What its add saves for {@code alias@specifier} under the workspace settings: the specifier, and the catalog it
     * adds the entry to; throws {@link Rejected} where its add stops.
     */
    default Saved added(String alias, String specifier, @Nullable Map<String, @Nullable Object> workspace) {
        return new Saved(specifier, null);
    }

    /**
     * The dependency fields its add sorts when it saves a dependency in {@code field}.
     */
    default List<String> sortedOnAdd(String field) {
        return emptyList();
    }

    /**
     * The order its add sorts a dependency field's keys in.
     */
    default Comparator<String> addOrder() {
        return String::compareTo;
    }

    /**
     * Whether its add keeps a dependency field that holds nothing.
     */
    default boolean keepsEmptyFieldsOnAdd() {
        return true;
    }

    @Value
    class Saved {
        String specifier;

        /**
         * The catalog the add puts the entry in; null when it writes the specifier into the manifest.
         */
        @Nullable
        String catalogName;
    }
}
