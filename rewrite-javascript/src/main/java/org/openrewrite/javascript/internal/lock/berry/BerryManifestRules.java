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
package org.openrewrite.javascript.internal.lock.berry;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.DependencyPathSegment;
import org.openrewrite.javascript.internal.lock.HostEnvironment;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.ManifestRules;
import org.openrewrite.javascript.internal.lock.Rejected;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.openrewrite.javascript.internal.lock.berry.Configuration.DEFAULT_RC_FILENAME;

/**
 * Yarn Berry's reading of the files the dependency recipes edit: {@code resolutions} with one parent descriptor, the
 * catalogs of .yarnrc.yml as its configuration parses them, and its add's save.
 */
public final class BerryManifestRules implements ManifestRules {

    public static final BerryManifestRules RULES = new BerryManifestRules();

    private BerryManifestRules() {
    }

    @Override
    public OverridePlace overridePlace(String rootManifest, @Nullable Map<String, @Nullable Object> workspace,
                                       int release) {
        return OverridePlace.RESOLUTIONS;
    }

    // Yarn compares a parent with the resolved locator, which an npm range never equals (override-key-ranged-parent)
    @Override
    public @Nullable String overrideDeclined(String packageName, List<DependencyPathSegment> path) {
        StringBuilder key = new StringBuilder();
        for (DependencyPathSegment segment : path) {
            key.append(segment(segment)).append('/');
        }
        key.append(packageName);
        String rejected = Resolution.rejects(key.toString());
        if (rejected != null) {
            return "Yarn drops the resolution " + key + " (YN0057): " + rejected;
        }
        if (path.size() == 1 && !exactVersion(path.get(0).getVersion())) {
            return "Yarn matches a resolution's parent by its exact version only, not " + segment(path.get(0));
        }
        return null;
    }

    private static boolean exactVersion(@Nullable String version) {
        String bare = version != null && version.startsWith("npm:") ? version.substring("npm:".length()) : version;
        return bare == null || bare.equals(NodeSemver.valid(bare, NodeSemver.Options.DEFAULT));
    }

    // one parent (override-recipe-path)
    @Override
    public String overrideKey(String packageName, List<DependencyPathSegment> path, List<String> existing) {
        return path.isEmpty() ? packageName : segment(path.get(path.size() - 1)) + "/" + packageName;
    }

    @Override
    public @Nullable String outranked(String packageName, List<String> keys,
                                      Map<String, @Nullable Object> resolutions) {
        List<String> before = new ArrayList<>();
        for (String key : keys) {
            String parent = key.substring(0, Math.max(0, key.length() - packageName.length() - 1));
            if (key.endsWith("/" + packageName) && isParent(parent)) {
                before.add(key);
            }
        }
        return before.isEmpty() ? null : "Yarn applies " + String.join(", ", before) +
                                         " before a global override of " + packageName;
    }

    // a resolution's parent descriptor, not the scope of the package it resolves
    private static boolean isParent(String prefix) {
        return !prefix.isEmpty() && !(prefix.startsWith("@") && prefix.indexOf('/') < 0);
    }

    private static String segment(DependencyPathSegment segment) {
        return segment.getVersion() == null ? segment.getName() : segment.getName() + "@" + segment.getVersion();
    }

    @Override
    public String catalogFile() {
        return DEFAULT_RC_FILENAME;
    }

    @Override
    public List<String> catalogsAt(String text) {
        return emptyList();
    }

    @Override
    public Map<String, Map<String, String>> catalogs(String text) {
        try {
            return Configuration.catalogs(text, HostEnvironment.UNKNOWN);
        } catch (Configuration.UsageError | JsError e) {
            throw Rejected.rejects(Reason.MALFORMED_MANIFEST, "Yarn rejects " + DEFAULT_RC_FILENAME + ": " +
                                                              e.getMessage());
        }
    }

    @Override
    public @Nullable String catalogOf(String value) {
        return Catalogs.catalogOf(value);
    }

    @Override
    public List<String> sortedOnAdd(String field) {
        return DEPENDENCY_FIELDS;
    }

    @Override
    public boolean keepsEmptyFieldsOnAdd() {
        return false;
    }
}
